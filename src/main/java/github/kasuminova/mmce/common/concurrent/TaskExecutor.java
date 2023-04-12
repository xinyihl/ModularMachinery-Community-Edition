package github.kasuminova.mmce.common.concurrent;

import hellfirepvp.modularmachinery.common.tiles.base.TileEntitySynchronized;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ForkJoinPool;

/**
 * 一个简单的单 Tick 并发执行器
 * 注意：如果提交了一个会修改世界的引用，请使用锁或同步关键字修饰会修改世界的部分代码操作
 */
public class TaskExecutor {
    public static final int MAX_THREAD_SCHEDULE_PER_TICK = 4;
    public static final int THREAD_COUNT = 4;
    public static final ForkJoinPool FORK_JOIN_POOL = new ForkJoinPool(THREAD_COUNT);
    public static long totalExecuted = 0;
    public static long taskUsedTime = 0;
    public static long totalUsedTime = 0;
    public static long executedCount = 0;
    private final ConcurrentLinkedQueue<ActionExecutor> executors = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Action> mainThreadActions = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Action> collectedActions = new ConcurrentLinkedQueue<>();
    private final Set<TileEntitySynchronized> requireUpdateTEList = new HashSet<>();
    private volatile int maximumTaskMerge = 1;

    public void init() {
    }

    @SubscribeEvent
    public void onWorldTick(final TickEvent.WorldTickEvent event) {
        if (event.side == Side.CLIENT) {
            return;
        }

        int executed = executeActions();
        if (executed > 0) {
            maximumTaskMerge = Math.max(1, executed / MAX_THREAD_SCHEDULE_PER_TICK);
            totalExecuted += executed;
            executedCount++;
        }
    }

    /**
     * 正式执行队列内的所有操作。
     *
     * @return 已执行的数量
     */
    public int executeActions() {
        if (executors.isEmpty() && collectedActions.isEmpty()) {
            return 0;
        }

        int executed = 0;
        long time = System.nanoTime() / 1000;

        submitActionExecutor();
        executed += awaitActionExecutor();

        Action action;
        while ((action = mainThreadActions.poll()) != null) {
            action.doAction();
            executed++;
        }

        for (TileEntitySynchronized te : requireUpdateTEList) {
            te.markForUpdate();
        }
        requireUpdateTEList.clear();

        //Empty Check
        if (!executors.isEmpty()) {
            executed += executeActions();
        }

        totalUsedTime += System.nanoTime() / 1000 - time;
        return executed;
    }

    private int awaitActionExecutor() {
        int executed = 0;
        ActionExecutor actionExecutor;
        while ((actionExecutor = executors.poll()) != null) {
            actionExecutor.join();
            taskUsedTime += actionExecutor.usedTime;
            executed++;
        }
        return executed;
    }

    /**
     * <p>添加一个并行异步操作引用，这个操作必定在本 Tick 结束前执行完毕。</p>
     *
     * <p>由于每个任务的运行速度极快，导致线程池中的线程被频繁调度消耗性能，因此任务不会被立即执行，它会被收集到一个队列中。</p>
     * <p>当队列大小达到一定的阈值时，再提交给线程池执行。</p>
     *
     * @param action 要执行的异步任务
     */
    public void addParallelAsyncTask(final Action action) {
        collectedActions.offer(action);
        if (collectedActions.size() >= maximumTaskMerge) {
            submitActionExecutor();
        }
    }

    /**
     * <p>执行合并任务队列中的任务。</p>
     */
    private void submitActionExecutor() {
        if (collectedActions.isEmpty()) {
            return;
        }
        executors.offer((ActionExecutor) FORK_JOIN_POOL.submit(new ActionExecutor(collectedActions)));
        collectedActions.clear();
    }

    /**
     * <p>添加一个同步操作引用，这个操作必定会在异步操作完成后在***主线程***执行。</p>
     *
     * @param action 要执行的同步任务
     */
    public void addSyncTask(final Action action) {
        mainThreadActions.offer(action);
    }

    public synchronized void addTEUpdateTask(final TileEntitySynchronized te) {
        requireUpdateTEList.add(te);
    }
}
