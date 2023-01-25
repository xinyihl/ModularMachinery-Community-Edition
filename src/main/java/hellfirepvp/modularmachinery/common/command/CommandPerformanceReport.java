package hellfirepvp.modularmachinery.common.command;

import github.kasuminova.mmce.concurrent.TaskExecutor;
import hellfirepvp.modularmachinery.common.util.MiscUtils;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;

public class CommandPerformanceReport extends CommandBase {

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
        return "command.modularmachinery.performance_report";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
        if (args.length > 0 && args[0].equals("reset")) {
            TaskExecutor.tickExisted = 0;
            TaskExecutor.totalSubmitted = 0;
            TaskExecutor.totalExecuted = 0;
            TaskExecutor.totalUsedTime = 0;
            TaskExecutor.taskUsedTime = 0;
            sender.sendMessage(new TextComponentTranslation("command.modularmachinery.performance_report.reset"));
            return;
        }

        long totalExecuted = TaskExecutor.totalExecuted;
        long taskUsedTime = TaskExecutor.taskUsedTime;
        long totalUsedTime = TaskExecutor.totalUsedTime;
        long tickExisted = TaskExecutor.tickExisted;

        sender.sendMessage(
                new TextComponentTranslation("command.modularmachinery.performance_report.title",
                        tickExisted)
        );
        sender.sendMessage(new TextComponentString(""));

        sender.sendMessage(
                new TextComponentTranslation("command.modularmachinery.performance_report.total_submitted",
                        MiscUtils.formatDecimal(TaskExecutor.totalSubmitted))
        );

        sender.sendMessage(
                new TextComponentTranslation("command.modularmachinery.performance_report.total_executed",
                        MiscUtils.formatDecimal(totalExecuted))
        );

        sender.sendMessage(
                new TextComponentTranslation("command.modularmachinery.performance_report.total_used_time",
                        totalUsedTime / 1000)
        );

        long usedTimeAvg = totalExecuted == 0 ? 0 : taskUsedTime / totalExecuted;
        sender.sendMessage(
                new TextComponentTranslation("command.modularmachinery.performance_report.used_time_avg",
                        usedTimeAvg)
        );

        float usedTimeAvgPerTick = (float) (totalUsedTime / tickExisted) / 1000;
        sender.sendMessage(
                new TextComponentTranslation("command.modularmachinery.performance_report.used_time_avg_per_tick",
                        usedTimeAvgPerTick)
        );

        long executedAvgPerTick = totalExecuted / tickExisted;
        sender.sendMessage(
                new TextComponentTranslation("command.modularmachinery.performance_report.executed_avg_per_tick",
                        executedAvgPerTick)
        );

        sender.sendMessage(new TextComponentString(""));
    }
}
