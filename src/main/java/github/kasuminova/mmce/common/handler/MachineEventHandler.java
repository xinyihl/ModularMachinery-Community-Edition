package github.kasuminova.mmce.common.handler;

import github.kasuminova.mmce.common.capability.CapabilityUpgrade;
import github.kasuminova.mmce.common.capability.CapabilityUpgradeProvider;
import github.kasuminova.mmce.common.event.machine.MachineEvent;
import github.kasuminova.mmce.common.upgrade.MachineUpgrade;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.helper.UpgradeEventHandlerCT;
import hellfirepvp.modularmachinery.common.tiles.TileUpgradeBus;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import hellfirepvp.modularmachinery.common.util.MiscUtils;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

@SuppressWarnings("MethodMayBeStatic")
public class MachineEventHandler {

    @SubscribeEvent
    public void onMachineEvent(MachineEvent event) {
        TileMultiblockMachineController controller = event.getController();

        for (MachineUpgrade upgrade : controller.getFoundUpgrades().values()) {
            List<UpgradeEventHandlerCT> processors = upgrade.getEventHandlers(event.getClass());
            if (processors.isEmpty()) {
                continue;
            }

            TileUpgradeBus parentBus = upgrade.getParentBus();
            if (parentBus == null) {
                ModularMachinery.log.warn("Found a null UpgradeBus at controller " + MiscUtils.posToString(controller.getPos()));
                continue;
            }

            TileUpgradeBus.UpgradeBusProvider provider = parentBus.provideComponent();
            upgrade.readNBT(provider.getUpgradeCustomData(upgrade));

            for (final UpgradeEventHandlerCT handler : processors) {
                handler.handle(event, upgrade);
                if (event.isCanceled()) {
                    break;
                }
            }

            provider.setUpgradeCustomData(upgrade, upgrade.writeNBT());

            if (event.isCanceled()) {
                break;
            }
        }
    }

    @SubscribeEvent
    public void onAttachCapabilities(AttachCapabilitiesEvent<ItemStack> event) {
        Item item = event.getObject().getItem();

        if (!MachineUpgrade.supportsUpgrade(item)) {
            return;
        }

        CapabilityUpgradeProvider provider = new CapabilityUpgradeProvider();
        CapabilityUpgrade upgrade = provider.getUpgrade();

        List<MachineUpgrade> upgradeList = MachineUpgrade.getItemUpgradeList(item);
        if (upgradeList != null) {
            upgrade.getUpgrades().addAll(upgradeList);
        }

        event.addCapability(CapabilityUpgrade.CAPABILITY_NAME, provider);
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onItemTooltip(ItemTooltipEvent event) {
        CapabilityUpgrade upgrade = event.getItemStack().getCapability(CapabilityUpgrade.MACHINE_UPGRADE_CAPABILITY, null);
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
