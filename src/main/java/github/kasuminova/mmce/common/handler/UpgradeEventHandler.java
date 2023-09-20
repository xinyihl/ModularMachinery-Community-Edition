package github.kasuminova.mmce.common.handler;

import github.kasuminova.mmce.common.capability.CapabilityUpgrade;
import github.kasuminova.mmce.common.capability.CapabilityUpgradeProvider;
import github.kasuminova.mmce.common.upgrade.MachineUpgrade;
import github.kasuminova.mmce.common.upgrade.registry.RegistryUpgrade;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

@SuppressWarnings("MethodMayBeStatic")
public class UpgradeEventHandler {
    @SubscribeEvent
    public void onAttachCapabilities(AttachCapabilitiesEvent<ItemStack> event) {
        ItemStack stack = event.getObject();

        if (!RegistryUpgrade.supportsUpgrade(stack)) {
            return;
        }

        CapabilityUpgradeProvider provider = new CapabilityUpgradeProvider();
        CapabilityUpgrade upgrade = provider.getUpgrade();

        List<MachineUpgrade> upgradeList = RegistryUpgrade.getItemUpgradeList(stack);
        if (upgradeList != null) {
            upgradeList.forEach(u -> upgrade.getUpgrades().add(u.copy(stack)));
        }

        event.addCapability(CapabilityUpgrade.CAPABILITY_NAME, provider);
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onUpgradeItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!RegistryUpgrade.supportsUpgrade(stack)) {
            return;
        }
        CapabilityUpgrade upgrade = stack.getCapability(CapabilityUpgrade.MACHINE_UPGRADE_CAPABILITY, null);
        if (upgrade == null) {
            return;
        }
        List<MachineUpgrade> upgrades = upgrade.getUpgrades();
        List<String> toolTip = event.getToolTip();
        upgrades.forEach(machineUpgrade -> {
            toolTip.add(machineUpgrade.getType().getLocalizedName());
            toolTip.addAll(machineUpgrade.getDescriptions());
        });
    }
}
