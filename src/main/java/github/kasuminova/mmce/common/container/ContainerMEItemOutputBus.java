package github.kasuminova.mmce.common.container;

import appeng.container.AEBaseContainer;
import appeng.container.slot.SlotNormal;
import github.kasuminova.mmce.common.tile.MEItemOutputBus;
import net.minecraft.entity.player.EntityPlayer;

public class ContainerMEItemOutputBus extends AEBaseContainer {
    private final MEItemOutputBus owner;

    public ContainerMEItemOutputBus(final MEItemOutputBus owner, final EntityPlayer player) {
        super(player.inventory, owner);

        this.owner = owner;

        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 9; x++) {
                this.addSlotToContainer(new SlotNormal(this.owner.getInternalInventory().asGUIAccess(), y * 9 + x, 8 + 18 * x, 24 + 18 * y));
            }
        }

        this.bindPlayerInventory(getInventoryPlayer(), 0, 195 - /* height of player inventory */ 82);
    }

    public MEItemOutputBus getOwner() {
        return owner;
    }
}
