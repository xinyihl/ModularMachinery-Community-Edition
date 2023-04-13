package hellfirepvp.modularmachinery.common.integration.crafttweaker.event.recipe;

import crafttweaker.annotations.ZenRegister;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.IMachineController;
import hellfirepvp.modularmachinery.common.machine.factory.RecipeThread;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenRegister
@ZenClass("mods.modularmachinery.FactoryRecipeTickEvent")
public class FactoryRecipeTickEvent extends FactoryRecipeEvent {
    private boolean isFailure = false;
    private boolean destructRecipe = false;
    private String failureReason = null;
    public FactoryRecipeTickEvent(RecipeThread recipeThread, IMachineController controller) {
        super(recipeThread, controller);
    }

    @ZenMethod
    public void setFailed(boolean destructRecipe, String reason) {
        this.isFailure = true;
        this.destructRecipe = destructRecipe;
        this.failureReason = reason;
    }

    public boolean isFailure() {
        return isFailure;
    }

    public boolean isDestructRecipe() {
        return destructRecipe;
    }

    public String getFailureReason() {
        return failureReason;
    }
}
