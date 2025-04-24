package hellfirepvp.modularmachinery.common.network;

import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.container.ContainerFactoryController;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
/**
 * Almost an exact copy of PktTileMachineControllerAction, but I prefer to handle
 * actions for different base class implementations separately.
 */
public class PktTileFactoryControllerAction implements IMessage, IMessageHandler<PktTileFactoryControllerAction, IMessage> {

    private PktTileFactoryControllerAction.Action action = null;

    public PktTileFactoryControllerAction() {
    }

    public PktTileFactoryControllerAction(final PktTileFactoryControllerAction.Action action) {
        this.action = action;
    }

    @Override
    public void fromBytes(final ByteBuf buf) {
        action = PktTileFactoryControllerAction.Action.values()[buf.readByte()];
    }

    @Override
    public void toBytes(final ByteBuf buf) {
        buf.writeByte(action.ordinal());
    }

    @Override
    public IMessage onMessage(final PktTileFactoryControllerAction message, final MessageContext ctx) {
        EntityPlayerMP player = ctx.getServerHandler().player;
        if (!(player.openContainer instanceof final ContainerFactoryController containerFactoryController)) {
            return null;
        }

        ModularMachinery.EXECUTE_MANAGER.addSyncTask(() -> {
            TileMultiblockMachineController tileController = containerFactoryController.getOwner();
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
