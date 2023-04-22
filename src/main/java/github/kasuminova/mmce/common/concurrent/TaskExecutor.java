package github.kasuminova.mmce.common.concurrent;

import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.tiles.base.TileEntitySynchronized;
import io.netty.util.internal.ThrowableUtil;
import io.netty.util.internal.shaded.org.jctools.queues.atomic.MpscLinkedAtomicQueue;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * 一个简单的单 Tick 并发执行器
 * 注意：如果提交了一个会修改世界的引用，请使用锁或同步关键字修饰会修改世界的部分代码操作
 */
public class TaskExecutor {
    public static final int THREAD_COUNT = Math.max(Math.max(Runtime.getRuntime().availableProcessors() / 4, 8), 4);
    public static final ThreadPoolExecutor THREAD_POOL = new ThreadPoolExecutor(
            THREAD_COUNT / 4, THREAD_COUNT, 5000, TimeUnit.MILLISECONDS,
            new PriorityBlockingQueue<>(),
            new CustomThreadFactory("MMCE-TaskExecutor-%s"));
    public static long totalExecuted = 0;
    public static long taskUsedTime = 0;
    public static long totalUsedTime = 0;
    public static long executedCount = 0;
    private final ArrayList<ActionExecutor> submitted = new ArrayList<>();
    private final MpscLinkedAtomicQueue<ActionExecutor> executors = new MpscLinkedAtomicQueue<>();
    private final MpscLinkedAtomicQueue<Action> mainThreadActions = new MpscLinkedAtomicQueue<>();
    private final HashSet<TileEntitySynchronized> requireUpdateTEList = new HashSet<>();
    private final TaskSubmitter submitter = new TaskSubmitter();
    private volatile boolean inTick = false;

    public void init() {
        THREAD_POOL.prestartAllCoreThreads();
        submitter.start();
    }

    @SubscribeEvent
    public void onWorldTick(final TickEvent.WorldTickEvent event) {
        if (event.side == Side.CLIENT) {
            return;
        }
        switch (event.phase) {
            case START:
                inTick = true;
                submitter.unpark();
                break;
            case END:
            default:
                inTick = false;
                break;
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
        if (submitted.isEmpty()) {
            return 0;
        }

        int executed = 0;
        long time = System.nanoTime() / 1000;

        submitTask();
        executed += awaitActionExecutor();

        Action action;
        while ((action = mainThreadActions.poll()) != null) {
            action.doAction();
            executed++;
        }

        synchronized (requireUpdateTEList) {
            for (TileEntitySynchronized te : requireUpdateTEList) {
                te.markForUpdate();
            }
            requireUpdateTEList.clear();
        }

        //Empty Check
        if (!submitted.isEmpty()) {
            executed += executeActions();
        }

        totalUsedTime += System.nanoTime() / 1000 - time;
        return executed;
    }

    private int awaitActionExecutor() {
        try {
            if (THREAD_POOL.awaitTermination(50, TimeUnit.MICROSECONDS)) {
                ModularMachinery.log.warn("[Modular Machinery] Parallel action execute timeout for 50ms.");
            }
        } catch (Exception e) {
            ModularMachinery.log.warn("An error occurred during asynchronous task execution!");
            ModularMachinery.log.warn(ThrowableUtil.stackTraceToString(e));
        }

        int executed = 0;
        for (ActionExecutor executor : submitted) {
            taskUsedTime += executor.usedTime;
            executed++;
        }
        submitted.clear();
        return executed;
    }

    /**
     * <p>添加一个并行异步操作引用，这个操作必定在本 Tick 结束前执行完毕。</p>
     *
     * @param action 要执行的异步任务
     */
    public ActionExecutor addParallelAsyncTask(final Action action) {
        return addParallelAsyncTask(action, 0);
    }

    /**
     * <p>添加一个并行异步操作引用，这个操作必定在本 Tick 结束前执行完毕。</p>
     *
     * @param action 要执行的异步任务
     * @param priority 优先级
     */
    public ActionExecutor addParallelAsyncTask(final Action action, final int priority) {
        ActionExecutor actionExecutor = new ActionExecutor(action, priority);
        executors.offer(actionExecutor);

        return actionExecutor;
    }

    /**
     * <p>添加一个同步操作引用，这个操作必定会在异步操作完成后在<strong>主线程</strong>执行。</p>
     *
     * @param action 要执行的同步任务
     */
    public void addSyncTask(final Action action) {
        mainThreadActions.offer(action);
    }

    public void addTEUpdateTask(final TileEntitySynchronized te) {
        synchronized (requireUpdateTEList) {
            requireUpdateTEList.add(te);
        }
    }

    public synchronized void submitTask() {
        ActionExecutor executor;
        while ((executor = executors.poll()) != null) {
            THREAD_POOL.execute(executor);
            submitted.add(executor);
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
            if (thread != null && thread.isAlive()) {
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
                        LockSupport.parkNanos(1000L);
                    }
                } else {
                    LockSupport.park();
                }
            }
        }
    }
}
