package hellfirepvp.modularmachinery.common.integration.crafttweaker.event.machine;

import crafttweaker.annotations.ZenRegister;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.event.IMachineController;
import stanhebben.zenscript.annotations.ZenClass;

@ZenRegister
@ZenClass("mods.modularmachinery.MachineTickEvent")
public class MachineTickEvent extends MachineEvent {
    public MachineTickEvent(IMachineController controller) {
        super(controller);
    }
}
