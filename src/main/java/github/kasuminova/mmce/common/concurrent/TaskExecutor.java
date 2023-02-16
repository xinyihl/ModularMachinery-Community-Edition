package github.kasuminova.mmce.common.concurrent;

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.LinkedList;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 一个简单的单 Tick 并发执行器
 * 注意：如果提交了一个会修改世界的引用，请使用锁或同步关键字修饰会修改世界的部分代码操作
 */
public class TaskExecutor {
    public static final int AVAILABLE_PROCESSORS = Runtime.getRuntime().availableProcessors() / 2;
    public static final int THREAD_COUNT = Math.max(AVAILABLE_PROCESSORS, 4);
    public static final ForkJoinPool FORK_JOIN_POOL = new ForkJoinPool(AVAILABLE_PROCESSORS);
    public static long totalSubmitted = 0;
    public static AtomicLong taskUsedTime = new AtomicLong(0);
    public static long totalUsedTime = 0;
    public static long tickExisted = 0;
    private final LinkedList<Action> preActions = new LinkedList<>();
    private final LinkedList<Action> postActions = new LinkedList<>();
    private final LinkedList<Action> mainThreadActions = new LinkedList<>();

    @SubscribeEvent
    public void onTick(final TickEvent.ServerTickEvent event) {
        switch (event.phase) {
            case START: {
                executeActions(preActions);
                break;
            }
            case END: {
                executeActions(postActions);
                tickExisted++;
                break;
            }
        }
    }

    public void executeActions(final LinkedList<Action> actions) {
        if (actions.isEmpty()) return;

        long time = System.nanoTime() / 1000;

        actions.stream().parallel().forEach(action -> {
            long start = System.nanoTime();
            action.doAction();
            taskUsedTime.getAndAdd((System.nanoTime() - start) / 1000);
        });
        actions.clear();

        Action action;
        while ((action = mainThreadActions.poll()) != null) {
            action.doAction();
        }

        totalUsedTime += System.nanoTime() / 1000 - time;
    }

    /**
     * 添加一个接口引用，这个引用必定会在下一个 tick 开始时完成
     *
     * @param action 引用
     */
    public void addPreTickTask(final Action action) {
        totalSubmitted++;
        preActions.add(action);
    }

    /**
     * 添加一个接口引用，这个引用必定会在该一个 tick 即将结束时前完成
     *
     * @param action 引用
     */
    public void addPostTickTask(final Action action) {
        totalSubmitted++;
        postActions.add(action);
    }

    public void addMainThreadTask(final Action action) {
        mainThreadActions.add(action);
    }
}
