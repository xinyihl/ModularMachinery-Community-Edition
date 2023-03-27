/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.tiles;

import hellfirepvp.modularmachinery.common.block.prop.ItemBusSize;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.tiles.base.MachineComponentTile;
import hellfirepvp.modularmachinery.common.tiles.base.TileInventory;
import hellfirepvp.modularmachinery.common.tiles.base.TileItemBus;
import hellfirepvp.modularmachinery.common.util.IOInventory;
import hellfirepvp.modularmachinery.common.util.ItemUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: TileItemOutputBus
 * Created by HellFirePvP
 * Date: 07.07.2017 / 18:41
 */
public class TileItemOutputBus extends TileItemBus implements MachineComponentTile {

    public TileItemOutputBus() {
    }

    @Override
    public void doRestrictedTick() {
        if (world.isRemote || ticksExisted % 20 != 0) {
            return;
        }

        for (EnumFacing facing : EnumFacing.VALUES) {
            BlockPos offset = getPos().offset(facing);
            TileEntity te = getWorld().getTileEntity(offset);
            if (te instanceof TileItemBus || !(te instanceof IItemHandler)) {
                continue;
            }

            EnumFacing accessingSide = facing.getOpposite();

            IItemHandler itemHandler = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, accessingSide);
            if (itemHandler == null) {
                continue;
            }

            outputInternalItemStack(itemHandler);
        }
    }

    private void outputInternalItemStack(IItemHandler itemHandler) {
        for (int itemHandlerSlotId = 0; itemHandlerSlotId < itemHandler.getSlots(); itemHandlerSlotId++) {
            ItemStack handlerStack = itemHandler.getStackInSlot(itemHandlerSlotId);
            if (handlerStack != ItemStack.EMPTY && handlerStack.getCount() == handlerStack.getMaxStackSize()) {
                continue;
            }

            for (int internalSlotId = 0; internalSlotId < inventory.getSlots(); internalSlotId++) {
                ItemStack internalStack = inventory.getStackInSlot(internalSlotId);
                if (internalStack == ItemStack.EMPTY) {
                    continue;
                }

                if (handlerStack == ItemStack.EMPTY) {
                    ItemStack notInserted = itemHandler.insertItem(itemHandlerSlotId, internalStack, false);
                    inventory.setStackInSlot(internalSlotId, notInserted);
                    if (notInserted == ItemStack.EMPTY) {
                        break;
                    }
                    continue;
                }

                if (ItemUtils.matchStacks(internalStack, handlerStack) && ItemUtils.matchTags(internalStack, handlerStack)) {
                    ItemStack notInserted = itemHandler.insertItem(itemHandlerSlotId, internalStack, false);
                    inventory.setStackInSlot(internalSlotId, notInserted);
                    if (notInserted == ItemStack.EMPTY) {
                        break;
                    }
                }
            }
        }
    }

    public TileItemOutputBus(ItemBusSize type) {
        super(type);
    }

    @Override
    public IOInventory buildInventory(TileInventory tile, int size) {
        int[] slots = new int[size];
        for (int i = 0; i < size; i++) {
            slots[i] = i;
        }
        return new IOInventory(tile, new int[]{}, slots);
    }

    @Nullable
    @Override
    public MachineComponent provideComponent() {
        return new MachineComponent.ItemBus(IOType.OUTPUT) {
            @Override
            public IOInventory getContainerProvider() {
                return TileItemOutputBus.this.inventory;
            }
        };
    }

}
