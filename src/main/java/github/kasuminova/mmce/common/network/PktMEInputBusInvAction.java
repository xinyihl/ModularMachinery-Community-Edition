package github.kasuminova.mmce.common.network;

import appeng.container.slot.SlotFake;
import github.kasuminova.mmce.common.container.ContainerMEItemInputBus;
import github.kasuminova.mmce.common.tile.MEItemInputBus;
import hellfirepvp.modularmachinery.ModularMachinery;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PktMEInputBusInvAction implements IMessage, IMessageHandler<PktMEInputBusInvAction, IMessage> {
    private Action action = null;
    private int addAmount = 0;
    private int slotID = 0;
    private int thresholdValue = 0;

    public PktMEInputBusInvAction() {
    }

    public PktMEInputBusInvAction(final Action action) {
        this.action = action;
    }

    public PktMEInputBusInvAction(final Action action, final int thresholdValue) {
        this.action = action;
        this.thresholdValue = thresholdValue;
    }

    public PktMEInputBusInvAction(final Action action, int addAmount, final int slotID) {
        this.action = action;
        this.addAmount = addAmount;
        this.slotID = slotID;
    }

    @Override
    public void fromBytes(final ByteBuf buf) {
        this.addAmount = buf.readInt();
        this.slotID = buf.readInt();
        this.action = PktMEInputBusInvAction.Action.values()[buf.readByte()];
        this.thresholdValue = buf.readInt();
    }

    @Override
    public void toBytes(final ByteBuf buf) {
        buf.writeInt(addAmount);
        buf.writeInt(slotID);
        buf.writeByte(action.ordinal());
        buf.writeInt(thresholdValue);
    }

    @Override
    public IMessage onMessage(final PktMEInputBusInvAction message, final MessageContext ctx) {
        EntityPlayerMP player = ctx.getServerHandler().player;
        if (!(player.openContainer instanceof final ContainerMEItemInputBus inputBus)) {
            return null;
        }
        ModularMachinery.EXECUTE_MANAGER.addSyncTask(() -> {
            MEItemInputBus provider = inputBus.getOwner();
            switch (message.action) {
                case ENABLE_THRESHOLD_MODE -> provider.setSlotProcessMode(MEItemInputBus.MERequestMode.THRESHOLD);
                case ENABLE_DEFAULT_MODE -> provider.setSlotProcessMode(MEItemInputBus.MERequestMode.DEFAULT);
                case SET_THRESHOLD -> provider.setThresholdValue(message.thresholdValue);
            }
        });

        Slot slot = inputBus.getSlot(message.slotID);
        if (!(slot instanceof SlotFake)) {
            return null;
        }

        ItemStack stack = slot.getStack();
        if (stack.isEmpty()) {
            return null;
        }

        int addAmount = message.addAmount;
        if (addAmount == 0) {
            return null;
        }

        int count = stack.getCount();
        if (addAmount > 0) {
            stack.grow(Math.min(slot.getSlotStackLimit() - count, addAmount));
            slot.onSlotChanged();
        } else {
            int decrAmount = -addAmount;
            stack.shrink(Math.min(count - 1, decrAmount));
            slot.onSlotChanged();
        }

        return null;
    }

    public enum Action {
        ENABLE_THRESHOLD_MODE,
        ENABLE_DEFAULT_MODE,
        SET_THRESHOLD,
        ADD_AMOUNT
    }
}
