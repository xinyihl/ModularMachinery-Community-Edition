package github.kasuminova.mmce.common.tile;

import appeng.api.networking.IGridNode;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEItemStack;
import appeng.me.GridAccessException;
import appeng.util.Platform;
import github.kasuminova.mmce.client.gui.GuiMEItemInputBus;
import github.kasuminova.mmce.common.tile.base.MEItemBus;
import github.kasuminova.mmce.common.util.Sides;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.lib.ItemsMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.util.IOInventory;
import hellfirepvp.modularmachinery.common.util.ItemUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;

public class MEItemInputBus extends MEItemBus implements SettingsTransfer {

    private static final String CONFIG_TAG_KEY = "configInventory";

    // A simple cache for AEItemStack.
    private static final Map<ItemStack, IAEItemStack> AE_STACK_CACHE = new WeakHashMap<>();

    private IOInventory configInventory = buildConfigInventory();

    private MERequestMode meRequestMode = MERequestMode.DEFAULT;

    private int thresholdValue = 256;

    @Override
    public IOInventory buildInventory() {
        int size = 30;
        int[] slotIDs = new int[size];
        for (int slotID = 0; slotID < size; slotID++) {
            slotIDs[slotID] = slotID;
        }
        IOInventory inv = new IOInventory(this, slotIDs, new int[]{});
        inv.setStackLimit(Integer.MAX_VALUE, slotIDs);
        inv.setListener(slot -> {
            synchronized (this) {
                changedSlots[slot] = true;
            }
        });
        return inv;
    }

    @Override
    public ItemStack getVisualItemStack() {
        return new ItemStack(ItemsMM.meItemInputBus);
    }

    public IOInventory buildConfigInventory() {
        int size = 30;

        int[] slotIDs = new int[size];
        for (int slotID = 0; slotID < size; slotID++) {
            slotIDs[slotID] = slotID;
        }
        IOInventory inv = new IOInventory(this, new int[]{}, new int[]{});
        inv.setStackLimit(Integer.MAX_VALUE, slotIDs);
        inv.setMiscSlots(slotIDs);
        inv.setListener(slot -> {
            synchronized (this) {
                changedSlots[slot] = true;
            }
        });
        return inv;
    }

    @Override
    public void readCustomNBT(final NBTTagCompound compound) {
        super.readCustomNBT(compound);

        if (compound.hasKey(CONFIG_TAG_KEY)) {
            readConfigInventoryNBT(compound.getCompoundTag(CONFIG_TAG_KEY));
        }

        if (compound.hasKey("meRequestMode")) {
            meRequestMode = MERequestMode.fromName(compound.getString("meRequestMode"));
        }

        if (compound.hasKey("thresholdValue")) {
            thresholdValue = (compound.getInteger("thresholdValue"));
        }

        if (Sides.isRunningOnClient()) {
            processClientGUIUpdate();
        }
    }

    @Override
    public void writeCustomNBT(final NBTTagCompound compound) {
        super.writeCustomNBT(compound);

        compound.setTag(CONFIG_TAG_KEY, configInventory.writeNBT());

        compound.setString("meRequestMode", meRequestMode.getName());

        compound.setInteger("thresholdValue", thresholdValue);
    }

    public IOInventory getConfigInventory() {
        return configInventory;
    }

    public void setMERequestMode(MERequestMode mode) {
        this.meRequestMode = mode;
    }

    public MERequestMode getMERequestMode() {
        return meRequestMode;
    }

    public int getThresholdValue() {
        return thresholdValue;
    }

    public void setThresholdValue(final int thresholdValue) {
        this.thresholdValue =  thresholdValue;
    }

    @Nullable
    @Override
    public MachineComponent.ItemBus provideComponent() {
        return new MachineComponent.ItemBus(IOType.INPUT) {
            @Override
            public IOInventory getContainerProvider() {
                return inventory;
            }

            @Override
            public boolean isAffectedBySeparateInput() {
                return true;
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

            if (cfgStack.isEmpty()) {
                if (!invStack.isEmpty()) {
                    return true;
                }
                continue;
            }

            if (invStack.isEmpty()) {
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

        int[] needUpdateSlots = getNeedUpdateSlots();
        if (needUpdateSlots.length == 0) {
            return TickRateModulation.SLOWER;
        }

        ReadWriteLock rwLock = inventory.getRWLock();

        try {
            rwLock.writeLock().lock();

            boolean successAtLeastOnce = false;
            inTick = true;
            IMEMonitor<IAEItemStack> inv = proxy.getStorage().getInventory(channel);
            for (final int slot : needUpdateSlots) {
                changedSlots[slot] = false;
                ItemStack cfgStack = configInventory.getStackInSlot(slot);
                ItemStack invStack = inventory.getStackInSlot(slot);

                int stackAmountInAE = getStackAmountFromAE(inv, cfgStack);

                if (meRequestMode == MEItemInputBus.MERequestMode.THRESHOLD) {
                    if (stackAmountInAE < thresholdValue) {
                        // skip request if below amount
                        continue;
                    }
                }

                if (cfgStack.isEmpty()) {
                    if (invStack.isEmpty()) {
                        continue;
                    }
                    inventory.setStackInSlot(slot, insertStackToAE(inv, invStack));
                    continue;
                }

                if (!ItemUtils.matchStacks(cfgStack, invStack)) {
                    if (invStack.isEmpty() || insertStackToAE(inv, invStack).isEmpty()) {

                        ItemStack stack = extractStackFromAE(inv, cfgStack);

                        inventory.setStackInSlot(slot, stack);

                        if (!stack.isEmpty()) {
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
                    if (!stack.isEmpty()) {
                        int newCount = invStack.getCount() + stack.getCount();
                        inventory.setStackInSlot(slot, ItemUtils.copyStackWithSize(invStack, newCount));
                        successAtLeastOnce = true;
                        failureCounter[slot] = 0;
                    } else {
                        // If AE doesn't have enough item?
                        failureCounter[slot]++;
                    }
                } else {
                    int countToExtract = invStack.getCount() - cfgStack.getCount();
                    ItemStack stack = insertStackToAE(inv, ItemUtils.copyStackWithSize(invStack, countToExtract));
                    if (stack.isEmpty()) {
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

            inTick = false;
            rwLock.writeLock().unlock();
            return successAtLeastOnce ? TickRateModulation.FASTER : TickRateModulation.SLOWER;
        } catch (GridAccessException e) {
            inTick = false;
            changedSlots = new boolean[changedSlots.length];
            rwLock.writeLock().unlock();
            return TickRateModulation.IDLE;
        }
    }


    private int getStackAmountFromAE(final IMEMonitor<IAEItemStack> inv, final ItemStack stack) {
        IAEItemStack aeStack = createStack(stack);
        if (aeStack == null) return 0;

        IAEItemStack found = inv.getStorageList().findPrecise(aeStack);
        return found != null ? (int) found.getStackSize() : 0;
    }

    private ItemStack extractStackFromAE(final IMEMonitor<IAEItemStack> inv, final ItemStack stack) throws GridAccessException {
        IAEItemStack aeStack = createStack(stack);
        if (aeStack == null) {
            return ItemStack.EMPTY;
        }

        IAEItemStack extracted = Platform.poweredExtraction(proxy.getEnergy(), inv, aeStack, source);
        if (extracted == null) {
            return ItemStack.EMPTY;
        }
        return extracted.createItemStack();
    }

    private ItemStack insertStackToAE(final IMEMonitor<IAEItemStack> inv, final ItemStack stack) throws GridAccessException {
        IAEItemStack aeStack = createStack(stack);
        if (aeStack == null) {
            return stack;
        }

        IAEItemStack left = Platform.poweredInsert(proxy.getEnergy(), inv, aeStack, source);
        if (left == null) {
            return ItemStack.EMPTY;
        }
        return left.createItemStack();
    }

    private IAEItemStack createStack(final ItemStack stack) {
        return AE_STACK_CACHE.computeIfAbsent(stack, v -> channel.createStack(stack));
    }

    @Override
    public void markNoUpdate() {
        if (proxy.isActive() && hasChangedSlots()) {
            try {
                proxy.getTick().alertDevice(proxy.getNode());
            } catch (GridAccessException e) {
                // NO-OP
            }
        }

        super.markNoUpdate();
    }

    public boolean configInvHasItem() {
        for (int i = 0; i < configInventory.getSlots(); i++) {
            ItemStack stack = configInventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public void readConfigInventoryNBT(final NBTTagCompound compound) {
        configInventory = IOInventory.deserialize(this, compound);
        configInventory.setListener(slot -> {
            synchronized (this) {
                changedSlots[slot] = true;
            }
        });

        int[] slotIDs = new int[configInventory.getSlots()];
        for (int slotID = 0; slotID < slotIDs.length; slotID++) {
            slotIDs[slotID] = slotID;
        }
        configInventory.setStackLimit(Integer.MAX_VALUE, slotIDs);
    }

    @SideOnly(Side.CLIENT)
    protected void processClientGUIUpdate() {
        if (world != null && world.isRemote) {
            GuiScreen currentScreen = Minecraft.getMinecraft().currentScreen;
            if (currentScreen instanceof GuiMEItemInputBus busGUI && busGUI.getOwner() == this) {
                busGUI.updateGUIState();
            }
        }
    }

    @Override
    public NBTTagCompound downloadSettings() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag(CONFIG_TAG_KEY, configInventory.writeNBT());
        return tag;
    }

    @Override
    public void uploadSettings(NBTTagCompound settings) {
        readConfigInventoryNBT(settings.getCompoundTag(CONFIG_TAG_KEY));
        try {
            proxy.getTick().alertDevice(proxy.getNode());
        } catch (GridAccessException e) {
            ModularMachinery.log.warn("Error while uploading settings", e);
        }
    }

    public enum MERequestMode {
        DEFAULT("default"),
        THRESHOLD("threshold");

        private final String name;

        MERequestMode(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public static MERequestMode fromName(String name) {
            for (MERequestMode mode : values()) {
                if (mode.name.equals(name)) {
                    return mode;
                }
            }
            return DEFAULT;
        }
    }
}