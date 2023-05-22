package hellfirepvp.modularmachinery.common.integration.crafttweaker.helper;

import crafttweaker.annotations.ZenRegister;
import github.kasuminova.mmce.common.event.machine.MachineEvent;
import github.kasuminova.mmce.common.upgrade.MachineUpgrade;
import stanhebben.zenscript.annotations.ZenClass;

@ZenRegister
@FunctionalInterface
@ZenClass("mods.modularmachinery.UpgradeEventHandler")
public interface UpgradeEventHandlerCT {
    void handle(MachineEvent event, MachineUpgrade upgrade);
}
