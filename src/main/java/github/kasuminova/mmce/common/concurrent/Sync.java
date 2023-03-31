package github.kasuminova.mmce.common.concurrent;

import crafttweaker.annotations.ZenRegister;
import hellfirepvp.modularmachinery.ModularMachinery;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenRegister
@ZenClass("mods.modularmachinery.Sync")
public class Sync {
    /**
     * <p>全局同步方法，用来进行方块更新、同步等非线程安全操作。</p>
     * <p>Global synchronization method.</p>
     * <p>For non-thread-safe operations such as block update and synchronization.</p>
     */
    public static synchronized void doSyncAction(Action action) {
        action.doAction();
    }

    @ZenMethod
    public static void addSyncTask(Action action) {
        ModularMachinery.EXECUTE_MANAGER.addSyncTask(action);
    }
}
