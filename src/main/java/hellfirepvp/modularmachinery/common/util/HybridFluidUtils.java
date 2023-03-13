package hellfirepvp.modularmachinery.common.util;

import mekanism.api.gas.GasStack;
import net.minecraftforge.fluids.FluidStack;

public class HybridFluidUtils {
    public static int maxGasInputParallelism(HybridGasTank handler, GasStack gas, int parallelism) {
        GasStack internal = handler.getGas();
        if (internal == null) {
            return 0;
        }
        if (!internal.isGasEqual(gas)) {
            return 0;
        }
        if (internal.amount < gas.amount) {
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
        if (handler.getCapacity() < gas.amount || internalAmount < gas.amount) {
            return 0;
        }
        int remaining = handler.getCapacity() - internalAmount;
        return Math.min(remaining / gas.amount, parallelism);
    }

    public static int maxFluidInputParallelism(HybridTank handler, FluidStack fluid, int parallelism) {
        FluidStack internal = handler.getFluid();
        if (internal == null) {
            return 0;
        }
        if (!internal.equals(fluid)) {
            return 0;
        }
        if (internal.amount < fluid.amount) {
            return 0;
        }
        return Math.min(internal.amount / fluid.amount, parallelism);
    }

    public static int maxFluidOutputParallelism(HybridTank handler, FluidStack fluid, int parallelism) {
        FluidStack internal = handler.getFluid();
        int internalAmount = internal == null ? 0 : internal.amount;
        if (internal != null && !internal.equals(fluid)) {
            return 0;
        }
        if (handler.getCapacity() < fluid.amount || internalAmount < fluid.amount) {
            return 0;
        }
        int remaining = handler.getCapacity() - internalAmount;
        return Math.min(remaining / fluid.amount, parallelism);
    }
}
