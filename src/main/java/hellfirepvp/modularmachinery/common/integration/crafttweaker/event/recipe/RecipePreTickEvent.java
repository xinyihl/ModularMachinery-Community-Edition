package hellfirepvp.modularmachinery.common.integration.crafttweaker.event.recipe;

import crafttweaker.annotations.ZenRegister;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.IMachineController;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenRegister
@ZenClass("mods.modularmachinery.RecipePreTickEvent")
public class RecipePreTickEvent extends RecipeEvent {
    private boolean preventProgressing = false;
    private boolean isFailure = false;
    private boolean destructRecipe = false;
    private String reason = null;

    public RecipePreTickEvent(IMachineController controller) {
        super(controller);
    }

    @ZenMethod
    public void preventProgressing(String reason) {
        this.preventProgressing = true;
        this.reason = reason;
    }

    @ZenMethod
    public void setFailed(boolean destructRecipe, String reason) {
        this.isFailure = true;
        this.destructRecipe = destructRecipe;
        this.reason = reason;
    }

    public boolean isFailure() {
        return isFailure;
    }

    public boolean isDestructRecipe() {
        return destructRecipe;
    }

    public boolean isPreventProgressing() {
        return preventProgressing;
    }

    public String getReason() {
        return reason;
    }
}
