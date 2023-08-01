package github.kasuminova.mmce.common.tile.base;

import appeng.api.AEApi;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridNode;
import appeng.api.networking.events.MENetworkChannelsChanged;
import appeng.api.networking.events.MENetworkEventSubscribe;
import appeng.api.networking.events.MENetworkPowerStatusChange;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.util.AECableType;
import appeng.api.util.AEPartLocation;
import appeng.api.util.DimensionalCoord;
import appeng.me.GridAccessException;
import appeng.me.helpers.AENetworkProxy;
import appeng.me.helpers.IGridProxyable;
import appeng.me.helpers.MachineSource;
import appeng.util.Platform;
import hellfirepvp.modularmachinery.common.tiles.base.MachineComponentTile;
import hellfirepvp.modularmachinery.common.tiles.base.SelectiveUpdateTileEntity;
import hellfirepvp.modularmachinery.common.tiles.base.TileColorableMachineComponent;
import hellfirepvp.modularmachinery.common.util.IOInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class MEItemBus extends TileColorableMachineComponent implements SelectiveUpdateTileEntity, MachineComponentTile, IActionHost, IGridProxyable, IGridTickable {

    protected final AENetworkProxy proxy = new AENetworkProxy(this, "aeProxy", getVisualItemStack(), true);
    protected final IActionSource source;
    protected final IItemStorageChannel channel = AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class);

    protected IOInventory inventory = buildInventory();

    public MEItemBus() {
        this.source = new MachineSource(this);
        proxy.setIdlePowerUsage(1.0D);
        proxy.setFlags(GridFlags.REQUIRE_CHANNEL);
    }

    public abstract IOInventory buildInventory();

    public abstract ItemStack getVisualItemStack();

    public IOInventory getInternalInventory() {
        return inventory;
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
        Capability<IItemHandler> cap = CapabilityItemHandler.ITEM_HANDLER_CAPABILITY;
        if (capability == cap) {
            return cap.cast(inventory);
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public void readCustomNBT(final NBTTagCompound compound) {
        super.readCustomNBT(compound);

        if (compound.hasKey("inventory")) {
            inventory = IOInventory.deserialize(this, compound.getCompoundTag("inventory"));
        }
        proxy.readFromNBT(compound);
    }

    @Override
    public void writeCustomNBT(final NBTTagCompound compound) {
        super.writeCustomNBT(compound);

        compound.setTag("inventory", inventory.writeNBT());
        proxy.writeToNBT(compound);
    }

    @Override
    public SPacketUpdateTileEntity getTrueUpdatePacket() {
        return super.getUpdatePacket();
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return null;
    }

    // AppEng Compat

    @MENetworkEventSubscribe
    public void stateChange(final MENetworkChannelsChanged change) {
        this.notifyNeighbors();
    }

    @MENetworkEventSubscribe
    public void stateChange(final MENetworkPowerStatusChange change) {
        this.notifyNeighbors();
    }

    private void notifyNeighbors() {
        if (this.proxy.isActive()) {
            try {
                this.proxy.getTick().wakeDevice(this.proxy.getNode());
            } catch (GridAccessException e) {
                // :P
            }
            Platform.notifyBlocksOfNeighbors(this.getWorld(), this.getPos());
        }
    }

    @Override
    public void gridChanged() {

    }

    @Nonnull
    @Override
    public IGridNode getActionableNode() {
        return proxy.getNode();
    }

    @Override
    public AENetworkProxy getProxy() {
        return proxy;
    }

    @Nonnull
    @Override
    public DimensionalCoord getLocation() {
        return new DimensionalCoord(this);
    }

    @Nullable
    @Override
    public IGridNode getGridNode(@Nonnull final AEPartLocation dir) {
        return proxy.getNode();
    }

    @Nonnull
    @Override
    public AECableType getCableConnectionType(@Nonnull final AEPartLocation dir) {
        return AECableType.SMART;
    }

    @Override
    public void securityBreak() {
        getWorld().destroyBlock(getPos(), true);
    }

    @Override
    public void onChunkUnload() {
        super.onChunkUnload();
        proxy.onChunkUnload();
    }

    @Override
    public void invalidate() {
        super.invalidate();
        proxy.invalidate();
    }

    @Override
    public void validate() {
        super.validate();
        proxy.onReady();
    }
}
