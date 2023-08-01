package github.kasuminova.mmce.common.handler;

import appeng.container.AEBaseContainer;
import github.kasuminova.mmce.common.network.PktPerformanceReport;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.base.Mods;
import hellfirepvp.modularmachinery.common.container.ContainerBase;
import hellfirepvp.modularmachinery.common.tiles.base.SelectiveUpdateTileEntity;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

@SuppressWarnings("MethodMayBeStatic")
public class EventHandler {
    /**
     * <p>当玩家对打开控制器界面时更新控制器的信息，以避免某些消息不同步的问题。</p>
     * <p>Update the controller's information when the player interacts with the controller,
     * Avoid the problem of some messages being out of sync.</p>
     */
    @SubscribeEvent
    public void onPlayerRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        World world = event.getWorld();
        if (world.isRemote) {
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
        if (event.phase != TickEvent.Phase.START || event.side == Side.CLIENT) {
            return;
        }

        EntityPlayer player = event.player;

        Container container = player.openContainer;
        TileEntity te;
        if (container instanceof ContainerBase) {
            te = ((ContainerBase<?>) container).getOwner();
        } else {
            if (Mods.AE2.isPresent() && container instanceof AEBaseContainer) {
                te = ((AEBaseContainer) container).getTileEntity();
            } else {
                return;
            }
        }

        if (!(te instanceof SelectiveUpdateTileEntity)) {
            return;
        }

        if (checkTERange(player, te)) {
            player.closeScreen();
            return;
        }

        ModularMachinery.EXECUTE_MANAGER.addSyncTask(() -> {
            SPacketUpdateTileEntity packet = ((SelectiveUpdateTileEntity) te).getTrueUpdatePacket();

            if (event.player instanceof EntityPlayerMP) {
                EntityPlayerMP playerMP = (EntityPlayerMP) player;
                playerMP.connection.sendPacket(packet);

                World world = event.player.getEntityWorld();
                if (world.getWorldTime() % 15 == 0 && te instanceof TileMultiblockMachineController) {
                    TileMultiblockMachineController ctrl = (TileMultiblockMachineController) te;
                    int usedTime = ctrl.usedTimeAvg();
                    ModularMachinery.NET_CHANNEL.sendTo(new PktPerformanceReport(usedTime), playerMP);
                }
            }
        });
    }

    private static boolean checkTERange(final EntityPlayer player, final TileEntity te) {
        BlockPos tePos = te.getPos();
        BlockPos playerPos = player.getPosition();
        double distance = tePos.getDistance(playerPos.getX(), playerPos.getY(), playerPos.getZ());
        return distance >= 6.0D;
    }
}
