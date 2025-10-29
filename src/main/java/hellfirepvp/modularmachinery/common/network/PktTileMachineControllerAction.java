package hellfirepvp.modularmachinery.common.network;

import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.container.ContainerController;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PktTileMachineControllerAction implements IMessage, IMessageHandler<PktTileMachineControllerAction, IMessage> {

    private PktTileMachineControllerAction.Action action = null;

    public PktTileMachineControllerAction() {
    }

    public PktTileMachineControllerAction(final PktTileMachineControllerAction.Action action) {
        this.action = action;
    }

    @Override
    public void fromBytes(final ByteBuf buf) {
        action = PktTileMachineControllerAction.Action.values()[buf.readByte()];
    }

    @Override
    public void toBytes(final ByteBuf buf) {
        buf.writeByte(action.ordinal());
    }

    @Override
    public IMessage onMessage(final PktTileMachineControllerAction message, final MessageContext ctx) {
        EntityPlayerMP player = ctx.getServerHandler().player;
        if (!(player.openContainer instanceof final ContainerController containerController)) {
            return null;
        }
        ModularMachinery.EXECUTE_MANAGER.addSyncTask(() -> {
            TileMultiblockMachineController tileController = containerController.getOwner();
            switch (message.action) {
                case ENABLE_SEPARATE_INPUT_MODE -> tileController.setInputMode(TileMultiblockMachineController.InputMode.SEPARATE_INPUT);
                case ENABLE_DEFAULT_MODE -> tileController.setInputMode(TileMultiblockMachineController.InputMode.DEFAULT);
            }
        });
        return null;
    }

    public enum Action {
        ENABLE_SEPARATE_INPUT_MODE,
        ENABLE_DEFAULT_MODE,
    }

}
