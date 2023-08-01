/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.util;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fml.common.Optional;

import javax.annotation.Nullable;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: HybridTank
 * Created by HellFirePvP
 * Date: 26.08.2017 / 18:57
 */
@Optional.Interface(iface = "mekanism.api.gas.IGasHandler", modid = "mekanism")
public class HybridTank extends FluidTank {

    public HybridTank(int capacity) {
        super(capacity);
    }

    @Nullable
    @Override
    public FluidStack getFluid() {
        return super.getFluid();
    }

    @Override
    public synchronized void setFluid(@Nullable FluidStack fluid) {
        super.setFluid(fluid);
    }

    @Override
    public int getFluidAmount() {
        return super.getFluidAmount();
    }

    @Override
    public synchronized int fill(FluidStack resource, boolean doFill) {
        return super.fill(resource, doFill);
    }

    @Override
    public synchronized int fillInternal(FluidStack resource, boolean doFill) {
        return super.fillInternal(resource, doFill);
    }

    @Override
    public synchronized FluidStack drain(FluidStack resource, boolean doDrain) {
        return super.drain(resource, doDrain);
    }

    @Override
    public synchronized FluidStack drain(int maxDrain, boolean doDrain) {
        return super.drain(maxDrain, doDrain);
    }

    @Nullable
    @Override
    public synchronized FluidStack drainInternal(FluidStack resource, boolean doDrain) {
        return super.drainInternal(resource, doDrain);
    }

    @Nullable
    @Override
    public synchronized FluidStack drainInternal(int maxDrain, boolean doDrain) {
        return super.drainInternal(maxDrain, doDrain);
    }
}
