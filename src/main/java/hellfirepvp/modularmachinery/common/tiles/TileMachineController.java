/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.tiles;

import crafttweaker.util.IEventHandler;
import github.kasuminova.mmce.common.concurrent.RecipeSearchTask;
import github.kasuminova.mmce.common.concurrent.Sync;
import github.kasuminova.mmce.common.concurrent.TaskExecutor;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.block.BlockController;
import hellfirepvp.modularmachinery.common.crafting.ActiveMachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.MachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.RecipeRegistry;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.event.machine.MachineEvent;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.event.machine.MachineTickEvent;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.event.recipe.*;
import hellfirepvp.modularmachinery.common.lib.BlocksMM;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.machine.MachineRegistry;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import hellfirepvp.modularmachinery.common.util.MiscUtils;
import io.netty.util.internal.ThrowableUtil;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * <p>完全重构的社区版机械控制器，拥有强大的异步逻辑和极低的性能消耗。</p>
 * <p>Completely refactored community edition mechanical controller with powerful asynchronous logic and extremely low performance consumption.</p>
 * TODO: This class is too large, consider improving readability.
 */
public class TileMachineController extends TileMultiblockMachineController {
    private BlockController parentController = null;
    private ActiveMachineRecipe activeRecipe = null;
    private RecipeCraftingContext context = null;
    private RecipeSearchTask searchTask = null;

    public TileMachineController() {
    }

    public TileMachineController(IBlockState state) {
        this();
        if (state.getBlock() instanceof BlockController) {
            this.parentController = (BlockController) state.getBlock();
            this.parentMachine = parentController.getParentMachine();
            this.controllerRotation = state.getValue(BlockController.FACING);
        } else {
            // wtf, where is the controller?
            ModularMachinery.log.warn("Invalid controller block at " + getPos() + " !");
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

        // Use async check for large structure
        if (isStructureFormed() && !ModularMachinery.pluginServerCompatibleMode && this.foundPattern.getPattern().size() >= 1000) {
            ModularMachinery.EXECUTE_MANAGER.addParallelAsyncTask(() -> {
                onMachineTick();
                if (!doStructureCheck() || !isStructureFormed()) {
                    return;
                }
                if (activeRecipe != null || searchAndStartRecipe()) {
                    doRecipeTick();
                }
            });
            return;
        }

        // Normal logic
        if (!doStructureCheck() || !isStructureFormed()) {
            return;
        }

        if (hasMachineTickEventHandlers()) {
            ModularMachinery.EXECUTE_MANAGER.addParallelAsyncTask(() -> {
                onMachineTick();
                if (activeRecipe != null || searchAndStartRecipe()) {
                    doRecipeTick();
                }
            });
            return;
        }

        if (activeRecipe != null || searchAndStartRecipe()) {
            ModularMachinery.EXECUTE_MANAGER.addParallelAsyncTask(this::doRecipeTick);
        }
    }

    @Override
    protected void checkRotation() {
        IBlockState state = getWorld().getBlockState(getPos());
        if (state.getBlock() instanceof BlockController) {
            this.parentController = (BlockController) state.getBlock();
            this.parentMachine = parentController.getParentMachine();
            this.controllerRotation = state.getValue(BlockController.FACING);
        } else {
            // wtf, where is the controller?
            ModularMachinery.log.warn("Invalid controller block at " + getPos() + " !");
            controllerRotation = EnumFacing.NORTH;
        }
    }

    private void doRecipeTick() {
        resetStructureCheckCounter();

        if (this.context == null) {
            //context preInit
            context = createContext(this.activeRecipe);
            context.setParallelism(this.activeRecipe.getParallelism());
        }

        CraftingStatus prevStatus = this.getCraftingStatus();
        MachineRecipe machineRecipe = this.activeRecipe.getRecipe();

        //检查预 Tick 事件是否阻止进一步运行。
        //Check if the PreTick event prevents further runs.
        if (!onPreTick()) {
            if (this.activeRecipe != null) {
                this.activeRecipe.tick(this, this.context);
                if (this.getCraftingStatus().isCrafting()) {
                    this.activeRecipe.setTick(Math.max(this.activeRecipe.getTick() - 1, 0));
                }
            }
            markForUpdateSync();
            return;
        }

        //当脚本修改了运行状态时，内部不再覆盖运行状态。
        //When scripts changed craftingStatus, it is no longer modified internally.
        if (prevStatus.equals(this.getCraftingStatus())) {
            this.craftingStatus = this.activeRecipe.tick(this, this.context);
        } else {
            this.activeRecipe.tick(this, this.context);
        }
        if (this.getCraftingStatus().isCrafting()) {
            onTick();
            if (this.activeRecipe.isCompleted()) {
                if (ModularMachinery.pluginServerCompatibleMode) {
                    ModularMachinery.EXECUTE_MANAGER.addSyncTask(this::onFinished);
                } else {
                    onFinished();
                }
            }
        } else if (machineRecipe.doesCancelRecipeOnPerTickFailure()) {
            this.activeRecipe = null;
            this.context = null;
        }
        markForUpdateSync();
    }

    @SuppressWarnings("unused")
    public BlockController getParentController() {
        return parentController;
    }

    @SuppressWarnings("unused")
    public DynamicMachine getParentMachine() {
        return parentMachine;
    }

    public boolean hasMachineTickEventHandlers() {
        List<IEventHandler<MachineEvent>> handlerList = this.foundMachine.getMachineEventHandlers(MachineTickEvent.class);
        return handlerList != null && !handlerList.isEmpty();
    }

    /**
     * <p>机器开始执行一个配方。</p>
     *
     * @param activeRecipe ActiveMachineRecipe
     * @param context      RecipeCraftingContext
     */
    public void onStart(ActiveMachineRecipe activeRecipe, RecipeCraftingContext context) {
        this.activeRecipe = activeRecipe;
        this.context = context;

        List<IEventHandler<RecipeEvent>> handlerList = this.activeRecipe.getRecipe().getRecipeEventHandlers(RecipeStartEvent.class);
        if (handlerList != null && !handlerList.isEmpty()) {
            for (IEventHandler<RecipeEvent> handler : handlerList) {
                RecipeStartEvent event = new RecipeStartEvent(this);
                handler.handle(event);
            }
        }
        activeRecipe.start(context);
    }

    /**
     * <p>机器在完成配方 Tick 后执行</p>
     *
     * @return 如果为 false，则进度停止增加，并在控制器状态栏输出原因
     */
    public boolean onPreTick() {
        List<IEventHandler<RecipeEvent>> handlerList = this.activeRecipe.getRecipe().getRecipeEventHandlers(RecipePreTickEvent.class);
        if (handlerList == null || handlerList.isEmpty()) return true;

        for (IEventHandler<RecipeEvent> handler : handlerList) {
            RecipePreTickEvent event = new RecipePreTickEvent(this);
            handler.handle(event);

            if (event.isPreventProgressing()) {
                setCraftingStatus(CraftingStatus.working(event.getReason()));
                return false;
            }
            if (event.isFailure()) {
                if (event.isDestructRecipe()) {
                    this.activeRecipe = null;
                    this.context = null;
                }
                setCraftingStatus(CraftingStatus.failure(event.getReason()));
                return false;
            }
        }

        return true;
    }

    /**
     * <p>与 {@code onPreTick()} 相似，但是可以销毁配方。</p>
     */
    public void onTick() {
        List<IEventHandler<RecipeEvent>> handlerList = this.activeRecipe.getRecipe().getRecipeEventHandlers(RecipeTickEvent.class);
        if (handlerList == null || handlerList.isEmpty()) return;

        for (IEventHandler<RecipeEvent> handler : handlerList) {
            RecipeTickEvent event = new RecipeTickEvent(this);
            handler.handle(event);
            if (event.isFailure()) {
                if (event.isDestructRecipe()) {
                    this.activeRecipe = null;
                    this.context = null;
                }
                setCraftingStatus(CraftingStatus.failure(event.getFailureReason()));
                return;
            }
        }
    }

    /**
     * <p>机械完成一个配方。</p>
     */
    public void onFinished() {
        List<IEventHandler<RecipeEvent>> handlerList = this.activeRecipe.getRecipe().getRecipeEventHandlers(RecipeFinishEvent.class);
        if (handlerList != null && !handlerList.isEmpty()) {
            for (IEventHandler<RecipeEvent> handler : handlerList) {
                RecipeFinishEvent event = new RecipeFinishEvent(this);
                handler.handle(event);
            }
        }

        this.context.finishCrafting();

        this.activeRecipe.reset();
        this.activeRecipe.setMaxParallelism(isParallelized() ? getMaxParallelism() : 1);
        this.context = createContext(this.activeRecipe);
        tryStartRecipe(context);
    }

    /**
     * <p>机器尝试开始执行一个配方。</p>
     *
     * @param context RecipeCraftingContext
     */
    private void tryStartRecipe(@Nonnull RecipeCraftingContext context) {
        RecipeCraftingContext.CraftingCheckResult tryResult = onCheck(context);

        if (tryResult.isSuccess()) {
            Sync.doSyncAction(() -> onStart(context.getActiveRecipe(), context));
            markForUpdateSync();
        } else {
            this.craftingStatus = CraftingStatus.failure(tryResult.getFirstErrorMessage(""));
            this.activeRecipe = null;
            this.context = null;

            createRecipeSearchTask();
        }
    }

    @Override
    public ActiveMachineRecipe getActiveRecipe() {
        return activeRecipe;
    }

    @Nullable
    @Override
    public ActiveMachineRecipe[] getActiveRecipeList() {
        return new ActiveMachineRecipe[]{activeRecipe};
    }

    @Override
    public boolean isWorking() {
        return getCraftingStatus().isCrafting();
    }

    public void cancelCrafting(String reason) {
        this.activeRecipe = null;
        this.context = null;
        this.craftingStatus = CraftingStatus.failure(reason);
    }

    @Override
    public void overrideStatusInfo(String newInfo) {
        this.getCraftingStatus().overrideStatusMessage(newInfo);
    }

    public void flushContextModifier() {
        if (context != null) {
            this.context.overrideModifier(MiscUtils.flatten(this.foundModifiers.values()));
            for (RecipeModifier modifier : customModifiers.values()) {
                this.context.addModifier(modifier);
            }
        }
    }

    @Override
    protected void onStructureFormed() {
        Sync.doSyncAction(() -> {
            if (parentController != null) {
                this.world.setBlockState(pos, parentController.getDefaultState().withProperty(BlockController.FACING, this.controllerRotation));
            } else {
                this.world.setBlockState(pos, BlocksMM.blockController.getDefaultState().withProperty(BlockController.FACING, this.controllerRotation));
            }
        });

        super.onStructureFormed();
    }

    @Override
    protected void resetMachine(boolean clearData) {
        super.resetMachine(clearData);
    }

    /**
     * 搜索并运行配方。
     *
     * @return 如果有配方正在运行，返回 true，否则 false。
     */
    private boolean searchAndStartRecipe() {
        if (searchTask != null) {
            if (!searchTask.isDone()) {
                return false;
            }

            //并发检查
            if (searchTask.getCurrentMachine() == getFoundMachine()) {
                try {
                    RecipeCraftingContext context = searchTask.get();
                    if (context != null) {
                        tryStartRecipe(context);
                        searchTask = null;
                        return true;
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
        return false;
    }

    private void createRecipeSearchTask() {
        searchTask = new RecipeSearchTask(this, getFoundMachine(), getMaxParallelism(), RecipeRegistry.getRecipesFor(foundMachine));
        TaskExecutor.FORK_JOIN_POOL.submit(searchTask);
    }

    @Override
    public void readCustomNBT(NBTTagCompound compound) {
        super.readCustomNBT(compound);

        if (compound.hasKey("activeRecipe")) {
            NBTTagCompound tag = compound.getCompoundTag("activeRecipe");
            ActiveMachineRecipe recipe = new ActiveMachineRecipe(tag);
            if (recipe.getRecipe() == null) {
                ModularMachinery.log.info("Couldn't find recipe named " + tag.getString("recipeName") + " for controller at " + getPos());
                this.activeRecipe = null;
            } else {
                this.activeRecipe = recipe;
            }
        } else {
            this.activeRecipe = null;
        }
    }

    @Override
    protected void readMachineNBT(NBTTagCompound compound) {
        if (compound.hasKey("parentMachine")) {
            ResourceLocation rl = new ResourceLocation(compound.getString("parentMachine"));
            parentMachine = MachineRegistry.getRegistry().getMachine(rl);
            if (parentMachine != null) {
                parentController = BlockController.MACHINE_CONTROLLERS.get(parentMachine);
            } else {
                ModularMachinery.log.info("Couldn't find machine named " + rl + " for controller at " + getPos());
            }
        }

        super.readMachineNBT(compound);
    }

    @Override
    public void writeCustomNBT(NBTTagCompound compound) {
        super.writeCustomNBT(compound);

        if (this.activeRecipe != null) {
            compound.setTag("activeRecipe", this.activeRecipe.serialize());
        }
    }

}
