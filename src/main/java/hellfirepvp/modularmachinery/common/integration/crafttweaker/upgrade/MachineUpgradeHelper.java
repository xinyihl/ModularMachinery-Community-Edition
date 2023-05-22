package hellfirepvp.modularmachinery.common.integration.crafttweaker.upgrade;

import crafttweaker.CraftTweakerAPI;
import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.item.IItemStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import github.kasuminova.mmce.common.capability.CapabilityUpgrade;
import github.kasuminova.mmce.common.upgrade.MachineUpgrade;
import github.kasuminova.mmce.common.upgrade.SimpleDynamicMachineUpgrade;
import github.kasuminova.mmce.common.upgrade.SimpleMachineUpgrade;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

import javax.annotation.Nullable;

@ZenRegister
@ZenClass("mods.modularmachinery.MachineUpgradeHelper")
public class MachineUpgradeHelper {

    @ZenMethod
    public static void registerSupportedItem(IItemStack itemStack) {
        Item item = CraftTweakerMC.getItemStack(itemStack).getItem();
        if (item != Items.AIR) {
            MachineUpgrade.addSupportedItem(item);
        }
    }

    @ZenMethod
    public static void addFixedUpgrade(IItemStack itemStack, String upgradeName) {
        Item item = CraftTweakerMC.getItemStack(itemStack).getItem();
        if (item == Items.AIR) {
            return;
        }
        MachineUpgrade upgrade = MachineUpgrade.getUpgrade(upgradeName);
        if (upgrade == null) {
            CraftTweakerAPI.logError("[ModularMachinery] Cloud not find MachineUpgrade " + upgradeName + '!');
            return;
        }
        MachineUpgrade.addFixedUpgrade(item, upgrade);
    }

    @ZenMethod
    public static IItemStack addUpgradeToIItemStack(IItemStack iItemStack, MachineUpgrade upgrade) {
        ItemStack itemStack = CraftTweakerMC.getItemStack(iItemStack);
        if (!MachineUpgrade.supportsUpgrade(itemStack.getItem())) {
            return iItemStack;
        }
        CapabilityUpgrade capability = itemStack.getCapability(CapabilityUpgrade.MACHINE_UPGRADE_CAPABILITY, null);
        if (capability == null) {
            return iItemStack;
        }
        capability.getUpgrades().add(upgrade);
        return CraftTweakerMC.getIItemStack(itemStack);
    }

    @Nullable
    @ZenMethod
    public static MachineUpgrade getUpgrade(String upgradeName) {
        return MachineUpgrade.getUpgrade(upgradeName);
    }

    @ZenMethod
    public static SimpleDynamicMachineUpgrade castToSimpleDynamicMachineUpgrade(MachineUpgrade upgrade) {
        return upgrade instanceof SimpleDynamicMachineUpgrade ? (SimpleDynamicMachineUpgrade) upgrade : null;
    }

    @ZenMethod
    public static SimpleMachineUpgrade castToSimpleMachineUpgrade(MachineUpgrade upgrade) {
        return upgrade instanceof SimpleMachineUpgrade ? (SimpleMachineUpgrade) upgrade : null;
    }
}
