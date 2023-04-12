package hellfirepvp.modularmachinery.common.tiles;

import crafttweaker.util.IEventHandler;
import github.kasuminova.mmce.common.concurrent.RecipeSearchTask;
import github.kasuminova.mmce.common.concurrent.Sync;
import github.kasuminova.mmce.common.concurrent.TaskExecutor;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.block.BlockController;
import hellfirepvp.modularmachinery.common.block.BlockFactoryController;
import hellfirepvp.modularmachinery.common.crafting.ActiveMachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.RecipeRegistry;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.event.recipe.*;
import hellfirepvp.modularmachinery.common.lib.BlocksMM;
import hellfirepvp.modularmachinery.common.machine.factory.RecipeThread;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import hellfirepvp.modularmachinery.common.util.MiscUtils;
import io.netty.util.internal.ThrowableUtil;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Tuple;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;
import java.util.*;

public class TileFactoryController extends TileMultiblockMachineController {
    private final Map<String, RecipeThread> daemonRecipeThreads = new LinkedHashMap<>();
    private final List<RecipeThread> recipeThreadList = new LinkedList<>();
    private int totalParallelism = 1;
    private BlockFactoryController parentController = null;
    private Tuple<String, RecipeSearchTask> searchTask = null;

    public TileFactoryController() {

    }

    public TileFactoryController(IBlockState state) {
        this();
        if (state.getBlock() instanceof BlockFactoryController) {
            parentController = (BlockFactoryController) state.getBlock();
            controllerRotation = state.getValue(BlockController.FACING);
            parentMachine = parentController.getParentMachine();
        } else {
            ModularMachinery.log.warn("Invalid factory controller block at " + getPos() + " !");
            controllerRotation = EnumFacing.NORTH;
        }
    }

    @Override
    public void doRestrictedTick() {
        if (getWorld().isRemote) {
            return;
        }
        if (getWorld().getStrongPower(getPos()) > 0) {
            return;
        }

        if (!doStructureCheck() || !isStructureFormed()) {
            return;
        }

        if (recipeResearchRetryCounter > 1) {
            onMachineTick();

            if (recipeThreadList.size() < foundMachine.getMaxThreads()) {
                searchAndStartRecipe();
            }

            if (!recipeThreadList.isEmpty()) {
                ModularMachinery.EXECUTE_MANAGER.addParallelAsyncTask(this::doRecipeTick);
                markForUpdateSync();
            }
            return;
        }

        ModularMachinery.EXECUTE_MANAGER.addParallelAsyncTask(() -> {
            onMachineTick();

            if (recipeThreadList.size() < foundMachine.getMaxThreads()) {
                searchAndStartRecipe();
            }

            if (!recipeThreadList.isEmpty()) {
                doRecipeTick();
                markForUpdateSync();
            }
        });
    }

    /**
     * 工厂开始运行队列中的配方。
     */
    protected void doRecipeTick() {
        for (int i = 0; i < recipeThreadList.size(); i++) {
            // 如果配方已经完成运行，或中途运行失败，则从队列中移除配方。
            if (doThreadRecipeTick(recipeThreadList.get(i))) {
                recipeThreadList.remove(i);
                i--;
            }
        }
    }

    /**
     * 工厂守护线程开始执行配方 Tick
     */
    protected void doDaemonThreadRecipeTick(RecipeThread thread) {
        if (!thread.isWorking()) {
            return;
        }

        ActiveMachineRecipe activeRecipe = thread.getActiveRecipe();
        boolean waitForFinish = thread.isWaitForFinish();

        // PreTickEvent
        if (!waitForFinish && onThreadRecipePreTick(thread)) {
            return;
        }
        // RecipeTick
        CraftingStatus status = thread.onTick();
        if (!status.isCrafting() && activeRecipe.getRecipe().doesCancelRecipeOnPerTickFailure()) {
            thread.setActiveRecipe(null).setContext(null);
            return;
            // PostTickEvent
        } else if (!waitForFinish && onThreadRecipePostTick(thread)) {
            return;
        }
        if (!activeRecipe.isCompleted()) {
            return;
        }

        // FinishedEvent
        if (ModularMachinery.pluginServerCompatibleMode) {
            ModularMachinery.EXECUTE_MANAGER.addSyncTask(thread::onFinished);
        } else {
            thread.onFinished();
        }
    }

    /**
     * 工厂线程开始执行配方 Tick
     * @return 如果返回 {@code true}，则从控制器配方线程队列中移除当前配方线程。
     */
    protected boolean doThreadRecipeTick(RecipeThread thread) {
        if (!thread.isWorking()) {
            return true;
        }

        ActiveMachineRecipe activeRecipe = thread.getActiveRecipe();
        boolean waitForFinish = thread.isWaitForFinish();

        // PreTickEvent
        if (!waitForFinish && onThreadRecipePreTick(thread)) {
            return true;
        }
        // RecipeTick
        CraftingStatus status = thread.onTick();
        if (!status.isCrafting() && activeRecipe.getRecipe().doesCancelRecipeOnPerTickFailure()) {
            return true;
                                     // PostTickEvent
        } else if (!waitForFinish && onThreadRecipePostTick(thread)) {
            return true;
        }
        if (!activeRecipe.isCompleted()) {
            return false;
        }

        // FinishedEvent
        if (ModularMachinery.pluginServerCompatibleMode) {
            ModularMachinery.EXECUTE_MANAGER.addSyncTask(() -> {
                CraftingStatus result = thread.onFinished();
                if (result == CraftingStatus.IDLE) {
                    recipeThreadList.remove(thread);
                }
            });
        } else {
            CraftingStatus result = thread.onFinished();
            return result == CraftingStatus.IDLE;
        }
        return false;
    }

    /**
     * <p>工厂线程开始执行一个配方。</p>
     */
    public void onThreadRecipeStart(RecipeThread thread) {
        ActiveMachineRecipe activeRecipe = thread.getActiveRecipe();
        List<IEventHandler<RecipeEvent>> handlerList = activeRecipe.getRecipe().getRecipeEventHandlers(RecipeStartEvent.class);
        if (handlerList != null && !handlerList.isEmpty()) {
            for (IEventHandler<RecipeEvent> handler : handlerList) {
                RecipeStartEvent event = new RecipeStartEvent(this);
                handler.handle(event);
            }
        }
        activeRecipe.start(thread.getContext());
    }

    /**
     * <p>工厂线程在完成配方 Tick 后执行</p>
     *
     * @return 如果为 false，则进度停止增加，并在控制器状态栏输出原因
     */
    public boolean onThreadRecipePreTick(RecipeThread thread) {
        ActiveMachineRecipe activeRecipe = thread.getActiveRecipe();
        List<IEventHandler<RecipeEvent>> handlerList = activeRecipe.getRecipe().getRecipeEventHandlers(RecipePreTickEvent.class);
        if (handlerList == null || handlerList.isEmpty()) return false;

        for (IEventHandler<RecipeEvent> handler : handlerList) {
            RecipePreTickEvent event = new RecipePreTickEvent(this);
            handler.handle(event);

            if (event.isPreventProgressing()) {
                activeRecipe.setTick(activeRecipe.getTick() - 1);
                thread.setStatus(CraftingStatus.working(event.getReason()));
                return false;
            }
            if (event.isFailure()) {
                if (event.isDestructRecipe()) {
                    return true;
                }
                thread.setStatus(CraftingStatus.working(event.getReason()));
                return false;
            }
        }

        return false;
    }

    /**
     * <p>与 {@code onPreTick()} 相似，但是可以销毁配方。</p>
     *
     * @return 如果返回 {@code true}，则从控制器配方线程队列中移除当前配方线程。
     */
    public boolean onThreadRecipePostTick(RecipeThread thread) {
        List<IEventHandler<RecipeEvent>> handlerList = thread.getActiveRecipe().getRecipe().getRecipeEventHandlers(RecipeTickEvent.class);
        if (handlerList == null || handlerList.isEmpty()) return false;

        for (IEventHandler<RecipeEvent> handler : handlerList) {
            RecipeTickEvent event = new RecipeTickEvent(this);
            handler.handle(event);
            if (event.isFailure()) {
                if (event.isDestructRecipe()) {
                    return true;
                }
                thread.setStatus(CraftingStatus.failure(event.getFailureReason()));
                return false;
            }
        }
        return false;
    }

    /**
     * <p>工厂线程完成一个配方。</p>
     */
    public void onThreadRecipeFinished(RecipeThread thread) {
        List<IEventHandler<RecipeEvent>> handlerList = thread.getActiveRecipe().getRecipe().getRecipeEventHandlers(RecipeFinishEvent.class);
        if (handlerList != null && !handlerList.isEmpty()) {
            for (IEventHandler<RecipeEvent> handler : handlerList) {
                RecipeFinishEvent event = new RecipeFinishEvent(this);
                handler.handle(event);
            }
        }
    }

    @Override
    protected void onStructureFormed() {
        Sync.doSyncAction(() -> {
            if (parentController != null) {
                this.world.setBlockState(pos, parentController.getDefaultState().withProperty(
                        BlockController.FACING, this.controllerRotation));
            } else {
                this.world.setBlockState(pos, BlocksMM.blockFactoryController.getDefaultState().withProperty(
                        BlockController.FACING, this.controllerRotation));
            }
        });

        super.onStructureFormed();

        daemonRecipeThreads.clear();
        foundMachine.getDaemonThreads().forEach((threadName, thread) -> daemonRecipeThreads.put(threadName, thread.copy(this)));
    }

    protected void searchAndStartRecipe() {
        if (searchTask != null) {
            RecipeSearchTask task = searchTask.getSecond();
            if (!task.isDone()) {
                return;
            }

            //并发检查
            if (task.getCurrentMachine() == getFoundMachine()) {
                try {
                    RecipeCraftingContext context = task.get();
                    if (context != null) {
                        offerRecipe(context, true);
                        searchTask = null;

                        if (recipeThreadList.size() < foundMachine.getMaxThreads()) {
                            createRecipeSearchTask("");
                        }
                        return;
                    } else {
                        CraftingStatus status = task.getStatus();
                        if (status != null) {
                            this.craftingStatus = status;
                        }
                    }
                } catch (Exception e) {
                    ModularMachinery.log.warn(ThrowableUtil.stackTraceToString(e));
                }
            }

            searchTask = null;
        } else if (this.ticksExisted % currentRecipeSearchDelay() == 0) {
            createRecipeSearchTask("");
        }
    }

    @Override
    protected void resetMachine(boolean clearData) {
        super.resetMachine(clearData);
        recipeThreadList.clear();
        daemonRecipeThreads.clear();
    }

    public List<RecipeThread> getRecipeThreadList() {
        return recipeThreadList;
    }

    /**
     * 获取工厂最大并行数。
     * 服务端调用。
     */
    public int getAvailableParallelism() {
        int maxParallelism = getMaxParallelism();
        for (RecipeThread thread : recipeThreadList) {
            maxParallelism -= (thread.getActiveRecipe().getParallelism() - 1);
        }

        return maxParallelism;
    }

    /**
     * 获取工厂最大并行数。
     * 仅限客户端。
     */
    public int getTotalParallelism() {
        return totalParallelism;
    }

    public void offerRecipe(RecipeCraftingContext context, boolean addToQueue) {
        if (recipeThreadList.size() > foundMachine.getMaxThreads()) {
            return;
        }

        RecipeThread thread = new RecipeThread(this);
        thread.setContext(context)
                  .setActiveRecipe(context.getActiveRecipe())
                  .setStatus(CraftingStatus.SUCCESS);
        if (addToQueue) {
            recipeThreadList.add(thread);
        }
        onThreadRecipeStart(thread);
    }

    @Override
    public void flushContextModifier() {
        recipeThreadList.stream().map(RecipeThread::getContext).filter(Objects::nonNull).forEach(context -> {
            context.overrideModifier(MiscUtils.flatten(this.foundModifiers.values()));
            for (RecipeModifier modifier : customModifiers.values()) {
                context.addModifier(modifier);
            }
        });
    }

    protected void createRecipeSearchTask(String threadName) {
        searchTask = new Tuple<>(threadName, new RecipeSearchTask(this, getFoundMachine(), getAvailableParallelism(), RecipeRegistry.getRecipesFor(foundMachine)));
        TaskExecutor.FORK_JOIN_POOL.submit(searchTask.getSecond());
    }

    @Override
    public void readCustomNBT(NBTTagCompound compound) {
        super.readCustomNBT(compound);
        parentController = BlockFactoryController.FACOTRY_CONTROLLERS.get(parentMachine);

        recipeThreadList.clear();
        if (compound.hasKey("queueList", Constants.NBT.TAG_LIST)) {
            NBTTagList queueList = compound.getTagList("queueList", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < queueList.tagCount(); i++) {
                NBTTagCompound tagAt = queueList.getCompoundTagAt(i);
                RecipeThread queueThread = RecipeThread.deserialize(tagAt, this);
                if (queueThread != null) {
                    recipeThreadList.add(queueThread);
                }
            }
        }

        if (compound.hasKey("totalParallelism")) {
            totalParallelism = compound.getInteger("totalParallelism");
        }
    }

    @Override
    public void writeCustomNBT(NBTTagCompound compound) {
        super.writeCustomNBT(compound);

        if (!isStructureFormed()) {
            return;
        }

        if (!recipeThreadList.isEmpty()) {
            NBTTagList queueList = new NBTTagList();
            recipeThreadList.forEach(queue -> queueList.appendTag(queue.serialize()));
            compound.setTag("queueList", queueList);
        }

        compound.setInteger("totalParallelism", getMaxParallelism());
    }

    @Nullable
    @Override
    public ActiveMachineRecipe[] getActiveRecipeList() {
        return recipeThreadList.stream().map(RecipeThread::getActiveRecipe).toArray(type -> new ActiveMachineRecipe[0]);
    }

    @Override
    public boolean isWorking() {
        return !recipeThreadList.isEmpty();
    }

    @Override
    public void overrideStatusInfo(String newInfo) {

    }
}
