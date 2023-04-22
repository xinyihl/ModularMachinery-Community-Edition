package github.kasuminova.mmce.common.concurrent;

public class ActionExecutor implements Runnable, Comparable<ActionExecutor> {
    public final Action action;
    public final int priority;
    public int usedTime = 0;

    public ActionExecutor(Action action) {
        this(action, 0);
    }

    public ActionExecutor(Action action, int priority) {
        this.action = action;
        this.priority = priority;
    }

    public void run() {
        long start = System.nanoTime() / 1000;

        action.doAction();

        usedTime = (int) (System.nanoTime() / 1000 - start);
    }

    @Override
    public int compareTo(ActionExecutor o) {
        return priority - o.priority;
    }
}