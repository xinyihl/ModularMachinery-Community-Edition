package github.kasuminova.mmce.common.integration.gregtech.componentproxy;

import github.kasuminova.mmce.common.machine.component.MachineComponentProxy;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.common.metatileentities.multi.multiblockpart.MetaTileEntityFluidHatch;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.IFluidHandler;

public class GTFluidHatchProxy implements MachineComponentProxy<GTFluidHatchProxy.GTFluidHatchMachineComponent> {
    public static final GTFluidHatchProxy INSTANCE = new GTFluidHatchProxy();

    private GTFluidHatchProxy() {
    }

    @Override
    public boolean isSupported(final TileEntity te) {
        if (te instanceof MetaTileEntityHolder metaTE) {
            return metaTE.getMetaTileEntity() instanceof MetaTileEntityFluidHatch;
        }
        return false;
    }

    @Override
    public GTFluidHatchMachineComponent proxyComponent(final TileEntity te) {
        if (!(te instanceof MetaTileEntityHolder metaTE)) {
            return null;
        }
        if (!(metaTE.getMetaTileEntity() instanceof MetaTileEntityFluidHatch fluidHatch)) {
            return null;
        }

        MultiblockAbility<IFluidTank> ability = fluidHatch.getAbility();
        if (ability == MultiblockAbility.IMPORT_FLUIDS) {
            return new GTFluidHatchMachineComponent(IOType.INPUT, fluidHatch.getImportFluids());
        }
        if (ability == MultiblockAbility.EXPORT_FLUIDS) {
            return new GTFluidHatchMachineComponent(IOType.OUTPUT, fluidHatch.getExportFluids());
        }

        return null;
    }

    public static class GTFluidHatchMachineComponent extends MachineComponent.FluidHatch {
        private final IFluidHandler fluidHandler;

        public GTFluidHatchMachineComponent(final IOType ioType, IFluidHandler fluidHandler) {
            super(ioType);
            this.fluidHandler = fluidHandler;
        }

        @Override
        public boolean isAsyncSupported() {
            return false;
        }

        @Override
        public IFluidHandler getContainerProvider() {
            return fluidHandler;
        }
    }
}
