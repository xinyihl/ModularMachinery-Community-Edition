package hellfirepvp.modularmachinery.common.tiles.base;

import net.minecraft.network.play.server.SPacketUpdateTileEntity;

public interface SelectiveUpdateTileEntity {
    SPacketUpdateTileEntity getTrueUpdatePacket();

}
