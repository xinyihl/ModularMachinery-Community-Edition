package hellfirepvp.modularmachinery.common.integration.crafttweaker.event.recipe;

import crafttweaker.annotations.ZenRegister;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.event.IMachineController;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenRegister
@ZenClass("mods.modularmachinery.RecipeStartEvent")
public class RecipeStartEvent extends RecipeEvent {
    public RecipeStartEvent(IMachineController controller) {
        super(controller);
    }

    @ZenMethod
    void cancelCrafting(String reason) {
        controller.getController().cancelCrafting(reason);
    }
}
