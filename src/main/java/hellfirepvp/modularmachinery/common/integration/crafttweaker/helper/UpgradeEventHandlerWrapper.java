package hellfirepvp.modularmachinery.common.integration.crafttweaker.helper;

import crafttweaker.annotations.ZenRegister;
import github.kasuminova.mmce.common.event.machine.MachineEvent;
import github.kasuminova.mmce.common.upgrade.MachineUpgrade;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenGetter;

@ZenRegister
@ZenClass("mods.modularmachinery.UpgradeEventHandlerWrapper")
public class UpgradeEventHandlerWrapper {
    private final MachineEvent event;
    private final MachineUpgrade upgrade;

    public UpgradeEventHandlerWrapper(final MachineEvent event, final MachineUpgrade upgrade) {
        this.event = event;
        this.upgrade = upgrade;
    }

    @ZenGetter("event")
    public MachineEvent getEvent() {
        return event;
    }

    @ZenGetter("upgrade")
    public MachineUpgrade getUpgrade() {
        return upgrade;
    }
}
