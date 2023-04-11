package hellfirepvp.modularmachinery.common.integration.crafttweaker.event.recipe;

import crafttweaker.annotations.ZenRegister;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.IMachineController;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.event.machine.MachineEvent;
import stanhebben.zenscript.annotations.ZenClass;

@ZenRegister
@ZenClass("mods.modularmachinery.RecipeEvent")
public abstract class RecipeEvent extends MachineEvent {
    public RecipeEvent(IMachineController controller) {
        super(controller);
    }
}
