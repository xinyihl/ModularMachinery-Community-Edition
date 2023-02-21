package hellfirepvp.modularmachinery.common.integration.crafttweaker.event.recipe;

import crafttweaker.annotations.ZenRegister;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.event.IMachineController;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenGetter;

@ZenRegister
@ZenClass("mods.modularmachinery.RecipeEvent")
public abstract class RecipeEvent {
    protected final IMachineController controller;

    public RecipeEvent(IMachineController controller) {
        this.controller = controller;
    }

    @ZenGetter("controller")
    public IMachineController getController() {
        return controller;
    }
}
