package hellfirepvp.modularmachinery.common.integration.crafttweaker.event;

import crafttweaker.CraftTweakerAPI;
import crafttweaker.annotations.ZenRegister;
import crafttweaker.util.IEventHandler;
import github.kasuminova.mmce.common.concurrent.Action;
import github.kasuminova.mmce.common.event.Phase;
import github.kasuminova.mmce.common.event.client.ControllerGUIRenderEvent;
import github.kasuminova.mmce.common.event.machine.MachineStructureFormedEvent;
import github.kasuminova.mmce.common.event.machine.MachineTickEvent;
import github.kasuminova.mmce.common.event.machine.SmartInterfaceUpdateEvent;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.machine.MachineRegistry;
import net.minecraft.util.ResourceLocation;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

import java.util.LinkedList;
import java.util.List;

@ZenRegister
@ZenClass("mods.modularmachinery.MMEvents")
public class MMEvents {
    public static final List<Action> WAIT_FOR_REGISTER_LIST = new LinkedList<>();

    @ZenMethod
    public static void onStructureFormed(String machineRegistryName, IEventHandler<MachineStructureFormedEvent> function) {
        WAIT_FOR_REGISTER_LIST.add(() -> {
            DynamicMachine machine = MachineRegistry.getRegistry().getMachine(new ResourceLocation(ModularMachinery.MODID, machineRegistryName));
            if (machine != null) {
                machine.addMachineEventHandler(MachineStructureFormedEvent.class, function);
            } else {
                CraftTweakerAPI.logError("Could not find machine `" + machineRegistryName + "`!");
            }
        });
    }

    @ZenMethod
    public static void onMachinePreTick(String machineRegistryName, IEventHandler<MachineTickEvent> function) {
        WAIT_FOR_REGISTER_LIST.add(() -> {
            DynamicMachine machine = MachineRegistry.getRegistry().getMachine(new ResourceLocation(ModularMachinery.MODID, machineRegistryName));
            if (machine != null) {
                machine.addMachineEventHandler(MachineTickEvent.class, event -> {
                    if (event.phase != Phase.START) {
                        return;
                    }
                    function.handle(event);
                });
            } else {
                CraftTweakerAPI.logError("Could not find machine `" + machineRegistryName + "`!");
            }
        });
    }

    @ZenMethod
    public static void onMachinePostTick(String machineRegistryName, IEventHandler<MachineTickEvent> function) {
        WAIT_FOR_REGISTER_LIST.add(() -> {
            DynamicMachine machine = MachineRegistry.getRegistry().getMachine(new ResourceLocation(ModularMachinery.MODID, machineRegistryName));
            if (machine != null) {
                machine.addMachineEventHandler(MachineTickEvent.class, event -> {
                    if (event.phase != Phase.END) {
                        return;
                    }
                    function.handle(event);
                });
            } else {
                CraftTweakerAPI.logError("Could not find machine `" + machineRegistryName + "`!");
            }
        });
    }

    @ZenMethod
    @Deprecated
    public static void onMachineTick(String machineRegistryName, IEventHandler<MachineTickEvent> function) {
        CraftTweakerAPI.logWarning("[ModularMachinery] Deprecated method onMachineTick()! Consider using onMachinePostTick()");
        onMachinePostTick(machineRegistryName, function);
    }

    @ZenMethod
    public static void onControllerGUIRender(String machineRegistryName, IEventHandler<ControllerGUIRenderEvent> function) {
        WAIT_FOR_REGISTER_LIST.add(() -> {
            DynamicMachine machine = MachineRegistry.getRegistry().getMachine(new ResourceLocation(ModularMachinery.MODID, machineRegistryName));
            if (machine != null) {
                machine.addMachineEventHandler(ControllerGUIRenderEvent.class, function);
            } else {
                CraftTweakerAPI.logError("Could not find machine `" + machineRegistryName + "`!");
            }
        });
    }

    @ZenMethod
    public static void onSmartInterfaceUpdate(String machineRegistryName, IEventHandler<SmartInterfaceUpdateEvent> function) {
        WAIT_FOR_REGISTER_LIST.add(() -> {
            DynamicMachine machine = MachineRegistry.getRegistry().getMachine(new ResourceLocation(ModularMachinery.MODID, machineRegistryName));
            if (machine != null) {
                machine.addMachineEventHandler(SmartInterfaceUpdateEvent.class, function);
            } else {
                CraftTweakerAPI.logError("Could not find machine `" + machineRegistryName + "`!");
            }
        });
    }

    public static void registryAll() {
        for (Action waitForRegister : WAIT_FOR_REGISTER_LIST) {
            waitForRegister.doAction();
        }
        WAIT_FOR_REGISTER_LIST.clear();
    }
}
