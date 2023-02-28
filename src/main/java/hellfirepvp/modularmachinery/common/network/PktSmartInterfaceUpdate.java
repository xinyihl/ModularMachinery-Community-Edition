package hellfirepvp.modularmachinery.common.network;

import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.container.ContainerSmartInterface;
import hellfirepvp.modularmachinery.common.tiles.TileSmartInterface;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import org.apache.commons.lang3.tuple.MutableTriple;
import org.apache.commons.lang3.tuple.Triple;

public class PktSmartInterfaceUpdate implements IMessage, IMessageHandler<PktSmartInterfaceUpdate, IMessage> {
    private Triple<BlockPos, String, Float> newData = null;

    public PktSmartInterfaceUpdate() {
    }

    public PktSmartInterfaceUpdate(Triple<BlockPos, String, Float> newData) {
        this.newData = newData;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        NBTTagCompound tag = ByteBufUtils.readTag(buf);
        if (tag != null) {
            newData = TileSmartInterface.deserializeBoundData(tag);
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeTag(buf, TileSmartInterface.serializeBoundData(newData));
    }

    @Override
    public IMessage onMessage(PktSmartInterfaceUpdate message, MessageContext ctx) {
        if (message == null) {
            return null;
        }

        EntityPlayerMP player = ctx.getServerHandler().player;
        if (!(player.openContainer instanceof ContainerSmartInterface)) {
            return null;
        }

        Triple<BlockPos, String, Float> newData = message.newData;
        TileSmartInterface owner = ((ContainerSmartInterface) player.openContainer).getOwner();
        TileSmartInterface.SmartInterfaceProvider provider = owner.provideComponent();
        MutableTriple<BlockPos, String, Float> machineData = (MutableTriple<BlockPos, String, Float>) provider.getMachineData(newData.getLeft());
        if (machineData != null) {
            machineData.setRight(newData.getRight());
            ModularMachinery.EXECUTE_MANAGER.addMainThreadTask(owner::markForUpdate);
        }

        return null;
    }
}
