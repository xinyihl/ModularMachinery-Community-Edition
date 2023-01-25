package github.kasuminova.mmce.concurrent;

import hellfirepvp.modularmachinery.ModularMachinery;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

public class TaskExecutorThread implements Runnable {
    private static final AtomicInteger THREAD_COUNT = new AtomicInteger(0);
    public final Thread executorThread = new Thread(this);
    public final TaskExecutor manager;
    public final AtomicInteger completedThreadCount;
    public final AtomicReference<ConcurrentLinkedQueue<Action>> actions = new AtomicReference<>(null);
    public long usedTime = 0;
    public long executed = 0;

    public TaskExecutorThread(TaskExecutor manager, AtomicInteger completedThreadCount) {
        this.manager = manager;
        this.completedThreadCount = completedThreadCount;
    }

    public void start() {
        executorThread.setName("MMCE-TaskExecutor-" + THREAD_COUNT.getAndIncrement());
        executorThread.start();
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            LockSupport.park();
            executed = 0;

            ConcurrentLinkedQueue<Action> actions = this.actions.get();
            if (actions != null) {
                long time = System.nanoTime();

                Action action;
                while ((action = actions.poll()) != null) {
                    action.doAction();
                    executed++;
                }

                usedTime = (System.nanoTime() - time) / 1000;
                if (usedTime > 1000 * 50) {
                    ModularMachinery.log.warn(String.format("ParallelAction takes too long to execute. (%sms)", usedTime / 1000));
                }

                manager.onThreadFinished();
            }
        }
    }
}
