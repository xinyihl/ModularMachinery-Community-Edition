package hellfirepvp.modularmachinery.common.integration.crafttweaker.event.recipe;

import crafttweaker.annotations.ZenRegister;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.IMachineController;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenRegister
@ZenClass("mods.modularmachinery.RecipeTickEvent")
public class RecipeTickEvent extends RecipeEvent {

    private boolean isFailure = false;
    private boolean destructRecipe = false;
    private String failureReason = null;
    public RecipeTickEvent(IMachineController controller) {
        super(controller);
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
