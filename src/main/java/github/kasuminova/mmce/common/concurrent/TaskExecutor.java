package github.kasuminova.mmce.common.concurrent;

import github.kasuminova.mmce.common.util.concurrent.Action;
import github.kasuminova.mmce.common.util.concurrent.ActionExecutor;
import github.kasuminova.mmce.common.util.concurrent.CustomForkJoinWorkerThreadFactory;
import github.kasuminova.mmce.common.util.concurrent.CustomThreadFactory;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.tiles.base.TileEntitySynchronized;
import io.netty.util.internal.ThrowableUtil;
import io.netty.util.internal.shaded.org.jctools.queues.atomic.MpscLinkedAtomicQueue;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.concurrent.*;
import java.util.concurrent.locks.LockSupport;

/**
 * 一个简单的单 Tick 并发执行器
 * 注意：如果提交了一个会修改世界的引用，请使用锁或同步关键字修饰会修改世界的部分代码操作
 */
public class TaskExecutor {
    public static final int THREAD_COUNT = Math.min(Math.max(Runtime.getRuntime().availableProcessors() / 4, 4), 8);

    private static final ThreadPoolExecutor THREAD_POOL = new ThreadPoolExecutor(4, THREAD_COUNT,
            5000, TimeUnit.MILLISECONDS,
            new PriorityBlockingQueue<>(),
            new CustomThreadFactory("MMCE-TaskExecutor-%s"));

    private static final ForkJoinPool FORK_JOIN_POOL = new ForkJoinPool(THREAD_COUNT,
            new CustomForkJoinWorkerThreadFactory("MMCE-ForkJoinPool-worker-%s"),
            null, true);

    public static long totalExecuted = 0;
    public static long taskUsedTime = 0;
    public static long totalUsedTime = 0;
    public static long executedCount = 0;

    public static long tickExisted = 0;

    private final MpscLinkedAtomicQueue<ActionExecutor> submitted = new MpscLinkedAtomicQueue<>();

    private final MpscLinkedAtomicQueue<ActionExecutor> executors = new MpscLinkedAtomicQueue<>();

    private final MpscLinkedAtomicQueue<ForkJoinTask<?>> forkJoinTasks = new MpscLinkedAtomicQueue<>();

    private final MpscLinkedAtomicQueue<Action> mainThreadActions = new MpscLinkedAtomicQueue<>();
    private final MpscLinkedAtomicQueue<TileEntitySynchronized> requireUpdateTEQueue = new MpscLinkedAtomicQueue<>();
    private final TaskSubmitter submitter = new TaskSubmitter();

    private volatile boolean inTick = false;

    public void init() {
        THREAD_POOL.prestartAllCoreThreads();
        submitter.start();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onServerTick(final TickEvent.ServerTickEvent event) {
        if (event.side == Side.CLIENT) {
            return;
        }
        switch (event.phase) {
            case START -> {
                inTick = true;
                submitter.unpark();
            }
            default -> {
                inTick = false;
                tickExisted++;
            }
        }

        int executed = executeActions();
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
    public int executeActions() {
        int executed = 0;
        long time = System.nanoTime() / 1000;

        submitTask();
        executed += spinAwaitActionExecutor();
        executed += executeMainThreadActions();

        updateTileEntity();

        // Empty Check
        if (!submitted.isEmpty()) {
            executed += executeActions();
        }

        totalUsedTime += System.nanoTime() / 1000 - time;
        return executed;
    }

    private int executeMainThreadActions() {
        int executed = 0;
        if (mainThreadActions.isEmpty()) {
            return executed;
        }

        Action action;
        while ((action = mainThreadActions.poll()) != null) {
            try {
                action.doAction();
            } catch (Exception e) {
                ModularMachinery.log.warn("An error occurred during synchronous task execution!");
                ModularMachinery.log.warn(ThrowableUtil.stackTraceToString(e));
            }
            executed++;
        }
        return executed;
    }

    private int spinAwaitActionExecutor() {
        int executed = 0;

        ActionExecutor executor;
        while ((executor = submitted.poll()) != null) {
            // Spin up while completing the operation in the queue.
            while (!executor.isCompleted) {
                executed += executeMainThreadActions();
                updateTileEntity();
                LockSupport.parkNanos(10_000L);
            }

            taskUsedTime += executor.usedTime;
            executed++;
        }
        return executed;
    }

    private void updateTileEntity() {
        if (requireUpdateTEQueue.isEmpty()) {
            return;
        }

        TileEntitySynchronized te;
        while ((te = requireUpdateTEQueue.poll()) != null) {
            te.markForUpdate();
        }
    }

    /**
     * <p>添加一个异步操作引用，这个操作必定在本 Tick 结束前执行完毕。</p>
     *
     * @param action 要执行的异步任务
     */
    public ActionExecutor addTask(final Action action) {
        return addTask(action, 0);
    }

    /**
     * <p>添加一个异步操作引用，这个操作必定在本 Tick 结束前执行完毕。</p>
     *
     * @param action   要执行的异步任务
     * @param priority 优先级
     */
    public ActionExecutor addTask(final Action action, final int priority) {
        ActionExecutor actionExecutor = new ActionExecutor(action, priority);
        executors.offer(actionExecutor);

        return actionExecutor;
    }

    public <T> ForkJoinTask<T> submitForkJoinTask(final ForkJoinTask<T> task) {
        forkJoinTasks.offer(task);
        return task;
    }



    /**
     * <p>添加一个同步操作引用，这个操作必定会在异步操作完成后在<strong>主线程</strong>中顺序执行。</p>
     *
     * @param action 要执行的同步任务
     */
    public void addSyncTask(final Action action) {
        mainThreadActions.offer(action);
    }

    public void addTEUpdateTask(final TileEntitySynchronized te) {
        requireUpdateTEQueue.offer(te);
    }

    private synchronized void submitTask() {
        ActionExecutor executor;
        while ((executor = executors.poll()) != null) {
            THREAD_POOL.execute(executor);
            submitted.offer(executor);
        }

        ForkJoinTask<?> forkJoinTask;
        while ((forkJoinTask = forkJoinTasks.poll()) != null) {
            FORK_JOIN_POOL.submit(forkJoinTask);
        }
    }

    public class TaskSubmitter implements Runnable {
        public Thread thread = null;

        public void start() {
            if (thread != null && thread.isAlive()) {
                thread.interrupt();
            }
            thread = new Thread(this);
            thread.setName("MMCE-TaskSubmitter");
            thread.start();
        }

        public void unpark() {
            if (thread != null) {
                LockSupport.unpark(thread);
            }
        }

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                if (inTick) {
                    if (!executors.isEmpty()) {
                        submitTask();
                    } else {
                        LockSupport.parkNanos(10000L);
                    }
                } else {
                    LockSupport.park();
                }
            }
        }
    }
}
