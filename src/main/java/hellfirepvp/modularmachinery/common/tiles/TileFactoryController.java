package hellfirepvp.modularmachinery.common.tiles;

import crafttweaker.util.IEventHandler;
import github.kasuminova.mmce.common.concurrent.FactoryRecipeSearchTask;
import github.kasuminova.mmce.common.concurrent.RecipeSearchTask;
import github.kasuminova.mmce.common.concurrent.SequentialTaskExecutor;
import github.kasuminova.mmce.common.concurrent.Sync;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.block.BlockController;
import hellfirepvp.modularmachinery.common.block.BlockFactoryController;
import hellfirepvp.modularmachinery.common.crafting.ActiveMachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.MachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.RecipeRegistry;
import hellfirepvp.modularmachinery.common.crafting.helper.CraftingStatus;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.event.recipe.*;
import hellfirepvp.modularmachinery.common.lib.BlocksMM;
import hellfirepvp.modularmachinery.common.machine.MachineRegistry;
import hellfirepvp.modularmachinery.common.machine.factory.RecipeThread;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import io.netty.util.internal.ThrowableUtil;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

public class TileFactoryController extends TileMultiblockMachineController {
    private final Map<String, RecipeThread> coreRecipeThreads = new LinkedHashMap<>();
    private final List<RecipeThread> recipeThreadList = new LinkedList<>();
    private final List<ForkJoinTask<?>> waitToExecute = new ArrayList<>();
    private int totalParallelism = 1;
    private BlockFactoryController parentController = null;
    private FactoryRecipeSearchTask searchTask = null;
    private SequentialTaskExecutor threadTask = null;

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
    public void doControllerTick() {
        if (getWorld().getStrongPower(getPos()) > 0) {
            return;
        }
        if (!doStructureCheck() || !isStructureFormed()) {
            return;
        }

        tickExecutor = ModularMachinery.EXECUTE_MANAGER.addParallelAsyncTask(() -> {
            executeSeqTask();
            onMachineTick();

            if (hasIdleThread()) {
                searchAndStartRecipe();
            }

            if (!coreRecipeThreads.isEmpty() || !recipeThreadList.isEmpty()) {
                doRecipeTick();
                markForUpdateSync();
            }
        }, usedTimeAvg());
    }

    /**
     * 工厂开始运行队列中的配方。
     */
    protected void doRecipeTick() {
        updateCoreThread();
        cleanIdleTimeoutThread();

        coreRecipeThreads.values().forEach(thread -> {
            if (thread.getActiveRecipe() == null) {
                thread.searchAndStartRecipe();
            }
            doThreadRecipeTick(thread);
        });

        recipeThreadList.forEach(this::doThreadRecipeTick);
    }

    /**
     * 工厂线程开始执行配方 Tick
     */
    protected void doThreadRecipeTick(RecipeThread thread) {
        ActiveMachineRecipe activeRecipe = thread.getActiveRecipe();
        if (activeRecipe == null) {
            thread.idleTime++;
            return;
        }

        // If this thread previously failed in completing the recipe,
        // it retries to complete the recipe.
        if (thread.isWaitForFinish()) {
            finishRecipe(thread);
            return;
        }

        // PreTickEvent
        if (!onThreadRecipePreTick(thread)) {
            return;
        }
        // RecipeTick
        CraftingStatus status = thread.onTick();
        if (!status.isCrafting() && activeRecipe.getRecipe().doesCancelRecipeOnPerTickFailure()) {
            thread.setActiveRecipe(null).setContext(null).getSemiPermanentModifiers().clear();
            return;
                   // PostTickEvent
        } else if (!onThreadRecipePostTick(thread)) {
            return;
        }
        if (!activeRecipe.isCompleted()) {
            return;
        }

        // FinishedEvent
        finishRecipe(thread);
    }

    private static void finishRecipe(RecipeThread thread) {
        // FinishedEvent
        if (ModularMachinery.pluginServerCompatibleMode) {
            ModularMachinery.EXECUTE_MANAGER.addSyncTask(thread::onFinished);
        } else {
            thread.onFinished();
        }
    }

    /**
     * <p>工厂线程开始执行一个配方。</p>
     */
    public void onThreadRecipeStart(RecipeThread thread) {
        ActiveMachineRecipe activeRecipe = thread.getActiveRecipe();
        List<IEventHandler<RecipeEvent>> handlerList = activeRecipe.getRecipe().getRecipeEventHandlers(FactoryRecipeStartEvent.class);
        if (handlerList != null && !handlerList.isEmpty()) {
            for (IEventHandler<RecipeEvent> handler : handlerList) {
                FactoryRecipeStartEvent event = new FactoryRecipeStartEvent(thread, this);
                handler.handle(event);
            }
        }
        activeRecipe.start(thread.getContext());
    }

    /**
     * <p>工厂线程在开始配方 Tick 前执行</p>
     *
     * @return 如果为 false，则进度停止增加，并在控制器状态栏输出原因
     */
    public boolean onThreadRecipePreTick(RecipeThread thread) {
        ActiveMachineRecipe activeRecipe = thread.getActiveRecipe();
        List<IEventHandler<RecipeEvent>> handlerList = activeRecipe.getRecipe().getRecipeEventHandlers(FactoryRecipePreTickEvent.class);
        if (handlerList == null || handlerList.isEmpty()) return true;

        for (IEventHandler<RecipeEvent> handler : handlerList) {
            FactoryRecipePreTickEvent event = new FactoryRecipePreTickEvent(thread, this);
            handler.handle(event);

            if (event.isPreventProgressing()) {
                activeRecipe.setTick(activeRecipe.getTick() - 1);
                thread.setStatus(CraftingStatus.working(event.getReason()));
                return true;
            }
            if (event.isFailure()) {
                if (event.isDestructRecipe()) {
                    thread.setActiveRecipe(null)
                            .setContext(null)
                            .setStatus(CraftingStatus.failure(event.getReason()))
                            .getSemiPermanentModifiers().clear();
                    return false;
                }
                thread.setStatus(CraftingStatus.failure(event.getReason()));
                return false;
            }
        }

        return true;
    }

    /**
     * <p>与 {@code onPreTick()} 相似，但是可以销毁配方。</p>
     *
     * @return 如果返回 {@code false}，即代表运行失败。
     */
    public boolean onThreadRecipePostTick(RecipeThread thread) {
        ActiveMachineRecipe activeRecipe = thread.getActiveRecipe();
        List<IEventHandler<RecipeEvent>> handlerList = activeRecipe.getRecipe().getRecipeEventHandlers(FactoryRecipeTickEvent.class);
        if (handlerList == null || handlerList.isEmpty()) return true;

        for (IEventHandler<RecipeEvent> handler : handlerList) {
            FactoryRecipeTickEvent event = new FactoryRecipeTickEvent(thread, this);
            handler.handle(event);
            if (event.isFailure()) {
                if (event.isDestructRecipe()) {
                    thread.setActiveRecipe(null)
                            .setContext(null)
                            .setStatus(CraftingStatus.failure(event.getFailureReason()))
                            .getSemiPermanentModifiers().clear();
                    return false;
                }
                thread.setStatus(CraftingStatus.failure(event.getFailureReason()));
                return false;
            }
        }
        return true;
    }

    /**
     * <p>工厂线程完成一个配方。</p>
     */
    public void onThreadRecipeFinished(RecipeThread thread) {
        ActiveMachineRecipe activeRecipe = thread.getActiveRecipe();
        List<IEventHandler<RecipeEvent>> handlerList = activeRecipe.getRecipe().getRecipeEventHandlers(FactoryRecipeFinishEvent.class);
        if (handlerList != null && !handlerList.isEmpty()) {
            for (IEventHandler<RecipeEvent> handler : handlerList) {
                FactoryRecipeFinishEvent event = new FactoryRecipeFinishEvent(thread, this);
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

        coreRecipeThreads.clear();
        foundMachine.getCoreThreadPreset().forEach((threadName, thread) ->
                coreRecipeThreads.put(threadName, thread.copyCoreThread(this)));
    }

    protected void searchAndStartRecipe() {
        if (searchTask != null) {
            RecipeSearchTask task = searchTask;
            if (!task.isDone()) {
                return;
            }
            //并发检查
            if (task.getCurrentMachine() != getFoundMachine()) {
                searchTask = null;
                return;
            }

            RecipeCraftingContext context = null;
            try {
                context = task.get();
            } catch (Exception e) {
                ModularMachinery.log.warn(ThrowableUtil.stackTraceToString(e));
            }

            if (context != null) {
                offerRecipe(context);
                searchTask = null;

                if (hasIdleThread()) {
                    createRecipeSearchTask();
                }
                resetRecipeSearchRetryCount();
                return;
            } else {
                incrementRecipeSearchRetryCount();
                CraftingStatus status = task.getStatus();
                if (status != null) {
                    this.craftingStatus = status;
                }
            }

            searchTask = null;
            return;
        }

        if (this.ticksExisted % currentRecipeSearchDelay() == 0) {
            createRecipeSearchTask();
        }
    }

    protected void executeSeqTask() {
        if (threadTask != null) {
            if (!threadTask.isDone() || waitToExecute.isEmpty()) {
                return;
            }
        } else if (waitToExecute.isEmpty()) {
            return;
        }
        threadTask = new SequentialTaskExecutor(waitToExecute);
        waitToExecute.clear();
        ForkJoinPool.commonPool().submit(threadTask);
    }

    @Override
    protected void resetMachine(boolean clearData) {
        super.resetMachine(clearData);
        recipeThreadList.clear();
        coreRecipeThreads.clear();
    }

    public List<RecipeThread> getRecipeThreadList() {
        return recipeThreadList;
    }

    public Map<String, RecipeThread> getCoreRecipeThreads() {
        return coreRecipeThreads;
    }

    /**
     * 获取工厂最大并行数。
     * 服务端调用。
     */
    public int getAvailableParallelism() {
        int maxParallelism = getMaxParallelism();
        for (RecipeThread thread : recipeThreadList) {
            ActiveMachineRecipe activeRecipe = thread.getActiveRecipe();
            if (activeRecipe == null) {
                continue;
            }
            maxParallelism -= (activeRecipe.getParallelism() - 1);
        }
        for (RecipeThread thread : coreRecipeThreads.values()) {
            ActiveMachineRecipe activeRecipe = thread.getActiveRecipe();
            if (activeRecipe == null) {
                continue;
            }
            maxParallelism -= (activeRecipe.getParallelism() - 1);
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

    public List<ForkJoinTask<?>> getWaitToExecute() {
        return waitToExecute;
    }

    public void offerRecipe(RecipeCraftingContext context) {
        for (RecipeThread thread : recipeThreadList) {
            if (thread.getActiveRecipe() == null) {
                thread.setContext(context)
                        .setActiveRecipe(context.getActiveRecipe())
                        .setStatus(CraftingStatus.SUCCESS);
                onThreadRecipeStart(thread);
                return;
            }
        }

        if (recipeThreadList.size() > foundMachine.getMaxThreads()) {
            return;
        }

        RecipeThread thread = new RecipeThread(this);
        thread.setContext(context)
                  .setActiveRecipe(context.getActiveRecipe())
                  .setStatus(CraftingStatus.SUCCESS);
        recipeThreadList.add(thread);
        onThreadRecipeStart(thread);
    }

    @Override
    public void flushContextModifier() {
        recipeThreadList.forEach(RecipeThread::flushContextModifier);
    }

    protected void createRecipeSearchTask() {
        searchTask = new FactoryRecipeSearchTask(
                this,
                getFoundMachine(),
                getAvailableParallelism(),
                RecipeRegistry.getRecipesFor(foundMachine),
                null, getActiveRecipeList());
        waitToExecute.add(searchTask);
    }

    /**
     * 更新核心线程列表。
     */
    protected void updateCoreThread() {
        Map<String, RecipeThread> threads = foundMachine.getCoreThreadPreset();
        if (threads.isEmpty()) {
            coreRecipeThreads.clear();
            return;
        }

        if (!coreRecipeThreads.isEmpty() && ticksExisted % 20 != 0) {
            return;
        }

        List<String> invalidThreads = new ArrayList<>();

        for (Map.Entry<String, RecipeThread> threadEntry : threads.entrySet()) {
            String name = threadEntry.getKey();
            if (!coreRecipeThreads.containsKey(name)) {
                coreRecipeThreads.put(name, threadEntry.getValue().copyCoreThread(this));
            }
        }

        for (Map.Entry<String, RecipeThread> threadEntry : coreRecipeThreads.entrySet()) {
            String name = threadEntry.getKey();
            RecipeThread thread = threads.get(name);
            if (thread == null) {
                invalidThreads.add(name);
                continue;
            }

            RecipeThread factoryThread = threadEntry.getValue();
            Set<MachineRecipe> recipeSet = factoryThread.getRecipeSet();
            recipeSet.clear();
            recipeSet.addAll(thread.getRecipeSet());
        }

        for (String name : invalidThreads) {
            coreRecipeThreads.remove(name);
        }
    }

    /**
     * 清理闲置时间过长的线程。
     */
    protected void cleanIdleTimeoutThread() {
        for (int i = 0; i < recipeThreadList.size(); i++) {
            RecipeThread thread = recipeThreadList.get(i);
            if (thread.isIdle() && thread.idleTime >= RecipeThread.IDLE_TIME_OUT) {
                recipeThreadList.remove(i);
                i--;
            }
        }
    }

    public boolean hasIdleThread() {
        if (recipeThreadList.size() < foundMachine.getMaxThreads()) {
            return true;
        }

        for (RecipeThread thread : recipeThreadList) {
            if (thread.getActiveRecipe() == null) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void readCustomNBT(NBTTagCompound compound) {
        super.readCustomNBT(compound);

        if (!isStructureFormed()) {
            return;
        }

        parentController = BlockFactoryController.FACOTRY_CONTROLLERS.get(parentMachine);

        recipeThreadList.clear();
        coreRecipeThreads.clear();

        if (compound.hasKey("threadList", Constants.NBT.TAG_LIST)) {
            NBTTagList threadList = compound.getTagList("threadList", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < threadList.tagCount(); i++) {
                NBTTagCompound tagAt = threadList.getCompoundTagAt(i);
                RecipeThread thread = RecipeThread.deserialize(tagAt, this);
                if (thread != null) {
                    recipeThreadList.add(thread);
                }
            }
        }

        if (compound.hasKey("coreThreadList", Constants.NBT.TAG_LIST)) {
            NBTTagList threadList = compound.getTagList("coreThreadList", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < threadList.tagCount(); i++) {
                NBTTagCompound tagAt = threadList.getCompoundTagAt(i);
                RecipeThread thread = RecipeThread.deserialize(tagAt, this);
                if (thread != null) {
                    coreRecipeThreads.put(thread.getThreadName(), thread);
                }
            }
        }

        if (compound.hasKey("totalParallelism")) {
            totalParallelism = compound.getInteger("totalParallelism");
        }
    }

    @Override
    protected void readMachineNBT(NBTTagCompound compound) {
        if (compound.hasKey("parentMachine")) {
            ResourceLocation rl = new ResourceLocation(compound.getString("parentMachine"));
            parentMachine = MachineRegistry.getRegistry().getMachine(rl);
            if (parentMachine != null) {
                parentController = BlockFactoryController.FACOTRY_CONTROLLERS.get(parentMachine);
            } else {
                ModularMachinery.log.info("Couldn't find machine named " + rl + " for controller at " + getPos());
            }
        }
        super.readMachineNBT(compound);
    }

    @Override
    public void writeCustomNBT(NBTTagCompound compound) {
        super.writeCustomNBT(compound);

        if (!isStructureFormed()) {
            return;
        }

        if (!recipeThreadList.isEmpty()) {
            NBTTagList threadList = new NBTTagList();
            recipeThreadList.forEach(thread -> threadList.appendTag(thread.serialize()));
            compound.setTag("threadList", threadList);
        }

        if (!coreRecipeThreads.isEmpty()) {
            NBTTagList threadList = new NBTTagList();
            coreRecipeThreads.values().forEach(thread -> threadList.appendTag(thread.serialize()));
            compound.setTag("coreThreadList", threadList);
        }

        compound.setInteger("totalParallelism", getMaxParallelism());
    }

    @Nullable
    @Override
    public ActiveMachineRecipe getActiveRecipe() {
        ActiveMachineRecipe[] activeRecipes = getActiveRecipeList();
        return activeRecipes.length == 0 ? null : activeRecipes[0];
    }

    @Nonnull
    @Override
    public ActiveMachineRecipe[] getActiveRecipeList() {
        List<ActiveMachineRecipe> list = new ArrayList<>();
        for (RecipeThread thread : coreRecipeThreads.values()) {
            ActiveMachineRecipe activeRecipe = thread.getActiveRecipe();
            if (activeRecipe != null) {
                list.add(activeRecipe);
            }
        }
        for (RecipeThread thread : recipeThreadList) {
            ActiveMachineRecipe activeRecipe = thread.getActiveRecipe();
            if (activeRecipe != null) {
                list.add(activeRecipe);
            }
        }
        return list.toArray(new ActiveMachineRecipe[0]);
    }

    @Override
    public boolean isWorking() {
        if (coreRecipeThreads.isEmpty() && recipeThreadList.isEmpty()) {
            return false;
        }
        for (RecipeThread thread : coreRecipeThreads.values()) {
            if (thread.getActiveRecipe() != null) {
                return true;
            }
        }
        for (RecipeThread thread : recipeThreadList) {
            if (thread.getActiveRecipe() != null) {
                return true;
            }
        }
        return false;
    }

    @Override
    @Deprecated
    public void overrideStatusInfo(String newInfo) {

    }
}
