package hellfirepvp.modularmachinery.common.util;

import mekanism.api.gas.GasStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

public class HybridFluidUtils {
    public static int maxGasInputParallelism(HybridGasTank handler, GasStack gas, int parallelism) {
        GasStack internal = handler.getGas();
        if (internal == null) {
            return 0;
        }
        if (!internal.isGasEqual(gas)) {
            return 0;
        }
        if (internal.amount < gas.amount || gas.amount < 0) {
            return 0;
        }
        return Math.min(internal.amount / gas.amount, parallelism);
    }

    public static int maxGasOutputParallelism(HybridGasTank handler, GasStack gas, int parallelism) {
        GasStack internal = handler.getGas();
        int internalAmount = internal == null ? 0 : internal.amount;
        if (internal != null && !internal.isGasEqual(gas)) {
            return 0;
        }
        if (handler.getCapacity() < gas.amount || internalAmount < gas.amount || gas.amount < 0) {
            return 0;
        }
        int remaining = handler.getCapacity() - internalAmount;
        return Math.min(remaining / gas.amount, parallelism);
    }

    public static int maxFluidInputParallelism(IFluidHandler handler, FluidStack fluid, int parallelism) {
        if (fluid == null || fluid.amount <= 0) {
            return 0;
        }
        int baseDrain = fluid.amount;
        int maxParallelism = Math.min(Integer.MAX_VALUE / fluid.amount, parallelism);

        fluid.amount *= maxParallelism;

        FluidStack drained = handler.drain(fluid, false);
        if (drained == null) {
            return 0;
        }

        return drained.amount / baseDrain;
    }

    public static int maxFluidOutputParallelism(IFluidHandler handler, FluidStack fluid, int parallelism) {
        if (fluid == null) {
            return 0;
        }

        if (fluid.amount <= 0) {
            return parallelism;
        }

        int baseFill = fluid.amount;
        int maxParallelism = Math.min(Integer.MAX_VALUE / fluid.amount, parallelism);

        fluid.amount *= maxParallelism;
        int filled = handler.fill(fluid, false);

        return filled / baseFill;
    }
}
