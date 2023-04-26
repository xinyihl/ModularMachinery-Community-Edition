/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.tiles;

import crafttweaker.util.IEventHandler;
import github.kasuminova.mmce.common.concurrent.Sync;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.block.BlockController;
import hellfirepvp.modularmachinery.common.crafting.ActiveMachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.MachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.helper.CraftingStatus;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.event.recipe.*;
import hellfirepvp.modularmachinery.common.lib.BlocksMM;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.machine.MachineRecipeThread;
import hellfirepvp.modularmachinery.common.machine.MachineRegistry;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import hellfirepvp.modularmachinery.common.util.BlockArrayCache;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.List;

/**
 * <p>完全重构的社区版机械控制器，拥有强大的异步逻辑和极低的性能消耗。</p>
 * <p>Completely refactored community edition mechanical controller with powerful asynchronous logic and extremely low performance consumption.</p>
 * TODO: This class is too large, consider improving readability.
 */
public class TileMachineController extends TileMultiblockMachineController {
    private MachineRecipeThread recipeThread = new MachineRecipeThread(this);
    private BlockController parentController = null;

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
    public void doControllerTick() {
        if (getWorld().getStrongPower(getPos()) > 0) {
            return;
        }
        if (!doStructureCheck() || !isStructureFormed()) {
            return;
        }

        tickExecutor = ModularMachinery.EXECUTE_MANAGER.addParallelAsyncTask(() -> {
            onMachineTick();
            if (doRecipeTick()) {
                markForUpdateSync();
            }
        }, usedTimeAvg());
    }

    protected boolean doRecipeTick() {
        MachineRecipeThread thread = this.recipeThread;
        if (thread.getActiveRecipe() == null) {
            thread.searchAndStartRecipe();
        }

        ActiveMachineRecipe activeRecipe = thread.getActiveRecipe();
        if (activeRecipe == null) {
            return false;
        }

        // If this thread previously failed in completing the recipe,
        // it retries to complete the recipe.
        if (thread.isWaitForFinish()) {
            // To prevent performance drain due to long output blocking,
            // try to complete the recipe every 10 Tick instead of every Tick.
            if (ticksExisted % 10 == 0) {
                thread.onFinished();
            }
            return true;
        }

        // PreTickEvent
        if (!onPreTick()) {
            return true;
        }
        // RecipeTick
        CraftingStatus status = thread.onTick();
        if (!status.isCrafting()) {
            boolean destruct = onFailure();
            if (destruct) {
                // Destruction recipe
                thread.setActiveRecipe(null).setContext(null).getSemiPermanentModifiers().clear();
            }
            return true;
        }
        // PostTickEvent
        if (!onPostTick()) {
            return true;
        }
        if (!activeRecipe.isCompleted()) {
            return true;
        }

        thread.onFinished();
        return true;
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

    @SuppressWarnings("unused")
    public BlockController getParentController() {
        return parentController;
    }

    @SuppressWarnings("unused")
    public DynamicMachine getParentMachine() {
        return parentMachine;
    }

    /**
     * <p>机器开始执行一个配方。</p>
     */
    public void onStart() {
        ActiveMachineRecipe activeRecipe = recipeThread.getActiveRecipe();
        List<IEventHandler<RecipeEvent>> handlerList = activeRecipe.getRecipe().getRecipeEventHandlers(RecipeStartEvent.class);
        if (handlerList != null && !handlerList.isEmpty()) {
            RecipeStartEvent event = new RecipeStartEvent(this);
            for (IEventHandler<RecipeEvent> handler : handlerList) {
                handler.handle(event);
            }
        }
        activeRecipe.start(recipeThread.getContext());
    }

    /**
     * <p>机器在完成配方 Tick 后执行</p>
     *
     * @return 如果为 false，则进度停止增加，并在控制器状态栏输出原因
     */
    public boolean onPreTick() {
        ActiveMachineRecipe activeRecipe = recipeThread.getActiveRecipe();
        List<IEventHandler<RecipeEvent>> handlerList = activeRecipe.getRecipe().getRecipeEventHandlers(RecipePreTickEvent.class);
        if (handlerList == null || handlerList.isEmpty()) return true;

        for (IEventHandler<RecipeEvent> handler : handlerList) {
            RecipePreTickEvent event = new RecipePreTickEvent(this);
            handler.handle(event);

            if (event.isPreventProgressing()) {
                activeRecipe.setTick(activeRecipe.getTick() - 1);
                recipeThread.setStatus(CraftingStatus.working(event.getReason()));
                return true;
            }
            if (event.isFailure()) {
                if (event.isDestructRecipe()) {
                    recipeThread.setActiveRecipe(null)
                            .setContext(null)
                            .setStatus(CraftingStatus.failure(event.getReason()))
                            .getSemiPermanentModifiers().clear();
                    return false;
                }
                recipeThread.setStatus(CraftingStatus.failure(event.getReason()));
                return false;
            }
        }

        return true;
    }

    /**
     * <p>与 {@code onPreTick()} 相似，但是可以销毁配方。</p>
     */
    public boolean onPostTick() {
        ActiveMachineRecipe activeRecipe = recipeThread.getActiveRecipe();
        List<IEventHandler<RecipeEvent>> handlerList = activeRecipe.getRecipe().getRecipeEventHandlers(RecipeTickEvent.class);
        if (handlerList == null || handlerList.isEmpty()) return true;

        for (IEventHandler<RecipeEvent> handler : handlerList) {
            RecipeTickEvent event = new RecipeTickEvent(this);
            handler.handle(event);
            if (event.isFailure()) {
                if (event.isDestructRecipe()) {
                    recipeThread.setActiveRecipe(null)
                            .setContext(null)
                            .setStatus(CraftingStatus.failure(event.getFailureReason()))
                            .getSemiPermanentModifiers().clear();
                    return false;
                }
                recipeThread.setStatus(CraftingStatus.failure(event.getFailureReason()));
                return false;
            }
        }
        return true;
    }

    /**
     * <p>运行配方失败时（例如跳电）触发，可能会触发多次。</p>
     *
     * @return true 为销毁配方（即为吞材料），false 则什么都不做。
     */
    public boolean onFailure() {
        ActiveMachineRecipe activeRecipe = recipeThread.getActiveRecipe();
        MachineRecipe recipe = activeRecipe.getRecipe();
        boolean destruct = recipe.doesCancelRecipeOnPerTickFailure();

        List<IEventHandler<RecipeEvent>> handlerList = recipe.getRecipeEventHandlers(RecipeFailureEvent.class);
        if (handlerList == null || handlerList.isEmpty()) {
            return destruct;
        }

        RecipeFailureEvent event = new RecipeFailureEvent(
                this, recipeThread.getStatus().getUnlocMessage(), destruct);
        for (IEventHandler<RecipeEvent> handler : handlerList) {
            handler.handle(event);
        }

        return event.isDestructRecipe();
    }

    /**
     * <p>机械完成一个配方。</p>
     */
    public void onFinished() {
        ActiveMachineRecipe activeRecipe = recipeThread.getActiveRecipe();
        List<IEventHandler<RecipeEvent>> handlerList = activeRecipe.getRecipe().getRecipeEventHandlers(RecipeFinishEvent.class);
        if (handlerList != null && !handlerList.isEmpty()) {
            RecipeFinishEvent event = new RecipeFinishEvent(this);
            for (IEventHandler<RecipeEvent> handler : handlerList) {
                handler.handle(event);
            }
        }
    }

    @Override
    protected void checkAllPatterns() {
        for (DynamicMachine machine : MachineRegistry.getRegistry()) {
            if (machine.isRequiresBlueprint() || machine.isFactoryOnly()) continue;
            if (matchesRotation(
                    BlockArrayCache.getBlockArrayCache(machine.getPattern(), controllerRotation),
                    machine, controllerRotation)) {
                onStructureFormed();
                break;
            }
        }
    }

    @Override
    public ActiveMachineRecipe getActiveRecipe() {
        return recipeThread.getActiveRecipe();
    }

    @Nullable
    @Override
    public ActiveMachineRecipe[] getActiveRecipeList() {
        return new ActiveMachineRecipe[]{recipeThread.getActiveRecipe()};
    }

    @Override
    public boolean isWorking() {
        return getControllerStatus().isCrafting();
    }

    @Override
    public void overrideStatusInfo(String newInfo) {
        this.getControllerStatus().overrideStatusMessage(newInfo);
    }

    public void flushContextModifier() {
        recipeThread.flushContextModifier();
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

    @Override
    public CraftingStatus getControllerStatus() {
        return recipeThread.getStatus();
    }

    @Override
    public void setControllerStatus(CraftingStatus status) {
        recipeThread.setStatus(status);
    }

    @Override
    public void readCustomNBT(NBTTagCompound compound) {
        super.readCustomNBT(compound);
        MachineRecipeThread thread = MachineRecipeThread.deserialize(compound, this);
        if (thread != null) {
            this.recipeThread = thread;
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
        this.recipeThread.serialize(compound);
    }

}
