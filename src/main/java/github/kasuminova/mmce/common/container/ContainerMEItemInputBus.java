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

        this.bindPlayerInventory(getInventoryPlayer(), 0, 141);

        IItemHandlerModifiable config = this.owner.getConfigInventory().asGUIAccess();
        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 6; x++) {
                this.addSlotToContainer(new SlotFake(config,
                        y * 6 + x, 8 + 18 * x, 18 * y + 34));
            }
        }

        IItemHandlerModifiable internal = this.owner.getInternalInventory().asGUIAccess();
        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 6; x++) {
                this.addSlotToContainer(new SlotDisabled(internal,
                        y * 6 + x, 93 + 9 + 18 * x + 40, 18 * y + 34));
            }
        }
    }

    public MEItemInputBus getOwner() {
        return owner;
    }
}
