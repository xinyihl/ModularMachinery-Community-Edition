package hellfirepvp.modularmachinery.common.integration.crafttweaker.helper;

import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.item.IItemStack;
import stanhebben.zenscript.annotations.ZenClass;

@ZenRegister
@ZenClass("mods.modularmachinery.AdvancedItemNBTChecker")
@FunctionalInterface
public interface AdvancedItemNBTChecker {
    boolean isMatch(IItemStack stack);
}
