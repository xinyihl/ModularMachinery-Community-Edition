package github.kasuminova.mmce.common.event;

import hellfirepvp.modularmachinery.common.container.ContainerBase;
import hellfirepvp.modularmachinery.common.data.Config;
import hellfirepvp.modularmachinery.common.tiles.base.SelectiveUpdateTileEntity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class EventHandler {
    /**
     * <p>当玩家对打开控制器界面时更新控制器的信息，以避免某些消息不同步的问题。</p>
     * <p>Update the controller's information when the player interacts with the controller,
     * Avoid the problem of some messages being out of sync.</p>
     */
    @SubscribeEvent
    @SideOnly(Side.SERVER)
    public void onPlayerRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        World world = event.getWorld();
        if (Config.selectiveUpdateTileEntity || !(event.getEntityPlayer() instanceof EntityPlayerMP)) {
            return;
        }

        TileEntity te = world.getTileEntity(event.getPos());
        EntityPlayerMP player = (EntityPlayerMP) event.getEntityPlayer();
        if (te instanceof SelectiveUpdateTileEntity) {
            player.connection.sendPacket(((SelectiveUpdateTileEntity) te).getTrueUpdatePacket());
        }
    }

    /**
     * <p>针对某些容易消耗大量带宽的方块实体提供选择性更新功能，以缓解网络压力。</p>
     * <p>Provide selective updates for certain square entities that tend to consume a lot of bandwidth to relieve network pressure.</p>
     */
    @SubscribeEvent
    @SideOnly(Side.SERVER)
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START || !Config.selectiveUpdateTileEntity || !(event.player instanceof EntityPlayerMP)) {
            return;
        }

        EntityPlayerMP player = (EntityPlayerMP) event.player;
        if (player.openContainer instanceof ContainerBase) {
            TileEntity te = ((ContainerBase<?>) player.openContainer).getOwner();
            if (te instanceof SelectiveUpdateTileEntity) {
                player.connection.sendPacket(((SelectiveUpdateTileEntity) te).getTrueUpdatePacket());
            }
        }
    }
}
