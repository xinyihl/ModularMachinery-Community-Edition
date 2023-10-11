package kport.modularmagic.common.tile.machinecomponent;

import hellfirepvp.modularmachinery.common.crafting.ComponentType;
import hellfirepvp.modularmachinery.common.lib.RegistriesMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import kport.modularmagic.common.crafting.component.ModularMagicComponents;
import kport.modularmagic.common.tile.TileAuraProvider;

public class MachineComponentAuraProvider extends MachineComponent<TileAuraProvider> {

    private final TileAuraProvider provider;

    public MachineComponentAuraProvider(TileAuraProvider provider, IOType ioType) {
        super(ioType);
        this.provider = provider;
    }

    @Override
    public ComponentType getComponentType() {
        return RegistriesMM.COMPONENT_TYPE_REGISTRY.getValue(ModularMagicComponents.KEY_COMPONENT_AURA);
    }

    @Override
    public TileAuraProvider getContainerProvider() {
        return provider;
    }
}
