package hellfirepvp.modularmachinery.common.integration.crafttweaker.event;

import crafttweaker.CraftTweakerAPI;
import crafttweaker.annotations.ZenRegister;
import crafttweaker.util.IEventHandler;
import github.kasuminova.mmce.common.concurrent.Action;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.event.client.ControllerGUIRenderEvent;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.event.machine.MachineStructureFormedEvent;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.event.machine.MachineTickEvent;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.machine.MachineRegistry;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

import java.util.ArrayList;
import java.util.List;

@ZenRegister
@ZenClass("mods.modularmachinery.MMEvents")
public class MMEvents {
    public static final List<Action> WAIT_FOR_REGISTER_LIST = new ArrayList<>();

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
    public static void onMachineTick(String machineRegistryName, IEventHandler<MachineTickEvent> function) {
        WAIT_FOR_REGISTER_LIST.add(() -> {
            DynamicMachine machine = MachineRegistry.getRegistry().getMachine(new ResourceLocation(ModularMachinery.MODID, machineRegistryName));
            if (machine != null) {
                machine.addMachineEventHandler(MachineTickEvent.class, function);
            } else {
                CraftTweakerAPI.logError("Could not find machine `" + machineRegistryName + "`!");
            }
        });
    }

    @ZenMethod
    @SideOnly(Side.CLIENT)
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

    public static void loadAll() {
        for (Action waitForRegister : WAIT_FOR_REGISTER_LIST) {
            waitForRegister.doAction();
        }
    }
}
