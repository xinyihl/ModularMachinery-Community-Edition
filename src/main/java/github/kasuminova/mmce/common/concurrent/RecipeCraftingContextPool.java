package github.kasuminova.mmce.common.concurrent;

import hellfirepvp.modularmachinery.common.crafting.ActiveMachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

public class RecipeCraftingContextPool {
    private static final Map<ResourceLocation, Queue<RecipeCraftingContext>> POOL = new ConcurrentHashMap<>();
    private static final AtomicLong CREATED_CONTEXTS = new AtomicLong(0);
    private static final AtomicLong CACHE_HIT_COUNT = new AtomicLong(0);
    private static final AtomicLong CACHE_RECYCLED_COUNT = new AtomicLong(0);

    private static int reloadCounter = 0;

    @Nonnull
    public static RecipeCraftingContext borrowCtx(@Nonnull final ActiveMachineRecipe activeRecipe,
                                                  @Nonnull final TileMultiblockMachineController ctrl)
    {
        if (POOL.isEmpty()) {
            CREATED_CONTEXTS.incrementAndGet();
            return new RecipeCraftingContext(reloadCounter, activeRecipe, ctrl);
        }

        Queue<RecipeCraftingContext> queue = POOL.computeIfAbsent(
                activeRecipe.getRecipe().getRegistryName(), q -> new ConcurrentLinkedQueue<>());

        RecipeCraftingContext ctx;
        if ((ctx = queue.poll()) != null) {
            CACHE_HIT_COUNT.incrementAndGet();
            return ctx.init(activeRecipe, ctrl);
        }

        CREATED_CONTEXTS.incrementAndGet();
        return new RecipeCraftingContext(reloadCounter, activeRecipe, ctrl);
    }

    public static void returnCtx(@Nonnull RecipeCraftingContext ctx) {
        if (ctx.getReloadCounter() != reloadCounter) {
            ctx.destroy();
            return;
        }
        CACHE_RECYCLED_COUNT.incrementAndGet();
        POOL.computeIfAbsent(ctx.getParentRecipe().getRegistryName(), q -> new ConcurrentLinkedQueue<>())
                .offer(ctx.resetAll());
    }

    public static long getPoolTotalSize() {
        long total = 0;
        for (final Queue<RecipeCraftingContext> queue : POOL.values()) {
            total += queue.size();
        }
        return total;
    }

    public static int getPools() {
        return POOL.size();
    }

    public static long getCreatedContexts() {
        return CREATED_CONTEXTS.get();
    }

    public static long getCacheHitCount() {
        return CACHE_HIT_COUNT.get();
    }

    public static long getCacheRecycledCount() {
        return CACHE_RECYCLED_COUNT.get();
    }

    public static Map.Entry<ResourceLocation, Queue<RecipeCraftingContext>> getMaxPoolSize() {
        Map.Entry<ResourceLocation, Queue<RecipeCraftingContext>> max = null;
        long maxSize = 0;

        for (final Map.Entry<ResourceLocation, Queue<RecipeCraftingContext>> entry : POOL.entrySet()) {
            int size = entry.getValue().size();
            if (size > maxSize) {
                max = entry;
                maxSize = size;
            }
        }

        return max;
    }

    public static void clear() {
        for (final Queue<RecipeCraftingContext> queue : POOL.values()) {
            queue.clear();
        }

        reloadCounter++;
        CREATED_CONTEXTS.set(0);
        CACHE_HIT_COUNT.set(0);
        CACHE_RECYCLED_COUNT.set(0);
    }
}
