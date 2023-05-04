package github.kasuminova.mmce.common.event.machine;

import crafttweaker.annotations.ZenRegister;
import crafttweaker.util.IEventHandler;
import github.kasuminova.mmce.common.helper.IMachineController;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.Event;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenGetter;
import stanhebben.zenscript.annotations.ZenSetter;

import java.util.List;

@ZenRegister
@ZenClass("mods.modularmachinery.MachineEvent")
public abstract class MachineEvent extends Event {
    protected final TileMultiblockMachineController controller;

    public MachineEvent(TileMultiblockMachineController controller) {
        this.controller = controller;
    }

    @ZenGetter("controller")
    public IMachineController getController() {
        return controller;
    }

    public void postEvent() {
        MinecraftForge.EVENT_BUS.post(this);
        if (!isCanceled()) {
            postCrTEvent();
        }
    }

    public void postCrTEvent() {
        DynamicMachine foundMachine = controller.getFoundMachine();
        if (foundMachine == null) {
            return;
        }
        List<IEventHandler<MachineEvent>> handlers = foundMachine.getMachineEventHandlers().get(getClass());
        if (handlers == null) {
            return;
        }

        for (IEventHandler<MachineEvent> handler : handlers) {
            handler.handle(this);
            if (isCanceled()) {
                break;
            }
        }
    }

    @Override
    @ZenSetter("canceled")
    public void setCanceled(boolean cancel) {
        super.setCanceled(cancel);
    }

    @Override
    public boolean isCancelable() {
        return true;
    }

}
