package hellfirepvp.modularmachinery.common.tiles;

import crafttweaker.util.IEventHandler;
import github.kasuminova.mmce.common.concurrent.RecipeSearchTask;
import github.kasuminova.mmce.common.concurrent.Sync;
import github.kasuminova.mmce.common.concurrent.TaskExecutor;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.block.BlockController;
import hellfirepvp.modularmachinery.common.block.BlockFactoryController;
import hellfirepvp.modularmachinery.common.crafting.ActiveMachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.event.recipe.*;
import hellfirepvp.modularmachinery.common.lib.BlocksMM;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import hellfirepvp.modularmachinery.common.util.MiscUtils;
import io.netty.util.internal.ThrowableUtil;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.LinkedList;
import java.util.List;

public class TileFactoryController extends TileMultiblockMachineController {

    private final List<RecipeQueueThread> recipeQueue = new LinkedList<>();
    private int totalParallelism = 1;
    private BlockFactoryController parentController = null;
    private RecipeSearchTask searchTask = null;

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
            if (recipeQueue.size() <= foundMachine.getMaxThreads()) {
                searchAndStartRecipe();
            }

            if (!recipeQueue.isEmpty()) {
                ModularMachinery.EXECUTE_MANAGER.addParallelAsyncTask(this::doRecipeTick);
                markForUpdateSync();
            }
            return;
        }

        ModularMachinery.EXECUTE_MANAGER.addParallelAsyncTask(() -> {
            if (recipeQueue.size() < foundMachine.getMaxThreads()) {
                searchAndStartRecipe();
            }

            if (!recipeQueue.isEmpty()) {
                doRecipeTick();
                markForUpdateSync();
            }
        });
    }

    /**
     * 工厂开始运行队列中的配方。
     */
    protected void doRecipeTick() {
        for (int i = 0; i < recipeQueue.size(); i++) {
            // 如果配方已经完成运行，或中途运行失败，则从队列中移除配方。
            if (doThreadRecipeTick(recipeQueue.get(i))) {
                recipeQueue.remove(i);
                i--;
            }
        }
    }

    /**
     * 工厂线程开始执行配方 Tick
     * @return 如果返回 {@code true}，则从控制器配方线程队列中移除当前配方线程。
     */
    protected boolean doThreadRecipeTick(RecipeQueueThread thread) {
        ActiveMachineRecipe activeRecipe = thread.activeRecipe;
        boolean waitForFinish = thread.waitForFinish;

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
                    onThreadRecipeFinished(thread);
                    recipeQueue.remove(thread);
                    return;
                }
                if (result.isCrafting()) {
                    onThreadRecipeFinished(thread);
                }
            });
        } else {
            CraftingStatus result = thread.onFinished();
            if (result == CraftingStatus.IDLE) {
                onThreadRecipeFinished(thread);
                return true;
            }
            if (result.isCrafting()) {
                onThreadRecipeFinished(thread);
            }
        }
        return false;
    }

    /**
     * <p>工厂线程开始执行一个配方。</p>
     */
    public void onThreadRecipeStart(RecipeQueueThread thread) {
        ActiveMachineRecipe activeRecipe = thread.activeRecipe;
        List<IEventHandler<RecipeEvent>> handlerList = activeRecipe.getRecipe().getRecipeEventHandlers(RecipeStartEvent.class);
        if (handlerList != null && !handlerList.isEmpty()) {
            for (IEventHandler<RecipeEvent> handler : handlerList) {
                RecipeStartEvent event = new RecipeStartEvent(this);
                handler.handle(event);
            }
        }
        activeRecipe.start(thread.context);
    }

    /**
     * <p>工厂线程在完成配方 Tick 后执行</p>
     *
     * @return 如果为 false，则进度停止增加，并在控制器状态栏输出原因
     */
    public boolean onThreadRecipePreTick(RecipeQueueThread thread) {
        ActiveMachineRecipe activeRecipe = thread.activeRecipe;
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
    public boolean onThreadRecipePostTick(RecipeQueueThread thread) {
        List<IEventHandler<RecipeEvent>> handlerList = thread.activeRecipe.getRecipe().getRecipeEventHandlers(RecipeTickEvent.class);
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
    public void onThreadRecipeFinished(RecipeQueueThread thread) {
        List<IEventHandler<RecipeEvent>> handlerList = thread.activeRecipe.getRecipe().getRecipeEventHandlers(RecipeFinishEvent.class);
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
    }

    protected void searchAndStartRecipe() {
        if (searchTask != null) {
            if (!searchTask.isDone()) {
                return;
            }

            //并发检查
            if (searchTask.getCurrentMachine() == getFoundMachine()) {
                try {
                    RecipeCraftingContext context = searchTask.get();
                    if (context != null) {
                        offerRecipe(context, true);
                        searchTask = null;

                        if (recipeQueue.size() < foundMachine.getMaxThreads()) {
                            createRecipeSearchTask();
                        }
                        return;
                    } else {
                        CraftingStatus status = searchTask.getStatus();
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
            createRecipeSearchTask();
        }
    }

    public List<RecipeQueueThread> getRecipeQueue() {
        return recipeQueue;
    }

    /**
     * 获取工厂剩余可用并行数。
     */
    @Override
    public int getMaxParallelism() {
        int maxParallelism = super.getMaxParallelism();
        for (RecipeQueueThread thread : recipeQueue) {
            maxParallelism -= (thread.activeRecipe.getParallelism() - 1);
        }

        return maxParallelism;
    }

    /**
     * 获取工厂最大并行数。
     */
    public int getTotalParallelism() {
        return totalParallelism;
    }

    /**
     * <p>机器尝试开始执行一个配方。</p>
     *
     * @param context RecipeCraftingContext
     */
    protected boolean tryStartRecipe(@Nonnull RecipeCraftingContext context, boolean addToQueue) {
        RecipeCraftingContext.CraftingCheckResult tryResult = onCheck(context);

        return tryResult.isSuccess() && offerRecipe(context, addToQueue);
    }

    public boolean offerRecipe(RecipeCraftingContext context, boolean addToQueue) {
        if (recipeQueue.size() > foundMachine.getMaxThreads()) {
            return false;
        }

        RecipeQueueThread thread = new RecipeQueueThread(this);
        thread.setContext(context)
                  .setActiveRecipe(context.getActiveRecipe())
                  .setStatus(CraftingStatus.SUCCESS);
        if (addToQueue) {
            recipeQueue.add(thread);
        }
        onThreadRecipeStart(thread);
        return true;
    }

    @Override
    public void flushContextModifier() {
        for (RecipeQueueThread thread : recipeQueue) {
            RecipeCraftingContext context = thread.context;
            context.overrideModifier(MiscUtils.flatten(this.foundModifiers.values()));
            for (RecipeModifier modifier : customModifiers.values()) {
                context.addModifier(modifier);
            }
        }
    }

    protected void createRecipeSearchTask() {
        searchTask = new RecipeSearchTask(this, getFoundMachine());
        TaskExecutor.FORK_JOIN_POOL.submit(searchTask);
    }

    @Override
    public void readCustomNBT(NBTTagCompound compound) {
        super.readCustomNBT(compound);
        parentController = BlockFactoryController.FACOTRY_CONTROLLERS.get(parentMachine);

        recipeQueue.clear();
        if (compound.hasKey("queueList", Constants.NBT.TAG_LIST)) {
            NBTTagList queueList = compound.getTagList("queueList", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < queueList.tagCount(); i++) {
                NBTTagCompound tagAt = queueList.getCompoundTagAt(i);
                RecipeQueueThread queueThread = RecipeQueueThread.deserialize(tagAt, this);
                if (queueThread != null) {
                    recipeQueue.add(queueThread);
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

        if (!recipeQueue.isEmpty()) {
            NBTTagList queueList = new NBTTagList();
            recipeQueue.forEach(queue -> queueList.appendTag(queue.serialize()));
            compound.setTag("queueList", queueList);
        }

        compound.setInteger("totalParallelism", super.getMaxParallelism());
    }

    @Nullable
    @Override
    public ActiveMachineRecipe[] getActiveRecipeList() {
        return recipeQueue.stream().map(e -> e.activeRecipe).toArray(type -> new ActiveMachineRecipe[0]);
    }

    @Override
    public boolean isWorking() {
        return false;
    }

    @Override
    public void overrideStatusInfo(String newInfo) {

    }

    public static class RecipeQueueThread {
        private final TileFactoryController factory;
        private ActiveMachineRecipe activeRecipe = null;
        private RecipeCraftingContext context = null;
        private CraftingStatus status = CraftingStatus.IDLE;
        private boolean waitForFinish = false;

        public RecipeQueueThread(TileFactoryController factory) {
            this.factory = factory;
        }

        public CraftingStatus onTick() {
            if (activeRecipe == null) {
                return new CraftingStatus(Type.NO_RECIPE, "");
            }
            if (context == null) {
                context = factory.createContext(activeRecipe);
            }

            return (status = activeRecipe.tick(factory, context));
        }

        public CraftingStatus onFinished() {
            if (activeRecipe == null) {
                return new CraftingStatus(Type.NO_RECIPE, "");
            }
            if (context == null) {
                context = factory.createContext(activeRecipe);
            }

            RecipeCraftingContext.CraftingCheckResult checkResult = context.canFinishCrafting();
            if (checkResult.isFailure()) {
                waitForFinish = true;
                return (status = CraftingStatus.failure(checkResult.getFirstErrorMessage("")));
            }

            context.finishCrafting();
            activeRecipe.reset();
            activeRecipe.setMaxParallelism(factory.getMaxParallelism());
            context = factory.createContext(activeRecipe);

            return factory.tryStartRecipe(context, false)
                    ? CraftingStatus.SUCCESS
                    : CraftingStatus.IDLE;
        }

        public NBTTagCompound serialize() {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setTag("activeRecipe", activeRecipe.serialize());
            tag.setTag("status", status.serialize());
            return tag;
        }

        public static RecipeQueueThread deserialize(NBTTagCompound tag, TileFactoryController factory) {
            if (!tag.hasKey("activeRecipe") || !tag.hasKey("status")) {
                return null;
            }
            return new RecipeQueueThread(factory)
                    .setActiveRecipe(new ActiveMachineRecipe(tag.getCompoundTag("activeRecipe")))
                    .setStatus(CraftingStatus.deserialize(tag.getCompoundTag("status")));
        }

        public ActiveMachineRecipe getActiveRecipe() {
            return activeRecipe;
        }

        public RecipeQueueThread setActiveRecipe(ActiveMachineRecipe activeRecipe) {
            this.activeRecipe = activeRecipe;
            return this;
        }

        public RecipeCraftingContext getContext() {
            return context;
        }

        public RecipeQueueThread setContext(RecipeCraftingContext context) {
            this.context = context;
            return this;
        }

        public CraftingStatus getStatus() {
            return status;
        }

        public RecipeQueueThread setStatus(CraftingStatus status) {
            this.status = status;
            return this;
        }

        public boolean isWaitForFinish() {
            return waitForFinish;
        }

        public RecipeQueueThread setWaitForFinish(boolean waitForFinish) {
            this.waitForFinish = waitForFinish;
            return this;
        }
    }
}
