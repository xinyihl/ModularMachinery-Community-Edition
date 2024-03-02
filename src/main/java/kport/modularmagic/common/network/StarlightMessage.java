package kport.modularmagic.common.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class StarlightMessage implements IMessage {

    public int starlightAmount;
    public BlockPos pos;

    public StarlightMessage() {
    }

    public StarlightMessage(int starlightAmount, BlockPos pos) {
        this.starlightAmount = starlightAmount;
        this.pos = pos;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        starlightAmount = buf.readInt();
        pos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(starlightAmount);
        buf.writeInt(pos.getX());
        buf.writeInt(pos.getY());
        buf.writeInt(pos.getZ());
    }
}
