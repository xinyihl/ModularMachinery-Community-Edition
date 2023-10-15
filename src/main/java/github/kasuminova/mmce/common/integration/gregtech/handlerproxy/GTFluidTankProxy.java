package github.kasuminova.mmce.common.integration.gregtech.handlerproxy;

import com.github.bsideup.jabel.Desugar;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

import javax.annotation.Nullable;

public class GTFluidTankProxy implements IFluidHandler {
    private final IFluidTank fluidTank;

    public GTFluidTankProxy(final IFluidTank fluidTank) {
        this.fluidTank = fluidTank;
    }

    @Override
    public IFluidTankProperties[] getTankProperties() {
        return new IFluidTankProperties[]{new GTFluidTankProp(fluidTank)};
    }

    @Override
    public int fill(final FluidStack resource, final boolean doFill) {
        return fluidTank.fill(resource, doFill);
    }

    @Nullable
    @Override
    public FluidStack drain(final FluidStack resource, final boolean doDrain) {
        FluidStack content = fluidTank.getFluid();
        if (content == null || !content.isFluidEqual(resource)) {
            return null;
        }
        return fluidTank.drain(resource.amount, doDrain);
    }

    @Nullable
    @Override
    public FluidStack drain(final int maxDrain, final boolean doDrain) {
        return fluidTank.drain(maxDrain, doDrain);
    }

    @Desugar
    private record GTFluidTankProp(IFluidTank fluidTank) implements IFluidTankProperties {

        @Nullable
        @Override
        public FluidStack getContents() {
            return fluidTank.getFluid();
        }

        @Override
        public int getCapacity() {
            return fluidTank.getCapacity();
        }

        @Override
        public boolean canFill() {
            return true;
        }

        @Override
        public boolean canDrain() {
            return true;
        }

        @Override
        public boolean canFillFluidType(final FluidStack fluidStack) {
            return true;
        }

        @Override
        public boolean canDrainFluidType(final FluidStack fluidStack) {
            return true;
        }
    }
}
