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
    public final PoolExecutor poolExecutor = new PoolExecutor();
    public final Thread poolExecutorThread = new Thread(poolExecutor);
    private final ConcurrentLinkedQueue<ActionExecutor> executors = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Action> preActions = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Action> postActions = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Action> asyncActions = new ConcurrentLinkedQueue<>();
    public volatile ConcurrentLinkedQueue<Action> activeActions = null;
    public Thread serverThread = null;

    public void init() {
        poolExecutorThread.setName("MMCE-PoolExecutor");
        poolExecutorThread.start();
    }

    @SubscribeEvent
    public void onTick(final TickEvent.ServerTickEvent event) {
        serverThread = Thread.currentThread();

        switch (event.phase) {
            case START:
                activeActions = preActions;
                break;
            case END:
                activeActions = postActions;
                break;
            default:
                activeActions = null;
                return;
        }

        if (activeActions != null) {
            long executed = executeActions();
            if (executed > 0) {
                totalExecuted += executed;
                executedCount++;
            }
        }
    }

    /**
     * 正式执行队列内的所有操作。
     *
     * @return 已执行的数量
     */
    public long executeActions() {
        if (activeActions.isEmpty() && executors.isEmpty()) {
            return 0;
        }

        if (asyncActions.isEmpty() && !poolExecutor.isRunning) {
            executeAsyncActions();
        }

        int executed = 0;
        long time = System.nanoTime() / 1000;

        Action action;
        while ((action = activeActions.poll()) != null) {
            ActionExecutor executorAction = new ActionExecutor(action);
            executors.add(executorAction);
            FORK_JOIN_POOL.execute(executorAction);
        }

        ActionExecutor actionExecutor;
        while ((actionExecutor = executors.poll()) != null) {
            actionExecutor.join();
            taskUsedTime += actionExecutor.usedTime;
            executed++;
        }

        if ((System.nanoTime() / 1000) - (50 * 1000 * 1000) > time) {
            ModularMachinery.log.warn(
                    "[Modular Machinery] Parallel action execute timeout for 50ms."
            );
        }

        while ((action = asyncActions.poll()) != null) {
            action.doAction();
        }

        //Empty Check
        if (!activeActions.isEmpty() || !executors.isEmpty()) {
            executed += executeActions();
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
     * <p>添加一个异步操作引用，这个操作必定在本 Tick 结束前执行完毕。</p>
     *
     * @param action 要执行的异步任务
     */
    public void addAsyncTask(final Action action) {
        asyncActions.add(action);
        if (!poolExecutor.isRunning) {
            LockSupport.unpark(poolExecutorThread);
        }
    }

    private void executeAsyncActions() {
        Action action;
        while ((action = asyncActions.poll()) != null) {
            ActionExecutor executor = new ActionExecutor(action);
            executors.add(executor);
            FORK_JOIN_POOL.execute(executor);
        }
    }

    private class PoolExecutor implements Runnable {
        private volatile boolean isRunning = false;

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                if (asyncActions.isEmpty()) {
                    LockSupport.park();
                }
                isRunning = true;
                executeAsyncActions();
                isRunning = false;
            }
        }
    }
}
