package github.kasuminova.mmce.common.network;

import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PktPerformanceReport implements IMessage, IMessageHandler<PktPerformanceReport, IMessage> {
    private int usedTime = 0;
    private int searchUsedTime = 0;

    public PktPerformanceReport() {
    }

    public PktPerformanceReport(int usedTime, int searchUsedTime) {
        this.usedTime = usedTime;
        this.searchUsedTime = searchUsedTime;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        usedTime = buf.readInt();
        searchUsedTime = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(usedTime);
        buf.writeInt(searchUsedTime);
    }

    @Override
    public IMessage onMessage(PktPerformanceReport message, MessageContext ctx) {
        TileMultiblockMachineController.usedTimeCache = message.usedTime;
        TileMultiblockMachineController.searchUsedTimeCache = message.searchUsedTime;
        return null;
    }
}
