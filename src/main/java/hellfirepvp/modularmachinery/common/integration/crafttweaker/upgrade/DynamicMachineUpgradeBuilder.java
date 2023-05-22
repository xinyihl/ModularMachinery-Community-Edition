package hellfirepvp.modularmachinery.common.integration.crafttweaker.upgrade;

import crafttweaker.CraftTweakerAPI;
import crafttweaker.annotations.ZenRegister;
import github.kasuminova.mmce.common.event.Phase;
import github.kasuminova.mmce.common.event.client.ControllerGUIRenderEvent;
import github.kasuminova.mmce.common.event.machine.MachineEvent;
import github.kasuminova.mmce.common.event.machine.MachineStructureFormedEvent;
import github.kasuminova.mmce.common.event.machine.MachineTickEvent;
import github.kasuminova.mmce.common.event.machine.SmartInterfaceUpdateEvent;
import github.kasuminova.mmce.common.event.recipe.*;
import github.kasuminova.mmce.common.upgrade.MachineUpgrade;
import github.kasuminova.mmce.common.upgrade.SimpleDynamicMachineUpgrade;
import github.kasuminova.mmce.common.upgrade.SimpleMachineUpgrade;
import github.kasuminova.mmce.common.upgrade.UpgradeType;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.helper.IFunction;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.helper.UpgradeEventHandlerCT;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.machine.MachineRegistry;
import net.minecraft.util.ResourceLocation;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenRegister
@ZenClass("mods.modularmachinery.DynamicMachineUpgradeBuilder")
public class DynamicMachineUpgradeBuilder {
    private final SimpleDynamicMachineUpgrade machineUpgrade;

    public DynamicMachineUpgradeBuilder(final SimpleDynamicMachineUpgrade machineUpgrade) {
        this.machineUpgrade = machineUpgrade;
    }

    @ZenMethod
    public static DynamicMachineUpgradeBuilder newBuilder(String name, String localizedName, int level, int maxStack) {
        if (SimpleMachineUpgrade.getUpgrade(name) != null) {
            CraftTweakerAPI.logError("[ModularMachinery] Already registered SimpleMachineUpgrade " + name + '!');
            return null;
        }
        return new DynamicMachineUpgradeBuilder(new SimpleDynamicMachineUpgrade(new UpgradeType(name, localizedName, level, maxStack)));
    }

    @ZenMethod
    public DynamicMachineUpgradeBuilder setDescriptionHandler(IFunction<SimpleDynamicMachineUpgrade, String[]> handler) {
        machineUpgrade.setDescriptionHandler(handler);
        return this;
    }

    @ZenMethod
    public DynamicMachineUpgradeBuilder setBusGUIDescriptionHandler(IFunction<SimpleDynamicMachineUpgrade, String[]> handler) {
        machineUpgrade.setBusGUIDescriptionHandler(handler);
        return this;
    }

    @ZenMethod
    public DynamicMachineUpgradeBuilder addCompatibleMachines(String... machineNames) {
        for (final String machineName : machineNames) {
            DynamicMachine machine = MachineRegistry.getRegistry().getMachine(new ResourceLocation(ModularMachinery.MODID, machineName));
            if (machine == null) {
                CraftTweakerAPI.logError("Cloud not found machine " + machineName);
                continue;
            }
            machineUpgrade.getType().addCompatibleMachine(machine);
        }
        return this;
    }

    @ZenMethod
    public DynamicMachineUpgradeBuilder addIncompatibleMachines(String... machineNames) {
        for (final String machineName : machineNames) {
            DynamicMachine machine = MachineRegistry.getRegistry().getMachine(new ResourceLocation(ModularMachinery.MODID, machineName));
            if (machine == null) {
                CraftTweakerAPI.logError("Cloud not found machine " + machineName);
                continue;
            }
            machineUpgrade.getType().addIncompatibleMachine(machine);
        }
        return this;
    }

    @ZenMethod
    public DynamicMachineUpgradeBuilder addRecipeCheckHandler(UpgradeEventHandlerCT handler) {
        addEventHandler(RecipeCheckEvent.class, handler);
        return this;
    }

    @ZenMethod
    public DynamicMachineUpgradeBuilder addRecipeStartHandler(UpgradeEventHandlerCT handler) {
        addEventHandler(RecipeStartEvent.class, handler);
        return this;
    }

    @ZenMethod
    public DynamicMachineUpgradeBuilder addRecipePreTickHandler(UpgradeEventHandlerCT handler) {
        addEventHandler(RecipeTickEvent.class, (event, upgrade) -> {
            if (((RecipeTickEvent) event).phase == Phase.START) handler.handle(event, upgrade);
        });
        return this;
    }

    @ZenMethod
    public DynamicMachineUpgradeBuilder addRecipePostTickHandler(UpgradeEventHandlerCT handler) {
        addEventHandler(RecipeTickEvent.class, (event, upgrade) -> {
            if (((RecipeTickEvent) event).phase == Phase.END) handler.handle(event, upgrade);
        });
        return this;
    }

    @ZenMethod
    @Deprecated
    public DynamicMachineUpgradeBuilder addRecipeTickHandler(UpgradeEventHandlerCT handler) {
        CraftTweakerAPI.logWarning("[ModularMachinery] Deprecated method addTickHandler()! Consider using addPostTickHandler()");
        return addRecipePostTickHandler(handler);
    }

    @ZenMethod
    public DynamicMachineUpgradeBuilder addRecipeFailureHandler(UpgradeEventHandlerCT handler) {
        addEventHandler(RecipeFailureEvent.class, handler);
        return this;
    }

    @ZenMethod
    public DynamicMachineUpgradeBuilder addRecipeFinishHandler(UpgradeEventHandlerCT handler) {
        addEventHandler(RecipeFinishEvent.class, handler);
        return this;
    }

    @ZenMethod
    public DynamicMachineUpgradeBuilder addFactoryRecipeStartHandler(UpgradeEventHandlerCT handler) {
        addEventHandler(FactoryRecipeStartEvent.class, handler);
        return this;
    }

    @ZenMethod
    public DynamicMachineUpgradeBuilder addFactoryRecipePreTickHandler(UpgradeEventHandlerCT handler) {
        addEventHandler(FactoryRecipeTickEvent.class, (event, upgrade) -> {
            if (((FactoryRecipeTickEvent) event).phase == Phase.START) handler.handle(event, upgrade);
        });
        return this;
    }

    @ZenMethod
    public DynamicMachineUpgradeBuilder addFactoryRecipePostTickHandler(UpgradeEventHandlerCT handler) {
        addEventHandler(FactoryRecipeTickEvent.class, (event, upgrade) -> {
            if (((FactoryRecipeTickEvent) event).phase == Phase.END) handler.handle(event, upgrade);
        });
        return this;
    }

    @ZenMethod
    public DynamicMachineUpgradeBuilder addFactoryRecipeFailureHandler(UpgradeEventHandlerCT handler) {
        addEventHandler(FactoryRecipeFailureEvent.class, handler);
        return this;
    }

    @ZenMethod
    public DynamicMachineUpgradeBuilder addFactoryRecipeFinishHandler(UpgradeEventHandlerCT handler) {
        addEventHandler(FactoryRecipeFinishEvent.class, handler);
        return this;
    }

    @ZenMethod
    public DynamicMachineUpgradeBuilder addMachinePreTickHandler(UpgradeEventHandlerCT handler) {
        addEventHandler(MachineTickEvent.class, (event, upgrade) -> {
            if (((MachineTickEvent) event).phase == Phase.START) handler.handle(event, upgrade);
        });
        return this;
    }

    @ZenMethod
    public DynamicMachineUpgradeBuilder addMachinePostTickHandler(UpgradeEventHandlerCT handler) {
        addEventHandler(MachineTickEvent.class, (event, upgrade) -> {
            if (((MachineTickEvent) event).phase == Phase.END) handler.handle(event, upgrade);
        });
        return this;
    }

    @ZenMethod
    public DynamicMachineUpgradeBuilder addStructureFormedHandler(UpgradeEventHandlerCT handler) {
        addEventHandler(MachineStructureFormedEvent.class, handler);
        return this;
    }

    @ZenMethod
    public DynamicMachineUpgradeBuilder addControllerGUIRenderHandler(UpgradeEventHandlerCT handler) {
        addEventHandler(ControllerGUIRenderEvent.class, handler);
        return this;
    }

    @ZenMethod
    public DynamicMachineUpgradeBuilder addSmartInterfaceUpdateHandler(UpgradeEventHandlerCT handler) {
        addEventHandler(SmartInterfaceUpdateEvent.class, handler);
        return this;
    }

    @ZenMethod
    public void buildAndRegister() {
        MachineUpgrade.registerUpgrade(machineUpgrade.getType().getName(), machineUpgrade);
    }

    private <E extends MachineEvent> void addEventHandler(Class<E> eventClass, UpgradeEventHandlerCT handler) {
        machineUpgrade.addEventHandler(eventClass, handler);
    }
}
