package kport.modularmagic.common.tile.machinecomponent;

import hellfirepvp.modularmachinery.common.crafting.ComponentType;
import hellfirepvp.modularmachinery.common.lib.RegistriesMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import kport.modularmagic.common.crafting.component.ModularMagicComponents;
import kport.modularmagic.common.crafting.helper.LifeEssenceProviderCopy;
import kport.modularmagic.common.tile.TileLifeEssenceProvider;

public class MachineComponentLifeEssenceProvider extends MachineComponent<LifeEssenceProviderCopy> {

    private final LifeEssenceProviderCopy lifeEssenceProvider;

    public MachineComponentLifeEssenceProvider(TileLifeEssenceProvider lifeEssenceProvider, IOType ioType) {
        super(ioType);
        this.lifeEssenceProvider = new LifeEssenceProviderCopy(lifeEssenceProvider);
    }

    @Override
    public ComponentType getComponentType() {
        return RegistriesMM.COMPONENT_TYPE_REGISTRY.getValue(ModularMagicComponents.KEY_COMPONENT_LIFE_ESSENCE);
    }

    @Override
    public LifeEssenceProviderCopy getContainerProvider() {
        return this.lifeEssenceProvider;
    }
}
