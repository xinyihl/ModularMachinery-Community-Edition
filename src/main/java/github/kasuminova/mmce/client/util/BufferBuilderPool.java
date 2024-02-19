package github.kasuminova.mmce.client.util;

import net.minecraft.client.renderer.BufferBuilder;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BufferBuilderPool {

    private static final Queue<BufferBuilder> POOL = new ConcurrentLinkedQueue<>();

    public static BufferBuilder borrowBuffer(int initSize) {
        BufferBuilder buffer;
        if ((buffer = POOL.poll()) != null) {
            return buffer;
        }
        return new BufferBuilder(initSize);
    }

    public static void returnBuffer(final BufferBuilder buffer) {
        POOL.offer(buffer);
    }

}
