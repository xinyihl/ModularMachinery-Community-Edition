package hellfirepvp.modularmachinery.common.integration.crafttweaker.event.recipe;

import crafttweaker.annotations.ZenRegister;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.IMachineController;
import stanhebben.zenscript.annotations.ZenClass;

@ZenRegister
@ZenClass("mods.modularmachinery.RecipeStartEvent")
public class RecipeStartEvent extends RecipeEvent {
    public RecipeStartEvent(IMachineController controller) {
        super(controller, controller.getActiveRecipeList()[0]);
    }

}
