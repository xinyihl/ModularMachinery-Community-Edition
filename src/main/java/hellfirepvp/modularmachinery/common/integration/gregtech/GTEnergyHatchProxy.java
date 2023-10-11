package hellfirepvp.modularmachinery.common.integration.gregtech;

import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.common.metatileentities.multi.multiblockpart.MetaTileEntityEnergyHatch;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.machine.component.MachineComponentProxy;
import net.minecraft.tileentity.TileEntity;

import javax.annotation.Nullable;

public class GTEnergyHatchProxy implements MachineComponentProxy {

    @Override
    public boolean isSupported(final TileEntity te) {
        if (te instanceof MetaTileEntityHolder metaTE) {
            return metaTE.getMetaTileEntity() instanceof MetaTileEntityEnergyHatch;
        }
        return false;
    }

    @Nullable
    @Override
    public MachineComponent<?> proxy(final TileEntity te) {
        return null;
    }

}
