package hellfirepvp.modularmachinery.common.tiles;

import hellfirepvp.modularmachinery.common.block.prop.CatalystBusSize;
import hellfirepvp.modularmachinery.common.block.prop.ItemBusSize;
import hellfirepvp.modularmachinery.common.tiles.base.TileInventory;
import hellfirepvp.modularmachinery.common.util.IOInventory;
import hellfirepvp.modularmachinery.common.util.IOInventoryRedstone;
import net.minecraft.util.EnumFacing;

public class TileCatalystBus extends TileItemInputBus {

    public TileCatalystBus() {
    }

    public TileCatalystBus(CatalystBusSize type) {
        super(ItemBusSize.LUDICROUS);
    }

    @Override
    public IOInventory buildInventory(TileInventory tile, int size) {
        int[] slots = new int[size];
        for (int i = 0; i < size; i++) {
            slots[i] = i;
        }
        return new IOInventoryRedstone(tile, slots, new int[]{}, new EnumFacing[0]);
    }

}
