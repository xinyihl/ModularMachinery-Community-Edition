package github.kasuminova.mmce.common.concurrent;

import hellfirepvp.modularmachinery.common.crafting.ActiveMachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class RecipeCraftingContextPool {
    private static final Map<ResourceLocation, Queue<WeakReference<RecipeCraftingContext>>> POOL = new ConcurrentHashMap<>();

    private static int reloadCounter = 0;

    @Nonnull
    public static RecipeCraftingContext borrowCtx(@Nonnull final ActiveMachineRecipe activeRecipe,
                                                  @Nonnull final TileMultiblockMachineController ctrl)
    {
        if (POOL.isEmpty()) {
            return new RecipeCraftingContext(reloadCounter, activeRecipe, ctrl);
        }

        Queue<WeakReference<RecipeCraftingContext>> queue = POOL.computeIfAbsent(
                activeRecipe.getRecipe().getRegistryName(), q -> new ConcurrentLinkedQueue<>());

        WeakReference<RecipeCraftingContext> polled;
        while (!queue.isEmpty()) {
            polled = queue.poll();
            if (polled == null) {
                continue;
            }

            RecipeCraftingContext ctx = polled.get();
            if (ctx == null) {
                continue;
            }

            return ctx.init(activeRecipe, ctrl);
        }

        return new RecipeCraftingContext(reloadCounter, activeRecipe, ctrl);
    }

    public static void returnCtx(@Nonnull RecipeCraftingContext ctx) {
        if (ctx.getReloadCounter() != reloadCounter) {
            ctx.destroy();
            return;
        }

        POOL.computeIfAbsent(ctx.getParentRecipe().getRegistryName(), q -> new ConcurrentLinkedQueue<>())
                .offer(new WeakReference<>(ctx.resetAll()));
    }

    public static long getPoolTotalSize() {
        long total = 0;
        for (final Queue<WeakReference<RecipeCraftingContext>> queue : POOL.values()) {
            total += queue.size();
        }
        return total;
    }

    public static int getPools() {
        return POOL.size();
    }

    public static Map.Entry<ResourceLocation, Queue<WeakReference<RecipeCraftingContext>>> getMaxPoolSize() {
        Map.Entry<ResourceLocation, Queue<WeakReference<RecipeCraftingContext>>> max = null;
        long maxSize = 0;

        for (final Map.Entry<ResourceLocation, Queue<WeakReference<RecipeCraftingContext>>> entry : POOL.entrySet()) {
            int size = entry.getValue().size();
            if (size > maxSize) {
                max = entry;
                maxSize = size;
            }
        }

        return max;
    }

    public static void clear() {
        for (final Queue<WeakReference<RecipeCraftingContext>> queue : POOL.values()) {
            queue.clear();
        }

        reloadCounter++;
    }
}
