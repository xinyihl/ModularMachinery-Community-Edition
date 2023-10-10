package kport.modularmagic.common.tile.machinecomponent;

import hellfirepvp.modularmachinery.common.crafting.ComponentType;
import hellfirepvp.modularmachinery.common.lib.RegistriesMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import kport.modularmagic.common.crafting.component.ModularMagicComponents;
import kport.modularmagic.common.tile.TileGridProvider;

public class MachineComponentGridProvider extends MachineComponent<TileGridProvider> {

    private TileGridProvider gridProvider;

    public MachineComponentGridProvider(TileGridProvider gridProvider, IOType ioType) {
        super(ioType);

        this.gridProvider = gridProvider;
    }

    @Override
    public ComponentType getComponentType() {
        return RegistriesMM.COMPONENT_TYPE_REGISTRY.getValue(ModularMagicComponents.KEY_COMPONENT_GRID);
    }

    @Override
    public TileGridProvider getContainerProvider() {
        return this.gridProvider;
    }
}
