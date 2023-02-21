package hellfirepvp.modularmachinery.common.integration.crafttweaker.event.recipe;

import crafttweaker.annotations.ZenRegister;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.event.IMachineController;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenRegister
@ZenClass("mods.modularmachinery.RecipePreTickEvent")
public class RecipePreTickEvent extends RecipeEvent {
    private boolean preventProgressing = false;
    private String preventReason = "";

    public RecipePreTickEvent(IMachineController controller) {
        super(controller);
    }

    @ZenMethod
    public void preventProgressing(String reason) {
        this.preventProgressing = true;
        this.preventReason = reason;
    }

    public boolean isPreventProgressing() {
        return preventProgressing;
    }

    public String getPreventReason() {
        return preventReason;
    }
}
