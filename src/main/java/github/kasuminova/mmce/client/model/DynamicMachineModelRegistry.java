package github.kasuminova.mmce.client.model;

import hellfirepvp.modularmachinery.common.machine.DynamicMachine;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class DynamicMachineModelRegistry {
    public static final DynamicMachineModelRegistry INSTANCE = new DynamicMachineModelRegistry();

    private DynamicMachineModelRegistry() {
    }

    private final Map<DynamicMachine, MachineControllerModel> machineModelRegistry = new HashMap<>();

    public void registerGeoModel(DynamicMachine machine, MachineControllerModel model) {
        machineModelRegistry.put(machine, model);
    }

    public MachineControllerModel getModel(DynamicMachine machine) {
        return machineModelRegistry.get(machine);
    }

    public Collection<MachineControllerModel> getAllModels() {
        return machineModelRegistry.values();
    }

    public void onReload() {
        machineModelRegistry.clear();
    }
}
