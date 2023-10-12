package kport.modularmagic.common.tile.machinecomponent;

import hellfirepvp.modularmachinery.common.crafting.ComponentType;
import hellfirepvp.modularmachinery.common.lib.RegistriesMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import kport.modularmagic.common.crafting.component.ModularMagicComponents;
import kport.modularmagic.common.crafting.helper.ImpetusProviderCopy;
import kport.modularmagic.common.tile.TileImpetusComponent;

/**
 * @author youyihj
 */
public class MachineComponentImpetus extends MachineComponent<ImpetusProviderCopy> {
    private final ImpetusProviderCopy provider;

    public MachineComponentImpetus(IOType ioType, TileImpetusComponent provider) {
        super(ioType);
        this.provider = new ImpetusProviderCopy(provider);
    }

    @Override
    public ComponentType getComponentType() {
        return RegistriesMM.COMPONENT_TYPE_REGISTRY.getValue(ModularMagicComponents.KEY_COMPONENT_IMPETUS);
    }

    @Override
    public ImpetusProviderCopy getContainerProvider() {
        return provider;
    }
}
