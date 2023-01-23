package github.kasuminova.mmce.concurrent;

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;

/**
 * 一个简单的单 Tick 并发执行器
 * 注意：如果提交了一个会修改世界的引用，请使用锁或同步关键字修饰会修改世界的部分代码操作
 */
public class TaskExecutor {
    public static long totalExecuted = 0;
    public static long totalUsedTime = 0;
    public static long tickExisted = 0;
    private final ArrayList<ParallelAction> preParallelActions = new ArrayList<>();
    private final ArrayList<ParallelAction> postParallelActions = new ArrayList<>();

    @SubscribeEvent
    public void onTick(final TickEvent.ServerTickEvent event) {
        switch (event.phase) {
            case START: {
                long time = System.nanoTime() / 1000;

                if (!preParallelActions.isEmpty()) {
                    preParallelActions.stream().parallel().forEach(ParallelAction::doAction);
                    totalExecuted += preParallelActions.size();

                    preParallelActions.clear();
                }

                time = System.nanoTime() / 1000 - time;
                totalUsedTime += time;
                break;
            }
            case END: {
                long time = System.nanoTime() / 1000;

                if (!postParallelActions.isEmpty()) {
                    postParallelActions.stream().parallel().forEach(ParallelAction::doAction);
                    totalExecuted += postParallelActions.size();

                    postParallelActions.clear();
                }

                time = System.nanoTime() / 1000 - time;
                totalUsedTime += time;

                tickExisted++;
                break;
            }
        }
    }

    /**
     * 添加一个接口引用，这个引用必定会在下一个 tick 开始时完成
     * @param action 引用
     */
    public void addPreTickTask(final ParallelAction action) {
        preParallelActions.add(action);
    }

    /**
     * 添加一个接口引用，这个引用必定会在该一个 tick 即将结束时前完成
     * @param action 引用
     */
    public void addPostTickTask(final ParallelAction action) {
        postParallelActions.add(action);
    }
}
