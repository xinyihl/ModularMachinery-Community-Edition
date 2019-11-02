package hellfirepvp.modularmachinery.common.container;

import hellfirepvp.modularmachinery.common.tiles.base.TileItemBus;
import net.minecraft.entity.player.EntityPlayer;

public class ContainerRedstoneBus extends ContainerItemBus{
    public ContainerRedstoneBus(TileItemBus owner, EntityPlayer opening) {
        super(owner, opening);
    }
}
