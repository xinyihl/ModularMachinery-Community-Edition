package github.kasuminova.mmce.common.concurrent;

import hellfirepvp.modularmachinery.ModularMachinery;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.locks.LockSupport;

/**
 * 一个简单的单 Tick 并发执行器
 * 注意：如果提交了一个会修改世界的引用，请使用锁或同步关键字修饰会修改世界的部分代码操作
 */
public class TaskExecutor {
    public static final int THREAD_COUNT = Math.max(Runtime.getRuntime().availableProcessors() / 2, 4);
    public static final ForkJoinPool FORK_JOIN_POOL = new ForkJoinPool(THREAD_COUNT);
    public static long totalExecuted = 0;
    public static long taskUsedTime = 0;
    public static long totalUsedTime = 0;
    public static long executedCount = 0;
    private final TaskExecutorThread[] executors = new TaskExecutorThread[THREAD_COUNT];
    private final ConcurrentLinkedQueue<Action> preActions = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Action> postActions = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Action> mainThreadActions = new ConcurrentLinkedQueue<>();
    public volatile ConcurrentLinkedQueue<Action> activeActions = null;
    public volatile int completedThreadCounter = 0;
    public Thread serverThread = null;

    public void init() {
        for (int i = 0; i < THREAD_COUNT; i++) {
            (executors[i] = new TaskExecutorThread(this)).start();
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            activeActions = null;
            for (TaskExecutorThread executor : executors) {
                Thread executorThread = executor.executorThread;
                executorThread.interrupt();
                LockSupport.unpark(executorThread);
            }
        }));
    }

    @SubscribeEvent
    public void onTick(final TickEvent.ServerTickEvent event) {
        serverThread = Thread.currentThread();

        switch (event.phase) {
            case START: {
                activeActions = preActions;
                break;
            }
            case END: {
                activeActions = postActions;
                break;
            }
        }

        if (activeActions != null) {
            long executed = executeActions();
            if (executed > 0) {
                totalExecuted += executed;
                executedCount++;
            }

            activeActions = null;
        }
    }

    /**
     * 正式执行队列内的所有操作。
     *
     * @return 已执行的数量
     */
    public long executeActions() {
        if (activeActions.isEmpty()) return 0;

        int executed = 0;
        completedThreadCounter = Math.max(executors.length - activeActions.size(), 0);
        for (int i = 0; i < Math.min(activeActions.size(), executors.length); i++) {
            Thread executorThread = executors[i].executorThread;
            LockSupport.unpark(executorThread);
        }

        long time = System.nanoTime() / 1000;
        await();

        for (TaskExecutorThread executor : executors) {
            taskUsedTime += executor.usedTime;
            executed += executor.executed;
        }

        completedThreadCounter = 0;
        totalUsedTime += System.nanoTime() / 1000 - time;

        Action action;
        while ((action = mainThreadActions.poll()) != null) {
            action.doAction();
        }

        totalUsedTime += System.nanoTime() / 1000 - time;

        return executed;
    }

    /**
     * 添加一个接口引用，这个引用必定会在下一个 tick 开始时完成
     *
     * @param action 引用
     */
    public void addPreTickTask(final Action action) {
        preActions.add(action);
    }

    /**
     * 添加一个接口引用，这个引用必定会在该 tick 结束前完成
     *
     * @param action 引用
     */
    public void addPostTickTask(final Action action) {
        postActions.add(action);
    }

    /**
     * <p>添加一个接口引用，这个引用会在并行操作完成后执行。</p>
     * <p>通常用来执行一些必须在主线程操作的内容。</p>
     *
     * @param action 要执行的任务队列
     */
    public void addMainThreadTask(final Action action) {
        mainThreadActions.add(action);
    }

    /**
     * 等待任务线程执行完毕，最多超时等待 250ms，超过时间则取消等待并打印警告信息。
     */
    private void await() {
        long time = System.currentTimeMillis();
        LockSupport.parkNanos(1000 * 1000 * 250);
        if (System.currentTimeMillis() - time > 250) {
            ModularMachinery.log.warn(
                    "[Modular Machinery] Parallel task execute timeout for 250ms ({} Threads Completed / {} Thread Total, {} Tasks Left).",
                    completedThreadCounter, executors, activeActions.size()
            );

            if (THREAD_COUNT > completedThreadCounter) {
                ModularMachinery.log.warn("[Modular Machinery] Some thread are not execute complete, printing stacktrace...");
                printThreadStackTrace();
            }
        }
    }

    /**
     * 打印未完成的线程堆栈信息
     */
    private void printThreadStackTrace() {
        for (TaskExecutorThread executor : executors) {
            if (executor.isRunning) {
                Thread executorThread = executor.executorThread;

                StringBuilder stackTrace = new StringBuilder(100).append(String.format(
                        "ThreadName: %s, State: %s, StackTrace: \n",
                        executorThread.getName(), executorThread.getState()));

                StackTraceElement[] threadStackTrace = executorThread.getStackTrace();
                for (int i = 0; i < threadStackTrace.length; i++) {
                    if (i == 0) {
                        stackTrace.append(threadStackTrace[i]).append("\n");
                    } else {
                        stackTrace.append("    ").append(threadStackTrace[i]).append("\n");
                    }
                }
                ModularMachinery.log.warn(stackTrace);
            }
        }
    }

    /**
     * <p>当一个线程完成执行时，调用此同步方法。</p>
     * <p>当所有线程完成后，使服务器线程继续执行逻辑。</p>
     */
    public void onThreadFinished() {
        synchronized (this) {
            completedThreadCounter++;
        }

        if (completedThreadCounter >= THREAD_COUNT) {
            LockSupport.unpark(serverThread);
        }
    }
}
