package github.kasuminova.mmce.common.container;

import appeng.container.AEBaseContainer;
import appeng.container.slot.SlotRestrictedInput;
import appeng.tile.inventory.AppEngInternalInventory;
import github.kasuminova.mmce.common.tile.MEPatternProvider;
import net.minecraft.entity.player.EntityPlayer;

public class ContainerMEPatternProvider extends AEBaseContainer {

    private final MEPatternProvider owner;

    public ContainerMEPatternProvider(final MEPatternProvider owner, final EntityPlayer player) {
        super(player.inventory, owner);
        this.owner = owner;

        this.bindPlayerInventory(getInventoryPlayer(), 0, 114);

        AppEngInternalInventory patterns = owner.getPatterns();
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlotToContainer(new SlotRestrictedInput(SlotRestrictedInput.PlacableItemType.ENCODED_PATTERN, patterns, (row * 9) + col, 8 + (col * 18), 28 + (row * 18), getInventoryPlayer()));
            }
        }
    }

    public MEPatternProvider getOwner() {
        return owner;
    }

}
