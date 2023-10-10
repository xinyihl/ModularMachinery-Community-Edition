package kport.modularmagic.common.tile.machinecomponent;

import hellfirepvp.modularmachinery.common.crafting.ComponentType;
import hellfirepvp.modularmachinery.common.lib.RegistriesMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import kport.modularmagic.common.crafting.component.ModularMagicComponents;
import kport.modularmagic.common.tile.TileAspectProvider;

public class MachineComponentAspectProvider extends MachineComponent<TileAspectProvider> {

    private TileAspectProvider aspectProvider;

    public MachineComponentAspectProvider(TileAspectProvider aspectProvider, IOType ioType) {
        super(ioType);
        this.aspectProvider = aspectProvider;
    }

    @Override
    public ComponentType getComponentType() {
        return RegistriesMM.COMPONENT_TYPE_REGISTRY.getValue(ModularMagicComponents.KEY_COMPONENT_ASPECT);
    }

    @Override
    public TileAspectProvider getContainerProvider() {
        return aspectProvider;
    }
}
