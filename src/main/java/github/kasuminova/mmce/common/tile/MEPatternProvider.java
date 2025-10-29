package github.kasuminova.mmce.common.tile;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.config.Upgrades;
import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.crafting.ICraftingProviderHelper;
import appeng.api.networking.events.MENetworkChannelsChanged;
import appeng.api.networking.events.MENetworkCraftingPatternChange;
import appeng.api.networking.events.MENetworkEventSubscribe;
import appeng.api.networking.events.MENetworkPowerStatusChange;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.channels.IFluidStorageChannel;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.util.IConfigManager;
import appeng.fluids.util.IAEFluidInventory;
import appeng.fluids.util.IAEFluidTank;
import appeng.helpers.DualityInterface;
import appeng.helpers.ICustomNameObject;
import appeng.helpers.IInterfaceHost;
import appeng.me.GridAccessException;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.util.Platform;
import appeng.util.inv.IAEAppEngInventory;
import appeng.util.inv.InvOperation;
import com.glodblock.github.common.item.fake.FakeFluids;
import com.glodblock.github.common.item.fake.FakeItemRegister;
import com.glodblock.github.integration.mek.FakeGases;
import com.google.common.collect.ImmutableSet;
import com.mekeng.github.common.me.data.IAEGasStack;
import com.mekeng.github.common.me.storage.IGasStorageChannel;
import github.kasuminova.mmce.client.gui.GuiMEPatternProvider;
import github.kasuminova.mmce.common.container.ContainerMEPatternProvider;
import github.kasuminova.mmce.common.event.machine.MachineEvent;
import github.kasuminova.mmce.common.event.recipe.FactoryRecipeFinishEvent;
import github.kasuminova.mmce.common.event.recipe.RecipeFinishEvent;
import github.kasuminova.mmce.common.network.PktMEPatternProviderHandlerItems;
import github.kasuminova.mmce.common.tile.base.MEMachineComponent;
import github.kasuminova.mmce.common.util.AEFluidInventoryUpgradeable;
import github.kasuminova.mmce.common.util.InfItemFluidHandler;
import github.kasuminova.mmce.common.util.PatternItemFilter;
import github.kasuminova.mmce.common.util.Sides;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.base.Mods;
import hellfirepvp.modularmachinery.common.crafting.ComponentType;
import hellfirepvp.modularmachinery.common.lib.ComponentTypesMM;
import hellfirepvp.modularmachinery.common.lib.ItemsMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.tiles.base.MachineComponentTileNotifiable;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import mekanism.api.gas.GasStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

public class MEPatternProvider extends MEMachineComponent implements ICraftingProvider, IAEAppEngInventory, IAEFluidInventory, MachineComponentTileNotifiable, IInterfaceHost, ICustomNameObject {

    public static final  int                           PATTERNS               = 36;
    public static final  int                           SUB_ITEM_HANDLER_SLOTS = 2;
    private static final ItemStack                     item                   = new ItemStack(ItemsMM.mePatternProvider);
    protected final      AppEngInternalInventory       subItemHandler         = new AppEngInternalInventory(this, SUB_ITEM_HANDLER_SLOTS);
    protected final      AEFluidInventoryUpgradeable   subFluidHandler        = new AEFluidInventoryUpgradeable(this, 1, Integer.MAX_VALUE);
    protected final      InfItemFluidHandler           handler                = new InfItemFluidHandler(subItemHandler, subFluidHandler);
    protected final      AppEngInternalInventory       patterns               = new AppEngInternalInventory(this, PATTERNS, 1, PatternItemFilter.INSTANCE);
    protected final      List<ICraftingPatternDetails> details                = new ObjectArrayList<>(PATTERNS);
    private final        DualityInterface              duality                = new DualityInterface(this.proxy, this);
    protected            WorkModeSetting               workMode               = WorkModeSetting.DEFAULT;
    protected volatile   boolean                       machineCompleted       = true;
    protected            boolean                       shouldReturnItems      = false;
    protected            boolean                       handlerDirty           = false;
    protected            int                           currentPatternIdx      = -1;
    protected            ICraftingPatternDetails       currentPattern         = null;
    private              String                        customName;
    private              String                        machineName;

    public MEPatternProvider() {
        // Initialize details...
        IntStream.range(0, 36).<ICraftingPatternDetails>mapToObj(i -> null).forEach(details::add);
        // Set handler onChanged consumer...
        handler.setOnItemChanged(slot -> {
            handlerDirty = true;
            markChunkDirty();
        });
        handler.setOnFluidChanged(slot -> {
            handlerDirty = true;
            markChunkDirty();
        });
        if (Mods.MEKANISM.isPresent() && Mods.MEKENG.isPresent()) {
            handler.setOnGasChanged(slot -> {
                handlerDirty = true;
                markChunkDirty();
            });
        }
    }

    @Nullable
    @Override
    public MachineComponent<InfItemFluidHandler> provideComponent() {
        return new MachineComponent<>(IOType.INPUT) {
            @Override
            public ComponentType getComponentType() {
                return ComponentTypesMM.COMPONENT_ITEM_FLUID_GAS;
            }

            @Override
            public InfItemFluidHandler getContainerProvider() {
                return handler;
            }

            @Override
            public boolean isAffectedBySeparateInput() {
                return true;
            }
        };
    }

    @MENetworkEventSubscribe
    public void stateChange(final MENetworkChannelsChanged change) {
        this.notifyNeighbors();
    }

    @MENetworkEventSubscribe
    public void stateChange(final MENetworkPowerStatusChange change) {
        this.notifyNeighbors();
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
        if (!acceptsPattern(patternDetails)) {
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
            if (Mods.MEKENG.isPresent() && FakeGases.isGasFakeItem(stackInSlot)) {
                GasStack gasStack = FakeItemRegister.getStack(stackInSlot);
                if (gasStack != null) {
                    handler.receiveGas(null, gasStack, true);
                    continue;
                }
            }

            handler.appendItem(stackInSlot);
        }

        handleNewPattern(patternDetails);
        machineCompleted = workMode != WorkModeSetting.CRAFTING_LOCK_MODE;
        return true;
    }

    private boolean acceptsPattern(final ICraftingPatternDetails patternDetails) {
        if (patternDetails.isCraftable() || !proxy.isActive() || !proxy.isPowered()) {
            return false;
        }
        // If workMode is Enhanced Blocking Mode, and the handler is not empty, and the current pattern is not the same as the new pattern, return false.
        return workMode != WorkModeSetting.ENHANCED_BLOCKING_MODE || handler.isEmpty() || currentPattern == null || currentPattern.equals(patternDetails);
    }

    private void handleNewPattern(final ICraftingPatternDetails patternDetails) {
        if (workMode == WorkModeSetting.ENHANCED_BLOCKING_MODE) {
            if (!patternDetails.equals(currentPattern)) {
                setCurrentPattern(patternDetails);
            }
        } else {
            resetCurrentPattern();
        }
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
        return (workMode == WorkModeSetting.CRAFTING_LOCK_MODE && !machineCompleted) ||
            (workMode == WorkModeSetting.BLOCKING_MODE && !handler.isEmpty());
    }

    protected void refreshPatterns() {
        for (int i = 0; i < PATTERNS; i++) {
            refreshPattern(i);
        }
        if (currentPatternIdx != -1 && currentPatternIdx < details.size()) {
            setCurrentPattern(details.get(currentPatternIdx));
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
        if (detail != null && !detail.isCraftable()) {
            details.set(slot, detail);
        }

        if (workMode == WorkModeSetting.ENHANCED_BLOCKING_MODE && slot == currentPatternIdx) {
            // If it is reading NBT.
            if (currentPattern == null) {
                currentPattern = detail;
            } else if (!currentPattern.equals(detail)) {
                resetCurrentPattern();
            }
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
        machineCompleted = true;

        try {
            synchronized (handler) {
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

                if (Mods.MEKANISM.isPresent() && Mods.MEKENG.isPresent()) {
                    returnGases();
                }
            }
        } catch (GridAccessException ignored) {
        }

        handlerDirty = true;
        markChunkDirty();
    }

    @Optional.Method(modid = "mekeng")
    private void returnGases() throws GridAccessException {
        List<GasStack> gasStackList = (List<GasStack>) handler.getGasStackList();
        IGasStorageChannel gasChannel = AEApi.instance().storage().getStorageChannel(IGasStorageChannel.class);
        IMEMonitor<IAEGasStack> gasInv = proxy.getStorage().getInventory(gasChannel);
        for (int i = 0; i < gasStackList.size(); i++) {
            final GasStack stack = gasStackList.get(i);
            if (stack == null) {
                continue;
            }
            IAEGasStack notInserted = insertStackToAE(gasInv, gasChannel.createStack(stack));
            if (notInserted != null) {
                gasStackList.set(i, notInserted.getGasStack());
            } else {
                gasStackList.set(i, null);
            }
        }
    }

    private <T extends IAEStack<T>> T insertStackToAE(final IMEMonitor<T> inv, final T stack) throws GridAccessException {
        if (stack == null) {
            return null;
        }
        return Platform.poweredInsert(proxy.getEnergy(), inv, stack.copy(), source);
    }

    private void resetCurrentPattern() {
        currentPatternIdx = -1;
        currentPattern = null;
    }

    private void setCurrentPattern(final ICraftingPatternDetails pattern) {
        if (pattern == null) {
            resetCurrentPattern();
            return;
        }
        currentPatternIdx = details.indexOf(pattern);
        currentPattern = pattern;
    }

    public AppEngInternalInventory getSubItemHandler() {
        return subItemHandler;
    }

    public AEFluidInventoryUpgradeable getSubFluidHandler() {
        return subFluidHandler;
    }

    public InfItemFluidHandler getInfHandler() {
        return handler;
    }

    public AppEngInternalInventory getPatterns() {
        return patterns;
    }

    public WorkModeSetting getWorkMode() {
        return workMode;
    }

    public void setWorkMode(final WorkModeSetting workMode) {
        this.workMode = workMode;
        if (workMode != WorkModeSetting.CRAFTING_LOCK_MODE) {
            this.machineCompleted = true;
        }
        if (workMode != WorkModeSetting.ENHANCED_BLOCKING_MODE) {
            resetCurrentPattern();
        }
    }

    @Override
    public boolean hasCapability(@Nonnull final Capability<?> capability, @Nullable final EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
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
        readProviderNBT(compound);
        if (compound.hasKey("machineCompleted")) {
            machineCompleted = compound.getBoolean("machineCompleted");
        }
        if (Sides.isRunningOnClient()) {
            processClientGUIUpdate();
        }
        if (compound.hasKey("customName")) {
            this.customName = compound.getString("customName");
        } else {
            this.customName = null;
        }
    }

    public void readProviderNBT(final NBTTagCompound compound) {
        subItemHandler.readFromNBT(compound, "subItemHandler");
        subFluidHandler.readFromNBT(compound, "subFluidHandler");
        handler.readFromNBT(compound, "handler");
        patterns.readFromNBT(compound, "patterns");
        workMode = WorkModeSetting.values()[compound.getByte("workMode")];

        if (compound.hasKey("currentPatternIdx") && workMode == WorkModeSetting.ENHANCED_BLOCKING_MODE) {
            currentPatternIdx = compound.getByte("currentPatternIdx");
        } else {
            resetCurrentPattern();
        }
    }

    @Override
    public void writeCustomNBT(final NBTTagCompound compound) {
        super.writeCustomNBT(compound);
        writeProviderNBT(compound);
        compound.setBoolean("machineCompleted", machineCompleted);
        if (this.customName != null) {
            compound.setString("customName", this.customName);
        }
    }

    public NBTTagCompound writeProviderNBT(final NBTTagCompound compound) {
        handler.writeToNBT(compound, "handler");
        patterns.writeToNBT(compound, "patterns");
        subItemHandler.writeToNBT(compound, "subItemHandler");
        subFluidHandler.writeToNBT(compound, "subFluidHandler");
        if (workMode != WorkModeSetting.DEFAULT) {
            compound.setByte("workMode", (byte) workMode.ordinal());
        }
        if (!handler.isEmpty() && currentPatternIdx != -1) {
            compound.setByte("currentPatternIdx", (byte) currentPatternIdx);
        }
        return compound;
    }

    public boolean isAllDefault() {
        if (IntStream.range(0, subItemHandler.getSlots()).mapToObj(subItemHandler::getStackInSlot).anyMatch(stackInSlot -> !stackInSlot.isEmpty())) {
            return false;
        }
        if (subFluidHandler.getFluidInSlot(0) != null) {
            return false;
        }
        if (IntStream.range(0, patterns.getSlots()).mapToObj(patterns::getStackInSlot).anyMatch(stackInSlot -> !stackInSlot.isEmpty())) {
            return false;
        }
        return workMode == WorkModeSetting.DEFAULT && handler.isEmpty();
    }

    public void sendHandlerItemsToClient() {
        if (world.isRemote || !handlerDirty) {
            return;
        }
        List<EntityPlayerMP> players = new ArrayList<>();
        world.playerEntities.stream()
                            .filter(EntityPlayerMP.class::isInstance)
                            .map(EntityPlayerMP.class::cast)
                            .forEach(playerMP -> {
                                if (playerMP.openContainer instanceof ContainerMEPatternProvider cPatternProvider) {
                                    if (cPatternProvider.getOwner() == this) {
                                        players.add(playerMP);
                                    }
                                }
                            });
        if (!players.isEmpty()) {
            PktMEPatternProviderHandlerItems message = new PktMEPatternProviderHandlerItems(this);
            players.forEach(player -> ModularMachinery.NET_CHANNEL.sendTo(message, player));
        }
        handlerDirty = false;
    }

    protected void processClientGUIUpdate() {
        if (world != null && world.isRemote) {
            GuiScreen currentScreen = Minecraft.getMinecraft().currentScreen;
            if (currentScreen instanceof GuiMEPatternProvider providerGUI && providerGUI.getOwner() == this) {
                providerGUI.updateGUIState();
            }
        }
    }

    @Override
    public void validate() {
        super.validate();
        if (!world.isRemote) {
            ModularMachinery.EXECUTE_MANAGER.addSyncTask(this::refreshPatterns);
        }
    }

    @Override
    public void saveChanges() {
        markNoUpdateSync();
    }

    @Override
    public void markChunkDirty() {
        super.markChunkDirty();
        if (handlerDirty && !world.isRemote) {
            ModularMachinery.EXECUTE_MANAGER.addSyncTask(this::sendHandlerItemsToClient);
        }
    }

    @Override
    public void onChangeInventory(final IItemHandler inv, final int slot, final InvOperation mc, final ItemStack removedStack, final ItemStack newStack) {
        refreshPattern(slot);
        try {
            this.getProxy().getGrid().postEvent(new MENetworkCraftingPatternChange(this, this.getProxy().getNode()));
        } catch (GridAccessException ignored) {
        }
    }

    @Override
    public void onFluidInventoryChanged(final IAEFluidTank inv, final int slot) {
        markChunkDirty();
    }

    @Override
    public void onMachineEvent(final MachineEvent event) {
        if (event instanceof RecipeFinishEvent || event instanceof FactoryRecipeFinishEvent) {
            if (!machineCompleted) {
                machineCompleted = true;
                ModularMachinery.EXECUTE_MANAGER.addSyncTask(() ->
                    event.getController().setSearchRecipeImmediately(true));
            }
        }
    }

    public String getMachineName() {
        if (machineName == null) {
            return item.getItem().getTranslationKey();
        } else {
            return machineName;
        }
    }

    public void setMachineName(String name) {
        this.machineName = name;
    }

    @Override
    public ItemStack getVisualItemStack() {
        return item;
    }

    @Override
    public String getCustomInventoryName() {
        if (this.hasCustomInventoryName()) {
            return this.customName;
        } else {
            return item.getItem().getTranslationKey();
        }
    }

    @Override
    public boolean hasCustomInventoryName() {
        return this.customName != null && !this.customName.isEmpty();
    }

    @Override
    public void setCustomName(@Nullable String customName) {
        this.customName = customName;
    }

    @Override
    public DualityInterface getInterfaceDuality() {
        return duality;
    }

    @Override
    public EnumSet<EnumFacing> getTargets() {
        return EnumSet.allOf(EnumFacing.class);
    }

    @Override
    public TileEntity getTileEntity() {
        return this;
    }

    @Override
    public int getInstalledUpgrades(Upgrades upgrades) {
        return 3;
    }

    @Override
    public TileEntity getTile() {
        return this;
    }

    @Override
    public IItemHandler getInventoryByName(String name) {
        return this.duality.getInventoryByName(name);
    }

    @Override
    public ImmutableSet<ICraftingLink> getRequestedJobs() {
        return this.duality.getRequestedJobs();
    }

    @Override
    public IAEItemStack injectCraftedItems(ICraftingLink link, IAEItemStack items, Actionable mode) {
        return this.duality.injectCraftedItems(link, items, mode);
    }

    @Override
    public void jobStateChange(ICraftingLink iCraftingLink) {
        this.duality.jobStateChange(iCraftingLink);
    }

    @Override
    public IConfigManager getConfigManager() {
        return this.duality.getConfigManager();
    }

    public enum WorkModeSetting {
        DEFAULT,
        BLOCKING_MODE,
        CRAFTING_LOCK_MODE,
        ENHANCED_BLOCKING_MODE,
    }

}
