package hellfirepvp.modularmachinery.common.integration.crafttweaker.event.client;

import crafttweaker.annotations.ZenRegister;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.IMachineController;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.event.machine.MachineEvent;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenSetter;

@ZenRegister
@ZenClass("mods.modularmachinery.ControllerGUIRenderEvent")
public class ControllerGUIRenderEvent extends MachineEvent {
    private String[] info = {};

    public ControllerGUIRenderEvent(IMachineController controller) {
        super(controller);
    }

    @ZenSetter("extraInfo")
    public void setExtraInfo(String... info) {
        this.info = info;
    }

    public String[] getInfo() {
        return info;
    }
}
