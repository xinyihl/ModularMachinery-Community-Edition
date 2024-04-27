package github.kasuminova.mmce.common.tile;

import appeng.api.AEApi;
import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.crafting.ICraftingProviderHelper;
import appeng.api.networking.events.MENetworkCraftingPatternChange;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.channels.IFluidStorageChannel;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.me.GridAccessException;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.util.Platform;
import appeng.util.inv.IAEAppEngInventory;
import appeng.util.inv.InvOperation;
import com.glodblock.github.common.item.fake.FakeFluids;
import com.glodblock.github.common.item.fake.FakeItemRegister;
import github.kasuminova.mmce.client.gui.GuiMEPatternProvider;
import github.kasuminova.mmce.common.tile.base.MEMachineComponent;
import github.kasuminova.mmce.common.util.InfItemFluidHandler;
import github.kasuminova.mmce.common.util.PatternItemFilter;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.base.Mods;
import hellfirepvp.modularmachinery.common.crafting.ComponentType;
import hellfirepvp.modularmachinery.common.lib.ComponentTypesMM;
import hellfirepvp.modularmachinery.common.lib.ItemsMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

public class MEPatternProvider extends MEMachineComponent implements ICraftingProvider, IAEAppEngInventory {

    public static final int PATTERNS = 36;

    protected final InfItemFluidHandler handler = new InfItemFluidHandler();

    protected final AppEngInternalInventory patterns = new AppEngInternalInventory(this, PATTERNS, 1, PatternItemFilter.INSTANCE);
    protected final List<ICraftingPatternDetails> details = new ObjectArrayList<>(PATTERNS);

    protected boolean blockingMode = false;
    protected boolean shouldReturnItems = false;

    public MEPatternProvider() {
        // Initialize details...
        IntStream.range(0, 36).<ICraftingPatternDetails>mapToObj(i -> null).forEach(details::add);
        // Set handler onChanged consumer...
        handler.setOnItemChanged(slot -> markNoUpdateSync());
        handler.setOnFluidChanged(slot -> markNoUpdateSync());
    }

    @Override
    public ItemStack getVisualItemStack() {
        return new ItemStack(ItemsMM.mePatternProvider, 1);
    }

    @Nullable
    @Override
    public MachineComponent<InfItemFluidHandler> provideComponent() {
        return new MachineComponent<>(IOType.INPUT) {
            @Override
            public ComponentType getComponentType() {
                return ComponentTypesMM.COMPONENT_ITEM_FLUID;
            }

            @Override
            public InfItemFluidHandler getContainerProvider() {
                return handler;
            }
        };
    }

    @Override
    public void provideCrafting(final ICraftingProviderHelper craftingTracker) {
        if (!proxy.isActive() || !proxy.isPowered()) {
            return;
        }
        details.stream()
                .filter(Objects::nonNull)
                .forEach(detail -> craftingTracker.addCraftingOption(this, detail));
    }

    @Override
    public boolean pushPattern(final ICraftingPatternDetails patternDetails, final InventoryCrafting table) {
        if (patternDetails.isCraftable() || !proxy.isActive() || !proxy.isPowered()) {
            return false;
        }

        int slots = table.getSizeInventory();
        for (int slot = 0; slot < slots; slot++) {
            ItemStack stackInSlot = table.getStackInSlot(slot);
            if (stackInSlot.isEmpty()) {
                continue;
            }

            if (Mods.AE2FCR.isPresent() && FakeFluids.isFluidFakeItem(stackInSlot)) {
                FluidStack fluidStack = FakeItemRegister.getStack(stackInSlot);
                if (fluidStack != null) {
                    handler.fill(fluidStack, true);
                    continue;
                }
            }

            handler.appendItem(stackInSlot);
        }

        return true;
    }

    @Override
    public void notifyNeighbors() {
        if (this.getProxy().isActive()) {
            try {
                this.getProxy().getGrid().postEvent(new MENetworkCraftingPatternChange(this, this.getProxy().getNode()));
            } catch (GridAccessException ignored) {
            }
        }

        Platform.notifyBlocksOfNeighbors(this.getWorld(), this.getPos());
    }

    @Override
    public boolean isBusy() {
        return blockingMode && !handler.isEmpty();
    }

    protected void refreshPatterns() {
        for (int i = 0; i < PATTERNS; i++) {
            refreshPattern(i);
        }
        try {
            this.getProxy().getGrid().postEvent(new MENetworkCraftingPatternChange(this, this.getProxy().getNode()));
        } catch (GridAccessException ignored) {
        }
    }

    protected void refreshPattern(final int slot) {
        details.set(slot, null);

        ItemStack pattern = patterns.getStackInSlot(slot);
        Item item = pattern.getItem();
        if (pattern.isEmpty() || !(item instanceof ICraftingPatternItem patternItem)) {
            return;
        }

        ICraftingPatternDetails detail = patternItem.getPatternForItem(pattern, getWorld());
        if (detail != null) {
            details.set(slot, detail);
        }
    }

    public void returnItemsScheduled() {
        if (!shouldReturnItems) {
            shouldReturnItems = true;
            ModularMachinery.EXECUTE_MANAGER.addSyncTask(this::returnItems);
        }
    }

    public void returnItems() {
        if (!shouldReturnItems || !proxy.isActive() || !proxy.isPowered()) {
            return;
        }
        shouldReturnItems = false;

        try {
            List<ItemStack> itemStackList = handler.getItemStackList();
            List<FluidStack> fluidStackList = handler.getFluidStackList();

            IItemStorageChannel itemChannel = AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class);
            IMEMonitor<IAEItemStack> itemInv = proxy.getStorage().getInventory(itemChannel);

            for (int i = 0; i < itemStackList.size(); i++) {
                final ItemStack stack = itemStackList.get(i);
                if (stack.isEmpty()) {
                    continue;
                }
                IAEItemStack notInserted = insertStackToAE(itemInv, itemChannel.createStack(stack));
                if (notInserted != null) {
                    itemStackList.set(i, notInserted.createItemStack());
                } else {
                    itemStackList.set(i, ItemStack.EMPTY);
                }
            }

            IFluidStorageChannel fluidChannel = AEApi.instance().storage().getStorageChannel(IFluidStorageChannel.class);
            IMEMonitor<IAEFluidStack> fluidInv = proxy.getStorage().getInventory(fluidChannel);
            for (int i = 0; i < fluidStackList.size(); i++) {
                final FluidStack stack = fluidStackList.get(i);
                if (stack == null) {
                    continue;
                }
                IAEFluidStack notInserted = insertStackToAE(fluidInv, fluidChannel.createStack(stack));
                if (notInserted != null) {
                    fluidStackList.set(i, notInserted.getFluidStack());
                } else {
                    fluidStackList.set(i, null);
                }
            }
        } catch (GridAccessException ignored) {
        }

        // TODO: Bandwidth Issue.
        markNoUpdateSync();
    }

    private <T extends IAEStack<T>> T insertStackToAE(final IMEMonitor<T> inv, final T stack) throws GridAccessException {
        if (stack == null) {
            return null;
        }
        return Platform.poweredInsert(proxy.getEnergy(), inv, stack.copy(), source);
    }

    public InfItemFluidHandler getInfHandler() {
        return handler;
    }

    public AppEngInternalInventory getPatterns() {
        return patterns;
    }

    public boolean isBlockingMode() {
        return blockingMode;
    }

    public void setBlockingMode(final boolean blockingMode) {
        this.blockingMode = blockingMode;
    }

    @Nullable
    @Override
    public <T> T getCapability(@Nonnull final Capability<T> capability, @Nullable final EnumFacing facing) {
        Capability<IItemHandler> cap = CapabilityItemHandler.ITEM_HANDLER_CAPABILITY;
        if (capability == cap) {
            return cap.cast(patterns);
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public void readCustomNBT(final NBTTagCompound compound) {
        super.readCustomNBT(compound);
        handler.readFromNBT(compound, "handler");
        patterns.readFromNBT(compound, "patterns");
        blockingMode = compound.getBoolean("blockingMode");

        if (FMLCommonHandler.instance().getSide().isClient()) {
            processClientGUIUpdate();
        }
    }

    @SideOnly(Side.CLIENT)
    protected void processClientGUIUpdate() {
        if (world != null && world.isRemote) {
            GuiScreen currentScreen = Minecraft.getMinecraft().currentScreen;
            if (currentScreen instanceof GuiMEPatternProvider providerGUI && providerGUI.getOwner() == this) {
                providerGUI.updateGUIState();
            }
        }
    }

    @Override
    public void writeCustomNBT(final NBTTagCompound compound) {
        super.writeCustomNBT(compound);
        handler.writeToNBT(compound, "handler");
        patterns.writeToNBT(compound, "patterns");
        compound.setBoolean("blockingMode", blockingMode);
    }

    @Override
    public void validate() {
        super.validate();
        refreshPatterns();
    }

    @Override
    public void saveChanges() {
        markNoUpdateSync();
    }

    @Override
    public void onChangeInventory(final IItemHandler inv, final int slot, final InvOperation mc, final ItemStack removedStack, final ItemStack newStack) {
        refreshPattern(slot);
        try {
            this.getProxy().getGrid().postEvent(new MENetworkCraftingPatternChange(this, this.getProxy().getNode()));
        } catch (GridAccessException ignored) {
        }
    }

}
