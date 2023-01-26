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
            TaskExecutor.totalUsedTime = 0;
            TaskExecutor.taskUsedTime.set(0);
            sender.sendMessage(new TextComponentTranslation("command.modularmachinery.performance_report.reset"));
            return;
        }

        long totalSubmitted = TaskExecutor.totalSubmitted;
        long taskUsedTime = TaskExecutor.taskUsedTime.get();
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
                new TextComponentTranslation("command.modularmachinery.performance_report.total_used_time",
                        totalUsedTime / 1000)
        );

        long usedTimeAvg = totalSubmitted == 0 ? 0 : taskUsedTime / totalSubmitted;
        sender.sendMessage(
                new TextComponentTranslation("command.modularmachinery.performance_report.used_time_avg",
                        usedTimeAvg)
        );

        float usedTimeAvgPerTick = (float) (totalUsedTime / tickExisted) / 1000;
        sender.sendMessage(
                new TextComponentTranslation("command.modularmachinery.performance_report.used_time_avg_per_tick",
                        usedTimeAvgPerTick)
        );

        long executedAvgPerTick = totalSubmitted / tickExisted;
        sender.sendMessage(
                new TextComponentTranslation("command.modularmachinery.performance_report.executed_avg_per_tick",
                        executedAvgPerTick)
        );

        sender.sendMessage(new TextComponentString(""));
    }
}
