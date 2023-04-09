package github.kasuminova.mmce.common.concurrent;

import hellfirepvp.modularmachinery.ModularMachinery;
import io.netty.util.internal.ThrowableUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ActionExecutor extends RecursiveAction {
    public final List<Action> actionList = new ArrayList<>();
    public int usedTime = 0;

    public ActionExecutor(Collection<Action> actionList) {
        this.actionList.addAll(actionList);
    }

    @Override
    protected void compute() {
        long start = System.nanoTime() / 1000;

        ForkJoinTask<Void> computeAction = new RecursiveAction() {
            @Override
            protected void compute() {
                try {
                    for (Action action : actionList) {
                        action.doAction();
                    }
                } catch (Exception e) {
                    ModularMachinery.log.warn("An error occurred during asynchronous task execution!");
                    ModularMachinery.log.warn(ThrowableUtil.stackTraceToString(e));
                }
            }
        }.fork();

        try {
            computeAction.get(50, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            ModularMachinery.log.warn("[Modular Machinery] Parallel action execute timeout for 50ms.");
            computeAction.cancel(true); // May not work.
        } catch (Exception e) {
            ModularMachinery.log.warn(e);
        }

        usedTime = (int) (System.nanoTime() / 1000 - start);
    }
}