package hellfirepvp.modularmachinery.common.integration.crafttweaker;

import crafttweaker.CraftTweakerAPI;
import crafttweaker.annotations.ZenRegister;
import github.kasuminova.mmce.client.model.DynamicMachineModelRegistry;
import github.kasuminova.mmce.client.model.MachineControllerModel;
import github.kasuminova.mmce.common.util.concurrent.Action;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.machine.MachineRegistry;
import hellfirepvp.modularmachinery.common.machine.factory.FactoryRecipeThread;
import hellfirepvp.modularmachinery.common.util.SmartInterfaceType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.FMLCommonHandler;
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
    public static void setInternalParallelism(String machineName, int parallelism) {
        if (parallelism < 0) {
            CraftTweakerAPI.logError("Max Parallelism must larger than 0!");
        }
        WAIT_FOR_MODIFY.add(() -> {
            DynamicMachine machine = MachineRegistry.getRegistry().getMachine(new ResourceLocation(ModularMachinery.MODID, machineName));
            if (machine == null) {
                CraftTweakerAPI.logError("Could not find machine `" + machineName + "`!");
                return;
            }
            machine.setInternalParallelism(parallelism);
        });
    }

    @ZenMethod
    public static void setMaxThreads(String machineName, int maxThreads) {
        // Maybe the author only wanted to use the core thread?
        if (maxThreads < 0) {
            CraftTweakerAPI.logError("Max Threads must larger than or equal 0!");
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
    public static void addCoreThread(String machineName, FactoryRecipeThread thread) {
        WAIT_FOR_MODIFY.add(() -> {
            DynamicMachine machine = MachineRegistry.getRegistry().getMachine(new ResourceLocation(ModularMachinery.MODID, machineName));
            if (machine == null) {
                CraftTweakerAPI.logError("Could not find machine `" + machineName + "`!");
                return;
            }
            machine.addCoreThread(thread);
        });
    }

    @ZenMethod
    public static void registerMachineGeoModel(String machineName, String modelLocation, String textureLocation, String animationFileLocation) {
        if (FMLCommonHandler.instance().getSide().isServer()) {
            return;
        }

        WAIT_FOR_MODIFY.add(() -> {
            DynamicMachine machine = MachineRegistry.getRegistry().getMachine(new ResourceLocation(ModularMachinery.MODID, machineName));
            if (machine == null) {
                CraftTweakerAPI.logError("Could not find machine `" + machineName + "`!");
                return;
            }

            ResourceLocation modelLocationRL = new ResourceLocation(modelLocation);
            ResourceLocation textureLocationRL = new ResourceLocation(textureLocation);
            ResourceLocation animationFileLocationRL = new ResourceLocation(animationFileLocation);

            if (modelLocationRL.getNamespace().equals("minecraft")) {
                modelLocationRL = new ResourceLocation(ModularMachinery.MODID, modelLocationRL.getPath());
            }
            if (textureLocationRL.getNamespace().equals("minecraft")) {
                textureLocationRL = new ResourceLocation(ModularMachinery.MODID, textureLocationRL.getPath());
            }
            if (animationFileLocationRL.getNamespace().equals("minecraft")) {
                animationFileLocationRL = new ResourceLocation(ModularMachinery.MODID, animationFileLocationRL.getPath());
            }

            DynamicMachineModelRegistry.INSTANCE.registerGeoModel(machine, new MachineControllerModel(modelLocationRL, textureLocationRL, animationFileLocationRL));
        });
    }

    public static void loadAll() {
        for (Action waitForRegister : WAIT_FOR_MODIFY) {
            waitForRegister.doAction();
        }
        WAIT_FOR_MODIFY.clear();
    }
}
