package github.kasuminova.mmce.common.concurrent;

import hellfirepvp.modularmachinery.common.crafting.ActiveMachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.MachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.RecipeRegistry;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.tiles.TileMachineController;

import java.util.concurrent.RecursiveTask;

public class RecipeSearchTask extends RecursiveTask<RecipeCraftingContext> {
    private final TileMachineController controller;
    private final DynamicMachine currentMachine;

    public RecipeSearchTask(TileMachineController controller, DynamicMachine currentMachine) {
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
            controller.setCraftingStatus(TileMachineController.CraftingStatus.failure(highestValidityResult.getFirstErrorMessage("")));
        } else {
            controller.setCraftingStatus(TileMachineController.CraftingStatus.failure(TileMachineController.Type.NO_RECIPE.getUnlocalizedDescription()));
        }
        controller.incrementRecipeSearchRetryCount();

        return null;
    }

    public TileMachineController getController() {
        return controller;
    }

    public DynamicMachine getCurrentMachine() {
        return currentMachine;
    }
}
