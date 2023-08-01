package github.kasuminova.mmce.common.integration;

import appeng.api.config.Upgrades;
import hellfirepvp.modularmachinery.common.lib.ItemsMM;
import net.minecraft.item.ItemStack;

public class ModIntegrationAE2 {
    public static void registerUpgrade() {
        Upgrades.CAPACITY.registerItem(new ItemStack(ItemsMM.meFluidOutputBus), 5);
        Upgrades.CAPACITY.registerItem(new ItemStack(ItemsMM.meFluidinputBus), 5);
    }
}
