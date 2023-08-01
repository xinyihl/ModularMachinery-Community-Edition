package github.kasuminova.mmce.common.tile;

import appeng.api.networking.IGridNode;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEItemStack;
import appeng.me.GridAccessException;
import appeng.util.Platform;
import github.kasuminova.mmce.common.tile.base.MEItemBus;
import hellfirepvp.modularmachinery.common.lib.ItemsMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.util.IOInventory;
import hellfirepvp.modularmachinery.common.util.ItemUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class MEItemInputBus extends MEItemBus {
    private IOInventory configInventory = buildConfigInventory();

    @Override
    public IOInventory buildInventory() {
        int size = 16;

        int[] slotIDs = new int[size];
        for (int slotID = 0; slotID < size; slotID++) {
            slotIDs[slotID] = slotID;
        }
        return new IOInventory(this, slotIDs, new int[]{});
    }

    @Override
    public ItemStack getVisualItemStack() {
        return new ItemStack(ItemsMM.meItemInputBus);
    }

    public IOInventory buildConfigInventory() {
        int size = 16;

        int[] slotIDs = new int[size];
        for (int slotID = 0; slotID < size; slotID++) {
            slotIDs[slotID] = slotID;
        }

        return new IOInventory(this, new int[]{}, new int[]{}).setMiscSlots(slotIDs);
    }

    @Override
    public void readCustomNBT(final NBTTagCompound compound) {
        super.readCustomNBT(compound);

        if (compound.hasKey("configInventory")) {
            configInventory = IOInventory.deserialize(this, compound.getCompoundTag("configInventory"));
        }
    }

    @Override
    public void writeCustomNBT(final NBTTagCompound compound) {
        super.writeCustomNBT(compound);

        compound.setTag("configInventory", configInventory.writeNBT());
    }

    public IOInventory getConfigInventory() {
        return configInventory;
    }

    @Nullable
    @Override
    public MachineComponent<IOInventory> provideComponent() {
        return new MachineComponent.ItemBus(IOType.INPUT) {
            @Override
            public IOInventory getContainerProvider() {
                return inventory;
            }
        };
    }

    @Nonnull
    @Override
    public TickingRequest getTickingRequest(@Nonnull final IGridNode node) {
        return new TickingRequest(10, 120, !needsUpdate(), true);
    }

    private boolean needsUpdate() {
        for (int slot = 0; slot < configInventory.getSlots(); slot++) {
            ItemStack cfgStack = configInventory.getStackInSlot(slot);
            ItemStack invStack = inventory.getStackInSlot(slot);

            if (cfgStack == ItemStack.EMPTY) {
                if (invStack != ItemStack.EMPTY) {
                    return true;
                }
                continue;
            }

            if (invStack == ItemStack.EMPTY) {
                return true;
            }

            if (!ItemUtils.matchStacks(cfgStack, invStack) || invStack.getCount() != cfgStack.getCount()) {
                return true;
            }
        }
        return false;
    }

    @Nonnull
    @Override
    public TickRateModulation tickingRequest(@Nonnull final IGridNode node, final int ticksSinceLastCall) {
        if (!proxy.isActive()) {
            return TickRateModulation.IDLE;
        }

        try {
            boolean successAtLeastOnce = false;

            IMEMonitor<IAEItemStack> inv = proxy.getStorage().getInventory(channel);

            for (int slot = 0; slot < configInventory.getSlots(); slot++) {
                ItemStack cfgStack = configInventory.getStackInSlot(slot);
                ItemStack invStack = inventory.getStackInSlot(slot);

                if (cfgStack == ItemStack.EMPTY) {
                    if (invStack == ItemStack.EMPTY) {
                        continue;
                    }
                    inventory.setStackInSlot(slot, insertStackToAE(inv, invStack));
                    successAtLeastOnce = true;
                    continue;
                }

                if (!ItemUtils.matchStacks(cfgStack, invStack)) {
                    if (insertStackToAE(inv, invStack) == ItemStack.EMPTY) {
                        ItemStack stack = extractStackFromAE(inv, cfgStack);
                        inventory.setStackInSlot(slot, stack);
                        if (stack != ItemStack.EMPTY) {
                            successAtLeastOnce = true;
                        }
                    }
                    continue;
                }

                if (cfgStack.getCount() == invStack.getCount()) {
                    continue;
                }

                if (cfgStack.getCount() > invStack.getCount()) {
                    int countToReceive = cfgStack.getCount() - invStack.getCount();
                    ItemStack stack = extractStackFromAE(inv, ItemUtils.copyStackWithSize(invStack, countToReceive));
                    if (stack != ItemStack.EMPTY) {
                        inventory.setStackInSlot(slot, ItemUtils.copyStackWithSize(
                                invStack, invStack.getCount() + stack.getCount())
                        );
                        successAtLeastOnce = true;
                    }
                } else {
                    int countToExtract = invStack.getCount() - cfgStack.getCount();
                    ItemStack stack = insertStackToAE(inv, ItemUtils.copyStackWithSize(invStack, countToExtract));
                    if (stack == ItemStack.EMPTY) {
                        inventory.setStackInSlot(slot, ItemUtils.copyStackWithSize(
                                invStack, invStack.getCount() - countToExtract)
                        );
                    } else {
                        inventory.setStackInSlot(slot, ItemUtils.copyStackWithSize(
                                invStack, invStack.getCount() - countToExtract + stack.getCount())
                        );
                    }
                    successAtLeastOnce = true;
                }
            }

            return successAtLeastOnce ? TickRateModulation.FASTER : TickRateModulation.SLOWER;
        } catch (GridAccessException e) {
            return TickRateModulation.IDLE;
        }
    }

    private ItemStack extractStackFromAE(final IMEMonitor<IAEItemStack> inv, final ItemStack invStack) throws GridAccessException {
        IAEItemStack aeStack = channel.createStack(invStack);
        if (aeStack == null) {
            return invStack;
        }

        IAEItemStack extracted = Platform.poweredExtraction(proxy.getEnergy(), inv, aeStack, source);
        if (extracted == null) {
            return ItemStack.EMPTY;
        }
        return extracted.createItemStack();
    }

    private ItemStack insertStackToAE(final IMEMonitor<IAEItemStack> inv, final ItemStack invStack) throws GridAccessException {
        IAEItemStack aeStack = channel.createStack(invStack);
        if (aeStack == null) {
            return invStack;
        }

        IAEItemStack left = Platform.poweredInsert(proxy.getEnergy(), inv, aeStack, source);
        if (left == null) {
            return ItemStack.EMPTY;
        }
        return left.createItemStack();
    }

    @Override
    public void markForUpdate() {
        if (proxy.isActive() && needsUpdate()) {
            try {
                proxy.getTick().alertDevice(proxy.getNode());
            } catch (GridAccessException e) {
                // NO-OP
            }
        }

        super.markForUpdate();
    }
}
