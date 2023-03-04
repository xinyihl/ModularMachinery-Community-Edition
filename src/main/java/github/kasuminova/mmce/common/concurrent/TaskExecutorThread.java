package github.kasuminova.mmce.common.concurrent;

import hellfirepvp.modularmachinery.ModularMachinery;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

public class TaskExecutorThread implements Runnable {
    private static final AtomicInteger THREAD_COUNT = new AtomicInteger(0);
    public final Thread executorThread = new Thread(this);
    public final TaskExecutor manager;
    public boolean isRunning = false;
    public long usedTime = 0;
    public int executed = 0;

    public TaskExecutorThread(TaskExecutor manager) {
        this.manager = manager;
    }

    public TaskExecutorThread start() {
        executorThread.setName("MMCE-TaskExecutor-" + THREAD_COUNT.getAndIncrement());
        executorThread.start();
        return this;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            executed = 0;

            ConcurrentLinkedQueue<Action> actions = this.manager.activeActions;
            if (actions != null) {
                isRunning = true;

                long time = System.nanoTime();

                Action action;
                while ((action = actions.poll()) != null) {
                    action.doAction();
                    executed++;
                }

                usedTime = (System.nanoTime() - time) / 1000;
                if (usedTime > 1000 * 50) {
                    ModularMachinery.log.warn("ExecutorThread takes too long to execute. ({}ms)", usedTime / 1000);
                }

                manager.onThreadFinished();
            }

            isRunning = false;
            LockSupport.park();
        }
    }
}