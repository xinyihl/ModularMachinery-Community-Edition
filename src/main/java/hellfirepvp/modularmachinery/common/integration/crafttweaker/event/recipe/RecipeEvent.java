package hellfirepvp.modularmachinery.common.integration.crafttweaker.event.recipe;

import crafttweaker.annotations.ZenRegister;
import hellfirepvp.modularmachinery.common.crafting.ActiveMachineRecipe;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.IMachineController;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.event.machine.MachineEvent;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenGetter;

@ZenRegister
@ZenClass("mods.modularmachinery.RecipeEvent")
public abstract class RecipeEvent extends MachineEvent {
    private final ActiveMachineRecipe machineRecipe;
    public RecipeEvent(IMachineController controller, ActiveMachineRecipe machineRecipe) {
        super(controller);
        this.machineRecipe = machineRecipe;
    }

    @ZenGetter("activeRecipe")
    public ActiveMachineRecipe getRecipe() {
        return machineRecipe;
    }
}
