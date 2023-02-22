package hellfirepvp.modularmachinery.common.integration.crafttweaker.event;

import crafttweaker.CraftTweakerAPI;
import crafttweaker.annotations.ZenRegister;
import crafttweaker.util.IEventHandler;
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

@ZenRegister
@ZenClass("mods.modularmachinery.MMEvents")
public class MMEvents {
    @ZenMethod
    public static void onStructureFormed(String machineRegistryName, IEventHandler<MachineStructureFormedEvent> function) {
        DynamicMachine machine = MachineRegistry.getRegistry().getMachine(new ResourceLocation("modularmachinery", machineRegistryName));
        if (machine != null) {
            machine.addMachineEventHandler(MachineStructureFormedEvent.class, function);
        } else {
            CraftTweakerAPI.logError("Cloud not find machine `modularmachinery:" + machineRegistryName + "`!");
        }
    }

    @ZenMethod
    public static void onMachineTick(String machineRegistryName, IEventHandler<MachineTickEvent> function) {
        DynamicMachine machine = MachineRegistry.getRegistry().getMachine(new ResourceLocation("modularmachinery", machineRegistryName));
        if (machine != null) {
            machine.addMachineEventHandler(MachineTickEvent.class, function);
        } else {
            CraftTweakerAPI.logError("Cloud not find machine `modularmachinery:" + machineRegistryName + "`!");
        }
    }

    @ZenMethod
    @SideOnly(Side.CLIENT)
    public static void onControllerGUIRender(String machineRegistryName, IEventHandler<ControllerGUIRenderEvent> function) {
        DynamicMachine machine = MachineRegistry.getRegistry().getMachine(new ResourceLocation("modularmachinery", machineRegistryName));
        if (machine != null) {
            machine.addMachineEventHandler(ControllerGUIRenderEvent.class, function);
        } else {
            CraftTweakerAPI.logError("Cloud not find machine `modularmachinery:" + machineRegistryName + "`!");
        }
    }
}
