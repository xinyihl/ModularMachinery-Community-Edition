package kport.modularmagic.common.tile.machinecomponent;

import hellfirepvp.modularmachinery.common.crafting.ComponentType;
import hellfirepvp.modularmachinery.common.lib.RegistriesMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import kport.modularmagic.common.crafting.component.ModularMagicComponents;
import kport.modularmagic.common.crafting.helper.ManaProviderCopy;
import kport.modularmagic.common.tile.TileManaProvider;

public class MachineComponentManaProvider extends MachineComponent<ManaProviderCopy> {

    private final ManaProviderCopy manaProvider;

    public MachineComponentManaProvider(IOType io, TileManaProvider manaProvider) {
        super(io);
        this.manaProvider = new ManaProviderCopy(manaProvider);
    }

    @Override
    public ComponentType getComponentType() {
        return RegistriesMM.COMPONENT_TYPE_REGISTRY.getValue(ModularMagicComponents.KEY_COMPONENT_MANA);
    }

    @Override
    public ManaProviderCopy getContainerProvider() {
        return manaProvider;
    }

}
