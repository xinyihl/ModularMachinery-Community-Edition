package github.kasuminova.mmce.common.handler;

import appeng.container.AEBaseContainer;
import github.kasuminova.mmce.common.network.PktPerformanceReport;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.base.Mods;
import hellfirepvp.modularmachinery.common.container.ContainerBase;
import hellfirepvp.modularmachinery.common.lib.ItemsMM;
import hellfirepvp.modularmachinery.common.tiles.base.SelectiveUpdateTileEntity;
import hellfirepvp.modularmachinery.common.tiles.base.TileEntitySynchronized;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SuppressWarnings("MethodMayBeStatic")
public class EventHandler {
    /**
     * <p>当玩家对某些方块实体右击时更新方块实体，以避免某些消息不同步的问题。</p>
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
        if (!(te instanceof SelectiveUpdateTileEntity) || !(te instanceof final TileEntitySynchronized teSync)) {
            return;
        }

        // 触发更新，并使其同步至客户端。
        teSync.notifyUpdate();
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

        if (!(te instanceof SelectiveUpdateTileEntity) || !(te instanceof final TileEntitySynchronized teSync)) {
            return;
        }

        if (checkTERange(player, te)) {
            player.closeScreen();
            return;
        }

        teSync.markForUpdateSync();

        ModularMachinery.EXECUTE_MANAGER.addSyncTask(() -> {
            if (event.player instanceof EntityPlayerMP) {
                EntityPlayerMP playerMP = (EntityPlayerMP) player;
                if (teSync.getLastUpdateTick() + 1 >= playerMP.world.getTotalWorldTime()) {
                    teSync.notifyUpdate();
                }

                World world = event.player.getEntityWorld();
                if (world.getWorldTime() % 15 == 0 && te instanceof final TileMultiblockMachineController ctrl) {
                    int usedTime = ctrl.usedTimeAvg();
                    int searchUsedTimeAvg = ctrl.recipeSearchUsedTimeAvg();
                    ModularMachinery.NET_CHANNEL.sendTo(new PktPerformanceReport(usedTime, searchUsedTimeAvg), playerMP);
                }
            }
        });
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onMEItemBusItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        Item item = stack.getItem();
        if (item != ItemsMM.meItemInputBus && item != ItemsMM.meItemOutputBus) {
            return;
        }

        if (stack.getTagCompound() != null && stack.getTagCompound().hasKey("inventory")) {
            event.getToolTip().add(I18n.format("gui.meitembus.nbt_stored"));
        }
    }

    private static boolean checkTERange(final EntityPlayer player, final TileEntity te) {
        BlockPos tePos = te.getPos();
        BlockPos playerPos = player.getPosition();
        return tePos.getDistance(playerPos.getX(), playerPos.getY(), playerPos.getZ()) >= 6.0D;
    }
}
