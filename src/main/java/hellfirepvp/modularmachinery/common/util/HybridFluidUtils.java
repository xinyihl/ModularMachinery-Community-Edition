package hellfirepvp.modularmachinery.common.util;

import github.kasuminova.mmce.common.util.MultiFluidTank;
import hellfirepvp.modularmachinery.common.crafting.helper.ProcessingComponent;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import mekanism.api.gas.GasStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fml.common.Optional;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class HybridFluidUtils {
    public static long doSimulateDrainOrFill(final FluidStack drainOrFill, final List<IFluidHandler> fluidHandlers, final long maxDrainOrFill, final IOType actionType) {
        long totalIO = 0;

        FluidStack stack = drainOrFill.copy();
        for (final IFluidHandler handler : fluidHandlers) {
            stack.amount = maxDrainOrFill - totalIO >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) (maxDrainOrFill - totalIO);

            switch (actionType) {
                case INPUT -> {
                    FluidStack drained = handler.drain(stack, false);
                    if (drained != null) {
                        totalIO += drained.amount;
                    }
                }
                case OUTPUT -> totalIO += handler.fill(stack, false);
            }

            if (totalIO >= maxDrainOrFill) {
                break;
            }
        }

        return totalIO;
    }

    public static void doDrainOrFill(final FluidStack drainOrFill, long maxDrainOrFill, final List<IFluidHandler> fluidHandlers, final IOType actionType) {
        long totalIO = maxDrainOrFill;

        FluidStack stack = drainOrFill.copy();
        for (final IFluidHandler handler : fluidHandlers) {
            stack.amount = totalIO >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) totalIO;

            switch (actionType) {
                case INPUT -> {
                    FluidStack drained = handler.drain(stack, true);
                    if (drained != null) {
                        totalIO -= drained.amount;
                    }
                }
                case OUTPUT -> totalIO -= handler.fill(stack, true);
            }

            if (totalIO <= 0) {
                break;
            }
        }
    }

    @Optional.Method(modid = "mekanism")
    public static long doSimulateDrainOrFill(final GasStack drainOrFill, final List<HybridGasTank> gasHandlers, final long maxDrainOrFill, final IOType actionType) {
        long totalIO = 0;

        GasStack stack = drainOrFill.copy();
        for (final HybridGasTank handler : gasHandlers) {
            stack.amount = maxDrainOrFill - totalIO >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) (maxDrainOrFill - totalIO);

            GasStack gas = handler.getGas();
            switch (actionType) {
                case INPUT -> {
                    if (!stack.isGasEqual(gas)) {
                        continue;
                    }
                    GasStack drained = handler.drawGas(EnumFacing.UP, stack.amount, false);
                    if (drained != null) {
                        totalIO += drained.amount;
                    }
                }
                case OUTPUT -> {
                    if (gas != null && !stack.isGasEqual(gas)) {
                        continue;
                    }
                    totalIO += handler.receiveGas(EnumFacing.UP, stack, false);
                }
            }

            if (totalIO >= maxDrainOrFill) {
                break;
            }
        }

        return totalIO;
    }

    @Optional.Method(modid = "mekanism")
    public static void doDrainOrFill(final GasStack drainOrFill, final long maxDrainOrFill, final List<HybridGasTank> gasHandlers, final IOType actionType) {
        long totalIO = maxDrainOrFill;

        GasStack stack = drainOrFill.copy();

        for (final HybridGasTank handler : gasHandlers) {
            stack.amount = totalIO >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) totalIO;

            GasStack gas = handler.getGas();

            switch (actionType) {
                case INPUT -> {
                    if (!stack.isGasEqual(gas)) {
                        continue;
                    }
                    GasStack drained = handler.drawGas(EnumFacing.UP, stack.amount, true);
                    if (drained != null) {
                        totalIO -= drained.amount;
                    }
                }
                case OUTPUT -> {
                    if (gas != null && !stack.isGasEqual(gas)) {
                        continue;
                    }
                    totalIO -= handler.receiveGas(EnumFacing.UP, stack, true);
                }
            }

            if (totalIO <= 0) {
                break;
            }
        }
    }

    @Nonnull
    public static List<IFluidHandler> castFluidHandlerComponents(final List<ProcessingComponent<?>> components) {
        List<IFluidHandler> fluidHandlers = new ArrayList<>();
        for (ProcessingComponent<?> component : components) {
            IFluidHandler providedComponent = (IFluidHandler) component.getProvidedComponent();
            fluidHandlers.add(providedComponent);
        }
        return fluidHandlers;
    }

    @Nonnull
    @Optional.Method(modid = "mekanism")
    public static List<HybridGasTank> castGasHandlerComponents(final List<ProcessingComponent<?>> components) {
        List<HybridGasTank> list = new ArrayList<>();
        for (ProcessingComponent<?> component : components) {
            Object providedComponent = component.getProvidedComponent();
            if (providedComponent instanceof final HybridGasTank hybridGasTank) {
                list.add(hybridGasTank);
            }
        }
        return list;
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    public static List<ProcessingComponent<?>> copyFluidHandlerComponents(final List<ProcessingComponent<?>> components) {
        List<ProcessingComponent<?>> list = new ArrayList<>();
        for (ProcessingComponent<?> component : components) {
            ProcessingComponent<Object> objectProcessingComponent = new ProcessingComponent<>(
                    (MachineComponent<Object>) component.component(),
                    new MultiFluidTank((IFluidHandler) component.getProvidedComponent()),
                    component.getTag());
            list.add(objectProcessingComponent);
        }
        return list;
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    @Optional.Method(modid = "mekanism")
    public static List<ProcessingComponent<?>> copyGasHandlerComponents(final List<ProcessingComponent<?>> components) {
        List<ProcessingComponent<?>> list = new ArrayList<>();
        for (ProcessingComponent<?> component : components) {
            if (!(component.getProvidedComponent() instanceof HybridGasTank)) {
                continue;
            }
            ProcessingComponent<Object> objectProcessingComponent = new ProcessingComponent<>(
                    (MachineComponent<Object>) component.component(),
                    CopyHandlerHelper.copyGasTank((HybridGasTank) component.getProvidedComponent()),
                    component.getTag());
            list.add(objectProcessingComponent);
        }
        return list;
    }
}
