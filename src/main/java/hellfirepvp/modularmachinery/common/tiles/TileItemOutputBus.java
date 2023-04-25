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
    public static int minWorkDelay = 10;
    public static int maxWorkDelay = 60;

    public TileItemOutputBus() {
    }

    public TileItemOutputBus(ItemBusSize type) {
        super(type);
    }

    @Override
    public void doRestrictedTick() {
        if (getWorld().isRemote || !canWork(minWorkDelay, maxWorkDelay)) {
            return;
        }

        for (EnumFacing facing : EnumFacing.VALUES) {
            BlockPos offset = getPos().offset(facing);
            TileEntity te = getWorld().getTileEntity(offset);
            if (te == null || te instanceof TileItemBus) {
                continue;
            }

            EnumFacing accessingSide = facing.getOpposite();

            IItemHandler itemHandler = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, accessingSide);
            if (itemHandler == null) {
                continue;
            }

            outputToExternal(itemHandler);
        }
    }

    private void outputToExternal(IItemHandler external) {
        boolean successAtLeastOnce = false;

        for (int externalSlotId = 0; externalSlotId < external.getSlots(); externalSlotId++) {
            ItemStack externalStack = external.getStackInSlot(externalSlotId);
            if (externalStack != ItemStack.EMPTY && externalStack.getCount() >= external.getSlotLimit(externalSlotId)) {
                continue;
            }

            for (int internalSlotId = 0; internalSlotId < inventory.getSlots(); internalSlotId++) {
                ItemStack internalStack = inventory.getStackInSlot(internalSlotId);
                if (internalStack == ItemStack.EMPTY) {
                    continue;
                }

                if (externalStack == ItemStack.EMPTY) {
                    ItemStack notInserted = external.insertItem(externalSlotId, internalStack, false);
                    inventory.setStackInSlot(internalSlotId, notInserted);
                    successAtLeastOnce = true;
                    if (notInserted == ItemStack.EMPTY) {
                        break;
                    }
                    continue;
                }

                if (!ItemUtils.matchStacks(internalStack, externalStack)) {
                    continue;
                }

                // Extract internal item to external.
                ItemStack notInserted = external.insertItem(externalSlotId, internalStack, false);
                inventory.setStackInSlot(internalSlotId, notInserted);
                if (notInserted.getCount() == internalStack.getCount()) {
                    break;
                }

                successAtLeastOnce = true;
                if (notInserted == ItemStack.EMPTY) {
                    break;
                }
            }
        }

        if (successAtLeastOnce) {
            incrementSuccessCounter(maxWorkDelay, minWorkDelay);
            markForUpdate();
        } else {
            decrementSuccessCounter();
        }
    }

    @Override
    public void markForUpdate() {
        super.markForUpdate();
        inventoryChanged = true;
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
    public MachineComponent<IOInventory> provideComponent() {
        return new MachineComponent.ItemBus(IOType.OUTPUT) {
            @Override
            public IOInventory getContainerProvider() {
                return TileItemOutputBus.this.inventory;
            }
        };
    }

}
