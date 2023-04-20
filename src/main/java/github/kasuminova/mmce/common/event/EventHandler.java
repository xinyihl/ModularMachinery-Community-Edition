package github.kasuminova.mmce.common.event;

import github.kasuminova.mmce.common.network.PktPerformanceReport;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.container.ContainerBase;
import hellfirepvp.modularmachinery.common.data.Config;
import hellfirepvp.modularmachinery.common.tiles.base.SelectiveUpdateTileEntity;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

public class EventHandler {
    /**
     * <p>当玩家对打开控制器界面时更新控制器的信息，以避免某些消息不同步的问题。</p>
     * <p>Update the controller's information when the player interacts with the controller,
     * Avoid the problem of some messages being out of sync.</p>
     */
    @SubscribeEvent
    public void onPlayerRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        World world = event.getWorld();
        if (Config.selectiveUpdateTileEntity || world.isRemote) {
            return;
        }

        TileEntity te = world.getTileEntity(event.getPos());
        if (!(te instanceof SelectiveUpdateTileEntity)) {
            return;
        }
        SPacketUpdateTileEntity packet = ((SelectiveUpdateTileEntity) te).getTrueUpdatePacket();
        if (event.getEntityPlayer() instanceof EntityPlayerMP) {
            ((EntityPlayerMP) event.getEntityPlayer()).connection.sendPacket(packet);
        }
    }

    /**
     * <p>针对某些容易消耗大量带宽的方块实体提供选择性更新功能，以缓解网络压力。</p>
     * <p>Provide selective updates for certain square entities that tend to consume a lot of bandwidth to relieve network pressure.</p>
     */
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START || !Config.selectiveUpdateTileEntity || event.side == Side.CLIENT) {
            return;
        }

        EntityPlayer player = event.player;
        if (!(player.openContainer instanceof ContainerBase)) {
            return;
        }
        TileEntity te = ((ContainerBase<?>) player.openContainer).getOwner();
        if (!(te instanceof SelectiveUpdateTileEntity)) {
            return;
        }

        SPacketUpdateTileEntity packet = ((SelectiveUpdateTileEntity) te).getTrueUpdatePacket();

        if (event.player instanceof EntityPlayerMP) {
            EntityPlayerMP playerMP = (EntityPlayerMP) player;
            playerMP.connection.sendPacket(packet);

            World world = event.player.getEntityWorld();
            if (world.getWorldTime() % 15 == 0 && te instanceof TileMultiblockMachineController) {
                TileMultiblockMachineController ctrl = (TileMultiblockMachineController) te;
                int usedTime = ctrl.usedTimeAvg();
                TileMultiblockMachineController.performanceCache = usedTime;
                ModularMachinery.NET_CHANNEL.sendTo(new PktPerformanceReport(usedTime), playerMP);
            }
        }
    }
}
