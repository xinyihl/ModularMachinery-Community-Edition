package hellfirepvp.modularmachinery.common.integration.crafttweaker.event.client;

import crafttweaker.annotations.ZenRegister;
import github.kasuminova.mmce.common.event.machine.MachineEvent;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenGetter;
import stanhebben.zenscript.annotations.ZenSetter;

@ZenRegister
@ZenClass("mods.modularmachinery.ControllerGUIRenderEvent")
public class ControllerGUIRenderEvent extends MachineEvent {
    private String[] extraInfo = {};

    public ControllerGUIRenderEvent(TileMultiblockMachineController controller) {
        super(controller);
    }

    @ZenSetter("extraInfo")
    public void setExtraInfo(String... info) {
        this.extraInfo = info;
    }

    @ZenGetter("extraInfo")
    public String[] getExtraInfo() {
        return extraInfo;
    }
}
