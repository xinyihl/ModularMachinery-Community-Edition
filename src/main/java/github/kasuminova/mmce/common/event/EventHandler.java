package github.kasuminova.mmce.common.event;

import hellfirepvp.modularmachinery.common.tiles.TileMachineController;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

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
        if (te instanceof TileMachineController) {
            ((TileMachineController) te).markForUpdate();
        }
    }
}
