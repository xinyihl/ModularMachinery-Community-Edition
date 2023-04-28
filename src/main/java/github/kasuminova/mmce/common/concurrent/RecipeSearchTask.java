package github.kasuminova.mmce.common.concurrent;

import hellfirepvp.modularmachinery.common.crafting.ActiveMachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.MachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.helper.CraftingStatus;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;

import java.util.concurrent.RecursiveTask;

public class RecipeSearchTask extends RecursiveTask<RecipeCraftingContext> {
    protected final TileMultiblockMachineController controller;
    protected final DynamicMachine currentMachine;
    protected final int maxParallelism;
    protected final Iterable<MachineRecipe> recipeList;
    protected CraftingStatus status = CraftingStatus.IDLE;

    public RecipeSearchTask(TileMultiblockMachineController controller, DynamicMachine currentMachine, int maxParallelism, Iterable<MachineRecipe> recipeList) {
        this.controller = controller;
        this.currentMachine = currentMachine;
        this.maxParallelism = maxParallelism;
        this.recipeList = recipeList;
    }

    @Override
    protected RecipeCraftingContext compute() {
        DynamicMachine foundMachine = controller.getFoundMachine();
        if (foundMachine == null) return null;

        MachineRecipe highestValidity = null;
        RecipeCraftingContext.CraftingCheckResult highestValidityResult = null;
        float validity = 0F;

        for (MachineRecipe recipe : recipeList) {
            ActiveMachineRecipe activeRecipe = new ActiveMachineRecipe(recipe, maxParallelism);
            RecipeCraftingContext context = controller.createContext(activeRecipe);
            RecipeCraftingContext.CraftingCheckResult result = controller.onCheck(context);
            if (result.isSuccess()) {
                //并发检查
                foundMachine = controller.getFoundMachine();
                if (foundMachine == null || !foundMachine.equals(currentMachine))
                    return null;

                return context;
            } else if (highestValidity == null ||
                    (result.getValidity() >= 0.5F && result.getValidity() > validity)) {
                highestValidity = recipe;
                highestValidityResult = result;
                validity = result.getValidity();
            }
        }

        //并发检查
        foundMachine = controller.getFoundMachine();
        if (foundMachine == null || !foundMachine.equals(currentMachine))
            return null;

        if (highestValidity != null) {
            status = CraftingStatus.failure(
                    highestValidityResult.getFirstErrorMessage(""));
        } else {
            status = CraftingStatus.failure(
                    TileMultiblockMachineController.Type.NO_RECIPE.getUnlocalizedDescription());
        }

        return null;
    }

    public CraftingStatus getStatus() {
        return status;
    }

    public DynamicMachine getCurrentMachine() {
        return currentMachine;
    }
}
