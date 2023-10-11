package kport.modularmagic.common.tile.machinecomponent;

import hellfirepvp.modularmachinery.common.crafting.ComponentType;
import hellfirepvp.modularmachinery.common.lib.RegistriesMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import kport.modularmagic.common.crafting.component.ModularMagicComponents;
import kport.modularmagic.common.tile.TileConstellationProvider;

public class MachineComponentConstellationProvider extends MachineComponent<TileConstellationProvider> {

    private final TileConstellationProvider provider;

    public MachineComponentConstellationProvider(TileConstellationProvider provider, IOType ioType) {
        super(ioType);
        this.provider = provider;
    }

    @Override
    public ComponentType getComponentType() {
        return RegistriesMM.COMPONENT_TYPE_REGISTRY.getValue(ModularMagicComponents.KEY_COMPONENT_CONSTELLATION);
    }

    @Override
    public TileConstellationProvider getContainerProvider() {
        return this.provider;
    }
}
