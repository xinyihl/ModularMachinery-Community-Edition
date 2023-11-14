package hellfirepvp.modularmachinery.common.command;

import github.kasuminova.mmce.common.concurrent.RecipeCraftingContextPool;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.util.MiscUtils;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Queue;

import static github.kasuminova.mmce.common.concurrent.TaskExecutor.*;

public class CommandPerformanceReport extends CommandBase {
    private static final String LANG_KEY = "command.modularmachinery.performance_report";

    @Nonnull
    @Override
    public String getName() {
        return "mm-performance_report";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Nonnull
    @Override
    public String getUsage(@Nonnull ICommandSender sender) {
        return LANG_KEY;
    }

    @Override
    public void execute(@Nonnull MinecraftServer server, @Nonnull ICommandSender sender, String[] args) {
        if (args.length > 0 && args[0].equals("reset")) {
            executedCount = 0;
            totalExecuted = 0;
            totalUsedTime = 0;
            taskUsedTime = 0;
            sender.sendMessage(new TextComponentTranslation(LANG_KEY + ".reset"));
            return;
        }

        long executedAvgPerExecution = executedCount == 0 ? 0 : totalExecuted / executedCount;
        double usedTimeAvgPerExecution = executedCount == 0 ? 0 : (double) (totalUsedTime / executedCount) / 1000;
        double taskUsedTimeAvg = totalExecuted == 0 ? 0 : (double) (taskUsedTime / totalExecuted) / 1000;
        float efficiency = executedCount == 0 || taskUsedTimeAvg == 0 ? 100 : (float) ((taskUsedTime / executedCount) / taskUsedTimeAvg) * 100;
        long usedTimeAvg = totalExecuted == 0 ? 0 : taskUsedTime / totalExecuted;
        long createdContexts = RecipeCraftingContextPool.getCreatedContexts();
        long cacheHitCount = RecipeCraftingContextPool.getCacheHitCount();
        long cacheRecycledCount = RecipeCraftingContextPool.getCacheRecycledCount();
        long ctxPoolTotalSize = RecipeCraftingContextPool.getPoolTotalSize();
        int ctxPools = RecipeCraftingContextPool.getPools();
        Map.Entry<ResourceLocation, Queue<RecipeCraftingContext>> ctxMaxPoolSize = RecipeCraftingContextPool.getMaxPoolSize();

        sender.sendMessage(new TextComponentTranslation(LANG_KEY + ".title", MiscUtils.formatDecimal(executedCount)));
        sender.sendMessage(new TextComponentString(""));

        sender.sendMessage(new TextComponentTranslation(LANG_KEY + ".total_executed", MiscUtils.formatDecimal(totalExecuted)));
        sender.sendMessage(new TextComponentTranslation(LANG_KEY + ".tasks_avg_per_execution", executedAvgPerExecution));
        sender.sendMessage(new TextComponentString(""));

        sender.sendMessage(new TextComponentTranslation(LANG_KEY + ".total_used_time", totalUsedTime / 1000));
        sender.sendMessage(new TextComponentTranslation(LANG_KEY + ".used_time_avg_per_execution", usedTimeAvgPerExecution));
        sender.sendMessage(new TextComponentString(""));

        sender.sendMessage(new TextComponentTranslation(LANG_KEY + ".task_used_time", MiscUtils.formatDecimal((long) ((double) taskUsedTime / 1000L))));
        sender.sendMessage(new TextComponentString(""));

        String efficiencyMsg = efficiency >= 100 ?
                TextFormatting.GREEN + String.valueOf(efficiency) + TextFormatting.WHITE : efficiency >= 50 ?
                TextFormatting.YELLOW + String.valueOf(efficiency) + TextFormatting.WHITE :
                TextFormatting.RED + String.valueOf(efficiency) + TextFormatting.WHITE;
        sender.sendMessage(new TextComponentTranslation(LANG_KEY + ".task_used_time_avg", taskUsedTimeAvg, efficiencyMsg));
        sender.sendMessage(new TextComponentTranslation(LANG_KEY + ".used_time_avg", usedTimeAvg));

        sender.sendMessage(new TextComponentString(""));

        sender.sendMessage(new TextComponentTranslation(LANG_KEY + ".ctx_created", MiscUtils.formatDecimal(createdContexts)));
        sender.sendMessage(new TextComponentTranslation(LANG_KEY + ".ctx_cache_hit_count", MiscUtils.formatDecimal(cacheHitCount)));
        sender.sendMessage(new TextComponentTranslation(LANG_KEY + ".ctx_cache_recycled_count", MiscUtils.formatDecimal(cacheRecycledCount)));
        sender.sendMessage(new TextComponentTranslation(LANG_KEY + ".ctx_pool_total",
                ctxPoolTotalSize, ctxPools));
        if (ctxMaxPoolSize != null) {
            sender.sendMessage(new TextComponentTranslation(LANG_KEY + ".ctx_pool_recipe_max",
                    ctxMaxPoolSize.getValue().size(), ctxMaxPoolSize.getKey().getPath()));
        }

        sender.sendMessage(new TextComponentString(""));
    }
}
