package kport.modularmagic.common.tile.machinecomponent;

import hellfirepvp.modularmachinery.common.crafting.ComponentType;
import hellfirepvp.modularmachinery.common.lib.RegistriesMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import kport.gugu_utils.GuGuCompoments;
import kport.modularmagic.common.tile.TileStarlightOutput;
import net.minecraft.util.ResourceLocation;

public class MachineComponentStarlightProviderOutput extends MachineComponent<TileStarlightOutput> {

    private final TileStarlightOutput starlightProvider;

    public MachineComponentStarlightProviderOutput(TileStarlightOutput starlightProvider, IOType ioType) {
        super(ioType);
        this.starlightProvider = starlightProvider;
    }

    @Override
    public ComponentType getComponentType() {
        return RegistriesMM.COMPONENT_TYPE_REGISTRY.getValue((ResourceLocation) GuGuCompoments.COMPONENT_STARLIGHT);
    }

    @Override
    public TileStarlightOutput getContainerProvider() {
        return starlightProvider;
    }
}
