package github.kasuminova.mmce.common.concurrent;

import hellfirepvp.modularmachinery.common.crafting.ActiveMachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.MachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.RecipeRegistry;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;

import java.util.concurrent.RecursiveTask;

public class RecipeSearchTask extends RecursiveTask<RecipeCraftingContext> {
    private final TileMultiblockMachineController controller;
    private final DynamicMachine currentMachine;
    private TileMultiblockMachineController.CraftingStatus status = null;

    public RecipeSearchTask(TileMultiblockMachineController controller, DynamicMachine currentMachine) {
        this.controller = controller;
        this.currentMachine = currentMachine;
    }

    @Override
    protected RecipeCraftingContext compute() {
        DynamicMachine foundMachine = controller.getFoundMachine();
        if (foundMachine == null) return null;
        Iterable<MachineRecipe> availableRecipes = RecipeRegistry.getRecipesFor(foundMachine);

        MachineRecipe highestValidity = null;
        RecipeCraftingContext.CraftingCheckResult highestValidityResult = null;
        float validity = 0F;

        for (MachineRecipe recipe : availableRecipes) {
            ActiveMachineRecipe activeRecipe = new ActiveMachineRecipe(recipe, controller.getMaxParallelism());
            RecipeCraftingContext context = controller.createContext(activeRecipe);
            RecipeCraftingContext.CraftingCheckResult result = controller.onCheck(context);
            if (result.isSuccess()) {
                //并发检查
                foundMachine = controller.getFoundMachine();
                if (foundMachine == null || !foundMachine.equals(currentMachine))
                    return null;

                controller.resetRecipeSearchRetryCount();

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
            status = TileMultiblockMachineController.CraftingStatus.failure(
                    highestValidityResult.getFirstErrorMessage(""));
        } else {
            status = TileMultiblockMachineController.CraftingStatus.failure(
                    TileMultiblockMachineController.Type.NO_RECIPE.getUnlocalizedDescription());
        }
        controller.incrementRecipeSearchRetryCount();

        return null;
    }

    public TileMultiblockMachineController.CraftingStatus getStatus() {
        return status;
    }

    public DynamicMachine getCurrentMachine() {
        return currentMachine;
    }
}
