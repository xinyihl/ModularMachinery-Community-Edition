package github.kasuminova.mmce.concurrent;

import hellfirepvp.modularmachinery.ModularMachinery;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

/**
 * 一个简单的单 Tick 并发执行器
 * 注意：如果提交了一个会修改世界的引用，请使用锁或同步关键字修饰会修改世界的部分代码操作
 */
public class TaskExecutor {
    public static final int AVAILABLE_PROCESSORS = Runtime.getRuntime().availableProcessors() / 2;
    public static final int THREAD_COUNT = Math.max(AVAILABLE_PROCESSORS, 4);
    public static long totalSubmitted = 0;
    public static long totalExecuted = 0;
    public static long taskUsedTime = 0;
    public static long totalUsedTime = 0;
    public static long tickExisted = 0;
    public final AtomicInteger completedThreadCount = new AtomicInteger(0);
    private final ArrayList<TaskExecutorThread> executors = new ArrayList<>((int) (THREAD_COUNT * 1.5));
    private final ConcurrentLinkedQueue<ParallelAction> preParallelActions = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<ParallelAction> postParallelActions = new ConcurrentLinkedQueue<>();
    public Thread serverThread = null;

    public TaskExecutor() {
        for (int i = 0; i < THREAD_COUNT; i++) {
            TaskExecutorThread taskExecutorThread = new TaskExecutorThread(this, completedThreadCount);
            taskExecutorThread.start();
            executors.add(taskExecutorThread);
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            for (TaskExecutorThread executor : executors) {
                executor.actions.set(null);
                LockSupport.unpark(executor.executorThread);
                executor.executorThread.interrupt();
            }
        }));
    }

    @SubscribeEvent
    public void onTick(final TickEvent.ServerTickEvent event) {
        serverThread = Thread.currentThread();

        switch (event.phase) {
            case START: {
                executeActions(preParallelActions);
                break;
            }
            case END: {
                executeActions(postParallelActions);
                tickExisted++;
                break;
            }
        }
    }

    public void executeActions(final ConcurrentLinkedQueue<ParallelAction> actions) {
        if (actions.isEmpty()) return;

        long time = System.nanoTime() / 1000;

        for (TaskExecutorThread executor : executors) {
            executor.actions.set(actions);
            LockSupport.unpark(executor.executorThread);
        }

        await(actions);

        for (TaskExecutorThread executor : executors) {
            taskUsedTime += executor.usedTime;
            totalExecuted += executor.executed;
        }

        completedThreadCount.set(0);
        totalUsedTime += System.nanoTime() / 1000 - time;
    }

    /**
     * 添加一个接口引用，这个引用必定会在下一个 tick 开始时完成
     *
     * @param action 引用
     */
    public void addPreTickTask(final ParallelAction action) {
        totalSubmitted++;
        preParallelActions.add(action);
    }

    /**
     * 添加一个接口引用，这个引用必定会在该一个 tick 即将结束时前完成
     *
     * @param action 引用
     */
    public void addPostTickTask(final ParallelAction action) {
        totalSubmitted++;
        postParallelActions.add(action);
    }

    public void await(final ConcurrentLinkedQueue<ParallelAction> actions) {
        long time = System.currentTimeMillis();
        //Timeout 1000ms
        LockSupport.parkNanos(1000 * 1000 * 250);
        if (System.currentTimeMillis() - time > 250) {
            ModularMachinery.log.warn(
                    String.format(
                            "[Modular Machinery] Parallel task execute timeout for 250ms (%s Threads Completed / %s Thread Total, %s Tasks Left).",
                            completedThreadCount.get(),
                            THREAD_COUNT,
                            actions.size())
            );
        }
    }

    public void onThreadFinished() {
        int completed = completedThreadCount.incrementAndGet();
        if (completed >= THREAD_COUNT) {
            LockSupport.unpark(serverThread);
        }
    }
}
