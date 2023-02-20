package hellfirepvp.modularmachinery.common.command;

import github.kasuminova.mmce.common.concurrent.TaskExecutor;
import hellfirepvp.modularmachinery.common.util.MiscUtils;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;

import static github.kasuminova.mmce.common.concurrent.TaskExecutor.*;

public class CommandPerformanceReport extends CommandBase {
    private static final String langKey = "command.modularmachinery.performance_report";

    @Override
    public String getName() {
        return "mm-performance_report";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return langKey;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
        if (args.length > 0 && args[0].equals("reset")) {
            executedCount = 0;
            totalExecuted = 0;
            totalUsedTime = 0;
            taskUsedTime = 0;
            sender.sendMessage(new TextComponentTranslation(langKey + ".reset"));
            return;
        }

        long executedAvgPerExecution = executedCount == 0 ? 0 : totalExecuted / executedCount;
        float usedTimeAvgPerExecution = executedCount == 0 ? 0 : (float) (totalUsedTime / executedCount) / 1000;
        long mainThreadUsedTimeAvg = executedCount == 0 ? 0 : ((totalUsedTime - taskUsedTime) / executedCount) / 1000;
        long taskUsedTimeAvg = totalExecuted == 0 ? 0 : (taskUsedTime / totalExecuted) / 1000;
        long usedTimeAvg = totalExecuted == 0 ? 0 : taskUsedTime / totalExecuted;

        sender.sendMessage(new TextComponentTranslation(langKey + ".title", executedCount));
        sender.sendMessage(new TextComponentString(""));

        sender.sendMessage(new TextComponentTranslation(langKey + ".total_executed", MiscUtils.formatDecimal(TaskExecutor.totalExecuted)));
        sender.sendMessage(new TextComponentTranslation(langKey + ".tasks_avg_per_execution", executedAvgPerExecution));
        sender.sendMessage(new TextComponentString(""));

        sender.sendMessage(new TextComponentTranslation(langKey + ".total_used_time", totalUsedTime / 1000));
        sender.sendMessage(new TextComponentTranslation(langKey + ".used_time_avg_per_execution", usedTimeAvgPerExecution));
        sender.sendMessage(new TextComponentString(""));

        sender.sendMessage(new TextComponentTranslation(langKey + ".main_thread_total_used_time", (totalUsedTime - taskUsedTime) / 1000));
        sender.sendMessage(new TextComponentTranslation(langKey + ".task_used_time", taskUsedTime / 1000));
        sender.sendMessage(new TextComponentString(""));

        sender.sendMessage(new TextComponentTranslation(langKey + ".main_thread_used_time_avg", mainThreadUsedTimeAvg));
        sender.sendMessage(new TextComponentTranslation(langKey + ".task_used_time_avg", taskUsedTimeAvg));

        sender.sendMessage(new TextComponentTranslation(langKey + ".used_time_avg", usedTimeAvg));

        sender.sendMessage(new TextComponentString(""));
    }
}
