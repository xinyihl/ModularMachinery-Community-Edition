/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.tiles;

import github.kasuminova.mmce.common.event.Phase;
import github.kasuminova.mmce.common.event.recipe.RecipeFailureEvent;
import github.kasuminova.mmce.common.event.recipe.RecipeFinishEvent;
import github.kasuminova.mmce.common.event.recipe.RecipeStartEvent;
import github.kasuminova.mmce.common.event.recipe.RecipeTickEvent;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.block.BlockController;
import hellfirepvp.modularmachinery.common.crafting.ActiveMachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.MachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.helper.CraftingStatus;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.lib.BlocksMM;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.machine.MachineRecipeThread;
import hellfirepvp.modularmachinery.common.machine.MachineRegistry;
import hellfirepvp.modularmachinery.common.machine.RecipeThread;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import hellfirepvp.modularmachinery.common.util.BlockArrayCache;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;

/**
 * <p>完全重构的社区版机械控制器，拥有强大的异步逻辑和极低的性能消耗。</p>
 * <p>Completely refactored community edition mechanical controller with powerful asynchronous logic and extremely low performance consumption.</p>
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

        tickExecutor = ModularMachinery.EXECUTE_MANAGER.addParallelAsyncTask(() -> {
            if (!doStructureCheck() || !isStructureFormed()) {
                return;
            }

            onMachineTick(Phase.START);

            final boolean prevWorkingStatus = isWorking();

            if (doRecipeTick()) {
                markForUpdateSync();
            }

            final boolean workingStatus = isWorking();
            if (prevWorkingStatus != workingStatus) {
                updateStatedMachineComponentSync(workingStatus);
            }

            onMachineTick(Phase.END);
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

        if (thread.getContext() == null) {
            thread.setContext(thread.createContext(activeRecipe));
        }

        // PreTickEvent
        new RecipeTickEvent(this, thread, Phase.START).postEvent();

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
        new RecipeTickEvent(this, thread, Phase.END).postEvent();

        if (thread.isCompleted()) {
            thread.onFinished();
        }
        return true;
    }

    @Override
    protected void updateComponents() {
        super.updateComponents();
        RecipeCraftingContext ctx = this.recipeThread.getContext();
        if (ctx == null) {
            return;
        }
        ctx.updateComponents(this.foundComponents);
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
        new RecipeStartEvent(this, recipeThread).postEvent();
        ActiveMachineRecipe activeRecipe = recipeThread.getActiveRecipe();
        activeRecipe.start(recipeThread.getContext());
    }

    /**
     * <p>运行配方失败时（例如跳电）触发，可能会触发多次。</p>
     *
     * @return true 为销毁配方（即为吞材料），false 则什么都不做。
     */
    public boolean onFailure() {
        ActiveMachineRecipe activeRecipe = recipeThread.getActiveRecipe();
        if (activeRecipe == null) {
            return false;
        }

        MachineRecipe recipe = activeRecipe.getRecipe();
        RecipeFailureEvent event = new RecipeFailureEvent(
                this, recipeThread, recipeThread.getStatus().getUnlocMessage(), recipe.doesCancelRecipeOnPerTickFailure());
        event.postEvent();

        return event.isDestructRecipe();
    }

    /**
     * <p>机械完成一个配方。</p>
     */
    public void onFinished() {
        new RecipeFinishEvent(this, recipeThread).postEvent();
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
    public RecipeThread[] getRecipeThreadList() {
        return new RecipeThread[]{recipeThread};
    }

    @Override
    public boolean isWorking() {
        return getControllerStatus().isCrafting();
    }

    @Override
    public void addModifier(final String key, final RecipeModifier modifier) {
        recipeThread.addModifier(key, modifier);
    }

    @Override
    public void removeModifier(final String key) {
        recipeThread.removeModifier(key);
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
        if (world.getBlockState(getPos()).getBlock() != parentController) {
            ModularMachinery.EXECUTE_MANAGER.addSyncTask(() -> {
                if (parentController != null) {
                    this.world.setBlockState(pos, parentController.getDefaultState().withProperty(BlockController.FACING, this.controllerRotation));
                } else {
                    this.world.setBlockState(pos, BlocksMM.blockController.getDefaultState().withProperty(BlockController.FACING, this.controllerRotation));
                }
            });
        }

        super.onStructureFormed();
    }

    @Override
    protected void resetMachine(boolean clearData) {
        super.resetMachine(clearData);
        this.recipeThread.setActiveRecipe(null).setContext(null).getSemiPermanentModifiers().clear();
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
