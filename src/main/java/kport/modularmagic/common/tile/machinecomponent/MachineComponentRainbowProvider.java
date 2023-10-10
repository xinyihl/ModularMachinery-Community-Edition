package kport.modularmagic.common.tile.machinecomponent;

import hellfirepvp.modularmachinery.common.crafting.ComponentType;
import hellfirepvp.modularmachinery.common.lib.RegistriesMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import kport.modularmagic.common.crafting.component.ModularMagicComponents;
import kport.modularmagic.common.tile.TileRainbowProvider;

public class MachineComponentRainbowProvider extends MachineComponent<TileRainbowProvider> {

    private TileRainbowProvider rainbowProvider;

    public MachineComponentRainbowProvider(TileRainbowProvider rainbowProvider, IOType ioType) {
        super(ioType);
        this.rainbowProvider = rainbowProvider;
    }

    @Override
    public ComponentType getComponentType() {
        return RegistriesMM.COMPONENT_TYPE_REGISTRY.getValue(ModularMagicComponents.KEY_COMPONENT_RAINBOW);
    }

    @Override
    public TileRainbowProvider getContainerProvider() {
        return this.rainbowProvider;
    }
}
