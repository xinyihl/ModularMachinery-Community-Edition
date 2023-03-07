package github.kasuminova.mmce.common.concurrent;

import hellfirepvp.modularmachinery.ModularMachinery;

import java.util.concurrent.RecursiveAction;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ActionExecutor extends RecursiveAction {
    public final Action action;
    public int usedTime = 0;

    public ActionExecutor(Action action) {
        this.action = action;
    }

    @Override
    protected void compute() {
        long start = System.nanoTime() / 1000;

        RecursiveAction computeAction = new RecursiveAction() {
            @Override
            protected void compute() {
                action.doAction();
            }
        };
        computeAction.fork();
        try {
            computeAction.get(50, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            ModularMachinery.log.warn("[Modular Machinery] Parallel action execute timeout for 50ms.");
            computeAction.cancel(true);
        } catch (Exception e) {
            ModularMachinery.log.warn(e);
        }

        usedTime = (int) (System.nanoTime() / 1000 - start);
    }
}