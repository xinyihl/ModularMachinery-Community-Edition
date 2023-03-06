package github.kasuminova.mmce.common.concurrent;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Locks {
    /**
     * 更新锁，用来进行方块更新、同步等非线程安全操作。
     */
    public static final Lock UPDATE_LOCK = new ReentrantLock();
}
