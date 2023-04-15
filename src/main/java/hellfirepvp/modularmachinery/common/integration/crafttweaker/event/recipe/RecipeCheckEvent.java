package hellfirepvp.modularmachinery.common.integration.crafttweaker.event.recipe;

import crafttweaker.annotations.ZenRegister;
import hellfirepvp.modularmachinery.common.crafting.ActiveMachineRecipe;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.IMachineController;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenRegister
@ZenClass("mods.modularmachinery.RecipeCheckEvent")
public class RecipeCheckEvent extends RecipeEvent {
    private boolean isFailure = false;
    private String failureReason = null;

    public RecipeCheckEvent(IMachineController controller, ActiveMachineRecipe recipe) {
        super(controller, recipe);
    }

    @ZenMethod
    public void setFailed(String reason) {
        this.isFailure = true;
        this.failureReason = reason;
    }

    public boolean isFailure() {
        return isFailure;
    }

    public String getFailureReason() {
        return failureReason;
    }
}
