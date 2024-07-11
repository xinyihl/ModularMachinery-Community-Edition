package github.kasuminova.mmce.common.util;

import com.mekeng.github.common.me.inventory.IGasInventoryHost;
import com.mekeng.github.common.me.inventory.impl.GasInventory;
import mekanism.api.gas.Gas;
import mekanism.api.gas.GasStack;
import mekanism.api.gas.GasTank;
import mekanism.api.gas.GasTankInfo;
import net.minecraft.util.EnumFacing;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class GasInventoryHandler extends GasInventory implements IExtendedGasHandler {

    public GasInventoryHandler(final int size, final int cap, @Nullable final IGasInventoryHost host) {
        super(size, cap, host);
    }

    public GasInventoryHandler(final int size, @Nullable final IGasInventoryHost host) {
        super(size, host);
    }

    public GasInventoryHandler(final int size, final int cap) {
        super(size, cap);
    }

    public GasInventoryHandler(final int size) {
        super(size);
    }

    @Override
    public GasStack drawGas(final GasStack toDraw, final boolean doTransfer) {
        if (toDraw == null || toDraw.amount <= 0) {
            return null;
        }

        GasStack ret = null;
        for (final GasTank tank : getTanks()) {
            if (!tank.canDraw(toDraw.getGas())) {
                continue;
            }

            if (ret == null) {
                ret = tank.draw(toDraw.amount, doTransfer);
                if (ret.amount >= toDraw.amount) {
                    return ret;
                }
                continue;
            }

            GasStack drawn = tank.draw(toDraw.amount - ret.amount, doTransfer);
            if (drawn != null) {
                ret.amount += drawn.amount;
                if (ret.amount >= toDraw.amount) {
                    return ret;
                }
            }
        }

        return ret;
    }

    @Override
    public int receiveGas(final EnumFacing side, final GasStack stack, final boolean doTransfer) {
        if (stack.amount <= 0) {
            return 0;
        }

        int received = 0;
        for (final GasTank tank : getTanks()) {
            if (tank.canReceive(stack.getGas())) {
                received += tank.receive(stack, doTransfer);
                if (stack.amount <= received) {
                    return received;
                }
            }
        }

        return received;
    }

    @Override
    public GasStack drawGas(final EnumFacing side, final int amount, final boolean doTransfer) {
        if (amount <= 0) {
            return null;
        }

        GasStack ret = null;
        for (final GasTank tank : getTanks()) {
            if (ret == null) {
                ret = tank.draw(amount, doTransfer);
                continue;
            }
            if (!tank.canDraw(ret.getGas())) {
                continue;
            }
            if (ret.amount < Integer.MAX_VALUE) {
                GasStack drawn = tank.draw(Math.min(amount - ret.amount, Integer.MAX_VALUE - ret.amount), doTransfer);
                if (drawn != null) {
                    ret.amount += drawn.amount;
                }
            }
        }

        return ret;
    }

    @Override
    public boolean canReceiveGas(final EnumFacing side, final Gas type) {
        for (final GasTank tank : getTanks()) {
            if (tank.canReceive(type)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canDrawGas(final EnumFacing side, final Gas type) {
        for (final GasTank tank : getTanks()) {
            if (tank.canDraw(type)) {
                return true;
            }
        }
        return false;
    }

    @Nonnull
    @Override
    public GasTankInfo[] getTankInfo() {
        return getTanks();
    }
}
