package hellfirepvp.modularmachinery.common.integration.crafttweaker.helper;

import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.item.IItemStack;
import github.kasuminova.mmce.common.helper.IMachineController;
import stanhebben.zenscript.annotations.ZenClass;

@ZenRegister
@ZenClass("mods.modularmachinery.AdvancedItemNBTChecker")
@FunctionalInterface
public interface AdvancedItemNBTCheckerCT {
    boolean isMatch(IMachineController controller, IItemStack stack);
}
