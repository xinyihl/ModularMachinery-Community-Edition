package hellfirepvp.modularmachinery.common.integration.crafttweaker.event.recipe;

import crafttweaker.annotations.ZenRegister;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.IMachineController;
import hellfirepvp.modularmachinery.common.machine.factory.RecipeThread;
import stanhebben.zenscript.annotations.ZenClass;

@ZenRegister
@ZenClass("mods.modularmachinery.FactoryRecipeStartEvent")
public class FactoryRecipeStartEvent extends FactoryRecipeEvent {
    public FactoryRecipeStartEvent(RecipeThread recipeThread, IMachineController controller) {
        super(recipeThread, controller);
    }
}
