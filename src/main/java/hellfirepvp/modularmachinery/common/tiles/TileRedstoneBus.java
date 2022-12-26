package hellfirepvp.modularmachinery.common.tiles;

import hellfirepvp.modularmachinery.common.block.prop.ItemBusSize;
import hellfirepvp.modularmachinery.common.block.prop.RedstoneBusSize;
import hellfirepvp.modularmachinery.common.lib.ItemsMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.tiles.base.TileInventory;
import hellfirepvp.modularmachinery.common.util.IOInventory;
import hellfirepvp.modularmachinery.common.util.IOInventoryRedstone;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;

import javax.annotation.Nullable;

public class TileRedstoneBus extends TileItemInputBus {

    public TileRedstoneBus() {
    }

    public TileRedstoneBus(RedstoneBusSize type) {
        super(ItemBusSize.TINY);
    }

    @Override
    public IOInventory buildInventory(TileInventory tile, int size) {
        int[] slots = new int[size];
        for (int i = 0; i < size; i++) {
            slots[i] = i;
        }
        return new IOInventoryRedstone(tile, slots, new int[]{}, new EnumFacing[0]);
    }

    @Nullable
    @Override
    public MachineComponent provideComponent() {
        this.inventory.setStackInSlot(0, new ItemStack(ItemsMM.redstoneSignal, 1, world.getStrongPower(pos)));
        return new MachineComponent.ItemBus(IOType.INPUT) {
            @Override
            public IOInventory getContainerProvider() {
                return TileRedstoneBus.this.inventory;
            }
        };
    }
}
