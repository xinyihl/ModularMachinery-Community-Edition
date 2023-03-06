package github.kasuminova.mmce.common.concurrent;

import hellfirepvp.modularmachinery.ModularMachinery;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

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
    private final ConcurrentLinkedQueue<Action> asyncActions = new ConcurrentLinkedQueue<>();
    public Thread serverThread = null;

    public void init() {
        poolExecutorThread.setName("MMCE-PoolExecutor");
        poolExecutorThread.start();
    }

    @SubscribeEvent
    public void onWorldTick(final TickEvent.WorldTickEvent event) {
        if (event.side == Side.CLIENT) {
            return;
        }

        serverThread = Thread.currentThread();

        long executed = executeActions();
        if (executed > 0) {
            totalExecuted += executed;
            executedCount++;
        }
    }

    /**
     * 正式执行队列内的所有操作。
     *
     * @return 已执行的数量
     */
    public long executeActions() {
        if (asyncActions.isEmpty() && executors.isEmpty()) {
            return 0;
        }

        if (!asyncActions.isEmpty()) {
            executeAsyncActions();
        }

        int executed = 0;
        long time = System.nanoTime() / 1000;

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

        //Empty Check
        if (!asyncActions.isEmpty() || !executors.isEmpty()) {
            executed += executeActions();
        }

        totalUsedTime += System.nanoTime() / 1000 - time;
        return executed;
    }

    /**
     * <p>添加一个异步操作引用，这个操作必定在本 Tick 结束前执行完毕。</p>
     *
     * @param action 要执行的异步任务
     */
    public void addAsyncTask(final Action action) {
        asyncActions.add(action);
        if (poolExecutorThread.getState() == Thread.State.TIMED_WAITING) {
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
        private int emptyQueueCounter = 0;

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                if (asyncActions.isEmpty()) {
                    //If is no actions, park 250μs + delayedTime(queueEmptyCount * 100μs, max: 25ms).
                    LockSupport.parkNanos(
                            250 * 1000 + Math.min(emptyQueueCounter * 100 * 1000, 25 * 1000 * 1000)
                    );
                    emptyQueueCounter++;
                } else {
                    emptyQueueCounter = 0;
                    executeAsyncActions();
                }
            }
        }
    }
}
