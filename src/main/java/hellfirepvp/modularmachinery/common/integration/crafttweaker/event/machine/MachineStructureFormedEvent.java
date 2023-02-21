package hellfirepvp.modularmachinery.common.integration.crafttweaker.event.machine;

import crafttweaker.annotations.ZenRegister;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.event.IMachineController;
import stanhebben.zenscript.annotations.ZenClass;

@ZenRegister
@ZenClass("mods.modularmachinery.MachineStructureFormedEvent")
public class MachineStructureFormedEvent extends MachineEvent {
    public MachineStructureFormedEvent(IMachineController controller) {
        super(controller);
    }
}
