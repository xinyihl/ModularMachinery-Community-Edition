package github.kasuminova.mmce.common.container;

import appeng.container.AEBaseContainer;
import appeng.container.slot.SlotDisabled;
import appeng.container.slot.SlotFake;
import github.kasuminova.mmce.common.tile.MEItemInputBus;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.items.IItemHandlerModifiable;

public class ContainerMEItemInputBus extends AEBaseContainer {
    private final MEItemInputBus owner;

    public ContainerMEItemInputBus(final MEItemInputBus owner, final EntityPlayer player) {
        super(player.inventory, owner);
        this.owner = owner;

        this.bindPlayerInventory(getInventoryPlayer(), 40, 195 - /* height of player inventory */ 72);

        IItemHandlerModifiable config = this.owner.getConfigInventory().asGUIAccess();
        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 6; x++) {
                this.addSlotToContainer(new SlotFake(config,
                        y * 6 + x, 12 + 18 * x, 18 * y + 9));
            }
        }

        IItemHandlerModifiable internal = this.owner.getInternalInventory().asGUIAccess();
        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 6; x++) {
                this.addSlotToContainer(new SlotDisabled(internal,
                        y * 6 + x, 93 + 9 + 18 * x + 35, 18 * y + 9));
            }
        }
    }

    public MEItemInputBus getOwner() {
        return owner;
    }
}
