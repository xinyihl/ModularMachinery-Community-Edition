package hellfirepvp.modularmachinery.common.integration.crafttweaker.event.machine;

import crafttweaker.annotations.ZenRegister;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.IMachineController;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenGetter;

@ZenRegister
@ZenClass("mods.modularmachinery.MachineEvent")
public abstract class MachineEvent {
    protected final IMachineController controller;

    public MachineEvent(IMachineController controller) {
        this.controller = controller;
    }

    @ZenGetter("controller")
    public IMachineController getController() {
        return controller;
    }
}
