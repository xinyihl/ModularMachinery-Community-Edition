package github.kasuminova.mmce.common.event.client;

import crafttweaker.annotations.ZenRegister;
import github.kasuminova.mmce.common.event.machine.MachineEvent;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenGetter;
import stanhebben.zenscript.annotations.ZenSetter;

@ZenRegister
@ZenClass("mods.modularmachinery.ControllerModelGetEvent")
public class ControllerModelGetEvent extends MachineEvent {
    private String modelName = "";

    public ControllerModelGetEvent(final TileMultiblockMachineController controller) {
        super(controller);
    }

    @ZenGetter("modelName")
    public String getModelName() {
        return modelName;
    }

    @ZenSetter("modelName")
    public void setModelName(final String modelName) {
        this.modelName = modelName;
    }
}
