package hellfirepvp.modularmachinery.common.integration.crafttweaker;

import crafttweaker.CraftTweakerAPI;
import crafttweaker.annotations.ZenRegister;
import github.kasuminova.mmce.common.concurrent.Action;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.machine.MachineRegistry;
import hellfirepvp.modularmachinery.common.machine.factory.RecipeThread;
import hellfirepvp.modularmachinery.common.util.SmartInterfaceType;
import net.minecraft.util.ResourceLocation;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

import java.util.LinkedList;
import java.util.List;

@ZenRegister
@ZenClass("mods.modularmachinery.MachineModifier")
public class MachineModifier {
    public static final List<Action> WAIT_FOR_MODIFY = new LinkedList<>();

    @ZenMethod
    public static void addSmartInterfaceType(String machineName, SmartInterfaceType type) {
        WAIT_FOR_MODIFY.add(() -> {
            DynamicMachine machine = MachineRegistry.getRegistry().getMachine(new ResourceLocation(ModularMachinery.MODID, machineName));
            if (machine == null) {
                CraftTweakerAPI.logError("Could not find machine `" + machineName + "`!");
                return;
            }
            if (!machine.hasSmartInterfaceType(type.getType())) {
                machine.addSmartInterfaceType(type);
            } else {
                CraftTweakerAPI.logWarning("[ModularMachinery] DynamicMachine `" + machine.getRegistryName() + "` is already has SmartInterfaceType `" + type.getType() + "`!");
            }
        });
    }

    @ZenMethod
    public static void setMaxParallelism(String machineName, int maxParallelism) {
        if (maxParallelism < 1) {
            CraftTweakerAPI.logError("Max Parallelism must larger than 1!");
        }
        WAIT_FOR_MODIFY.add(() -> {
            DynamicMachine machine = MachineRegistry.getRegistry().getMachine(new ResourceLocation(ModularMachinery.MODID, machineName));
            if (machine == null) {
                CraftTweakerAPI.logError("Could not find machine `" + machineName + "`!");
                return;
            }
            machine.setMaxParallelism(maxParallelism);
        });
    }

    @ZenMethod
    public static void setMaxThreads(String machineName, int maxThreads) {
        // Maybe the author only wanted to use the core thread?
        if (maxThreads < 0) {
            CraftTweakerAPI.logError("Max Threads must larger than 0!");
        }
        WAIT_FOR_MODIFY.add(() -> {
            DynamicMachine machine = MachineRegistry.getRegistry().getMachine(new ResourceLocation(ModularMachinery.MODID, machineName));
            if (machine == null) {
                CraftTweakerAPI.logError("Could not find machine `" + machineName + "`!");
                return;
            }
            machine.setMaxThreads(maxThreads);
        });
    }

    @ZenMethod
    public static void addCoreThread(String machineName, RecipeThread thread) {
        WAIT_FOR_MODIFY.add(() -> {
            DynamicMachine machine = MachineRegistry.getRegistry().getMachine(new ResourceLocation(ModularMachinery.MODID, machineName));
            if (machine == null) {
                CraftTweakerAPI.logError("Could not find machine `" + machineName + "`!");
                return;
            }
            machine.addCoreThread(thread);
        });
    }

    public static void loadAll() {
        for (Action waitForRegister : WAIT_FOR_MODIFY) {
            waitForRegister.doAction();
        }
    }
}
