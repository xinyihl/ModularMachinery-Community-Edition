package github.kasuminova.mmce.common.concurrent;

import java.util.concurrent.RecursiveAction;

public class ActionExecutor extends RecursiveAction {
    public final Action action;
    public int usedTime = 0;

    public ActionExecutor(Action action) {
        this.action = action;
    }

    @Override
    protected void compute() {
        long start = System.nanoTime() / 1000;
        action.doAction();
        usedTime = (int) (System.nanoTime() / 1000 - start);
    }
}