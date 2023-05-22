package hellfirepvp.modularmachinery.common.integration.theoneprobe;

import hellfirepvp.modularmachinery.common.crafting.ActiveMachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.helper.CraftingStatus;
import hellfirepvp.modularmachinery.common.integration.ModIntegrationTOP;
import hellfirepvp.modularmachinery.common.machine.factory.FactoryRecipeThread;
import hellfirepvp.modularmachinery.common.tiles.TileFactoryController;
import hellfirepvp.modularmachinery.common.tiles.TileMachineController;
import hellfirepvp.modularmachinery.common.tiles.TileParallelController;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import mcjty.theoneprobe.api.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class MMInfoProvider implements IProbeInfoProvider {
    @Override
    public String getID() {
        return "modularmachinery:dynamic_machine_info_provider";
    }

    @Override
    public void addProbeInfo(ProbeMode mode, IProbeInfo probeInfo, EntityPlayer player, World world, IBlockState blockState, IProbeHitData data) {
        if (!blockState.getBlock().hasTileEntity(blockState)) return;

        //获取方块实体
        TileEntity tileEntity = world.getTileEntity(data.getPos());
        if (tileEntity == null) return;
        //判断是否为机械控制器
        if (tileEntity instanceof TileMultiblockMachineController) {
            processMultiblockMachineTOP((TileMultiblockMachineController) tileEntity, probeInfo, player);
        }
        if (tileEntity instanceof TileParallelController) {
            processParallelControllerTOP((TileParallelController) tileEntity, probeInfo);
        }
    }

    private static void processParallelControllerTOP(TileParallelController parallelController, IProbeInfo probeInfo) {
        if (!ModIntegrationTOP.showParallelControllerInfo) {
            return;
        }
        TileParallelController.ParallelControllerProvider provider = parallelController.provideComponent();
        probeInfo.text(TextFormatting.AQUA + "{*top.parallelism*}" + TextFormatting.GREEN + provider.getParallelism());
        probeInfo.text(TextFormatting.GOLD + "{*top.max_parallelism*}" + TextFormatting.YELLOW + provider.getMaxParallelism());
    }

    private static void processMultiblockMachineTOP(TileMultiblockMachineController machine, IProbeInfo probeInfo, EntityPlayer player) {
        //是否形成结构
        if (machine.isStructureFormed()) {
            probeInfo.text(TextFormatting.GREEN + "{*top.machine.structure.found*}");
        } else {
            probeInfo.text(TextFormatting.RED + "{*top.machine.structure.none*}");
            return;
        }

        if (machine instanceof TileMachineController) {
            processMachineControllerTOP((TileMachineController) machine, probeInfo, player);
        }
        if (machine instanceof TileFactoryController) {
            processFactoryControllerTOP((TileFactoryController) machine, probeInfo, player);
        }
    }

    // TODO: Really long...
    private static void processFactoryControllerTOP(TileFactoryController factory, IProbeInfo probeInfo, EntityPlayer player) {
        if (factory.isWorking()) {
            probeInfo.text(TextFormatting.GREEN + "{*top.machine.working*}");
        } else {
            return;
        }

        List<FactoryRecipeThread> recipeThreads = factory.getFactoryRecipeThreadList();

        probeInfo.text(
                TextFormatting.GREEN + String.valueOf(recipeThreads.size()) +
                TextFormatting.AQUA + " {*top.factory.thread.running*}" +
                TextFormatting.RESET + " / " +
                TextFormatting.YELLOW + factory.getFoundMachine().getMaxThreads() +
                TextFormatting.GOLD + " {*top.factory.thread.max*}"
        );

        int trueMaxParallelism = factory.getMaxParallelism();
        if (factory.getAvailableParallelism() != trueMaxParallelism) {
            probeInfo.text(TextFormatting.AQUA + "{*top.parallelism*}" +
                    TextFormatting.GREEN + ((trueMaxParallelism - factory.getAvailableParallelism()) + 1));
            probeInfo.text(TextFormatting.GOLD + "{*top.max_parallelism*}" +
                    TextFormatting.YELLOW + trueMaxParallelism);
        }

        AtomicInteger i = new AtomicInteger();
        Collection<FactoryRecipeThread> coreThreadList = factory.getCoreRecipeThreads().values();
        List<FactoryRecipeThread> recipeThreadList = new ArrayList<>((int) ((coreThreadList.size() + recipeThreads.size()) * 1.5));
        recipeThreadList.addAll(coreThreadList);
        recipeThreadList.addAll(recipeThreads);

        recipeThreadList.stream().limit(6).forEach(thread -> {
            ActiveMachineRecipe activeRecipe = thread.getActiveRecipe();
            CraftingStatus status = thread.getStatus();

            int progressBarFilledColor = ModIntegrationTOP.recipeProgressBarFilledColor;
            int progressBarAlternateFilledColor = ModIntegrationTOP.recipeProgressBarAlternateFilledColor;
            int progressBarBorderColor = ModIntegrationTOP.recipeProgressBarBorderColor;

            String threadName;
            if (thread.isCoreThread()) {
                threadName = TextFormatting.BLUE + "{*" + thread.getThreadName() + "*}";
                i.getAndIncrement();
            } else {
                threadName = TextFormatting.AQUA + "{*top.factory.thread*}" + i.getAndIncrement();
            }

            if (status.isCrafting()) {
                probeInfo.text(threadName + ": " + TextFormatting.GREEN + "{*" + status.getUnlocMessage() + "*}");
            } else {
                probeInfo.text(threadName + ": " + TextFormatting.RED + "{*" + status.getUnlocMessage() + "*}");
                progressBarFilledColor = ModIntegrationTOP.failureProgressBarFilledColor;
                progressBarAlternateFilledColor = ModIntegrationTOP.failureProgressBarAlternateFilledColor;
                progressBarBorderColor = ModIntegrationTOP.failureProgressBarBorderColor;
            }

            if (activeRecipe == null) {
                return;
            }

            int tick = activeRecipe.getTick();
            int totalTick = activeRecipe.getTotalTick();
            float progress = (float) (tick * 100) / totalTick;

            String progressStr;
            if (player.isSneaking()) {
                //如：20.5 秒 / 40.0 秒
                //Example: 20.5 Sec / 40.0 Sec
                progressStr = String.format("%.1f s / %.1f s", (float) tick / 20, (float) totalTick / 20);
            } else if (ModIntegrationTOP.showRecipeProgressBarDecimalPoints && totalTick >= 1000) {
                //只有当启用了显示小数点且配方耗时超过 1000 tick 才会显示小数点
                progressStr = String.format("%.2f", progress) + "%";
            } else {
                progressStr = String.format("%.0f", progress) + "%";
            }

            IProbeInfo progressLine = probeInfo.horizontal(probeInfo.defaultLayoutStyle().alignment(ElementAlignment.ALIGN_CENTER));
            progressLine.text(TextFormatting.AQUA + "{*top.recipe.progress*}:  ");
            progressLine.progress((int) progress, 100, probeInfo.defaultProgressStyle()
                    .prefix(progressStr)
                    .filledColor(progressBarFilledColor)
                    .alternateFilledColor(progressBarAlternateFilledColor)
                    .borderColor(progressBarBorderColor)
                    .backgroundColor(ModIntegrationTOP.recipeProgressBarBackgroundColor)
                    .numberFormat(NumberFormat.NONE));
        });
    }

    // TODO: Really long...
    private static void processMachineControllerTOP(TileMachineController machine, IProbeInfo probeInfo, EntityPlayer player) {
        //是否在工作
        if (machine.getActiveRecipe() == null || machine.getFoundMachine() == null) {
            probeInfo.text(TextFormatting.RED + "{*" + machine.getControllerStatus().getUnlocMessage() + "*}");
            return;
        }

        int progressBarFilledColor = ModIntegrationTOP.recipeProgressBarFilledColor;
        int progressBarAlternateFilledColor = ModIntegrationTOP.recipeProgressBarAlternateFilledColor;
        int progressBarBorderColor = ModIntegrationTOP.recipeProgressBarBorderColor;

        if (machine.getControllerStatus().isCrafting()) {
            probeInfo.text(TextFormatting.GREEN + "{*top.machine.working*}");
        } else {
            probeInfo.text(TextFormatting.RED + "{*" + machine.getControllerStatus().getUnlocMessage() + "*}");
            progressBarFilledColor = ModIntegrationTOP.failureProgressBarFilledColor;
            progressBarAlternateFilledColor = ModIntegrationTOP.failureProgressBarAlternateFilledColor;
            progressBarBorderColor = ModIntegrationTOP.failureProgressBarBorderColor;
        }

        ActiveMachineRecipe activeRecipe = machine.getActiveRecipe();
        int tick = activeRecipe.getTick();
        int totalTick = activeRecipe.getTotalTick();
        float progress = (float) (tick * 100) / totalTick;

        if (activeRecipe.getParallelism() > 1) {
            probeInfo.text(TextFormatting.AQUA + "{*top.parallelism*}" + TextFormatting.GREEN + activeRecipe.getParallelism());
            probeInfo.text(TextFormatting.GOLD + "{*top.max_parallelism*}" + TextFormatting.YELLOW + activeRecipe.getMaxParallelism());
        }

        String progressStr;
        if (player.isSneaking()) {
            //如：20.5 秒 / 40.0 秒
            //Example: 20.5 Sec / 40.0 Sec
            progressStr = String.format("%.1f s / %.1f s", (float) tick / 20, (float) totalTick / 20);
        } else if (ModIntegrationTOP.showRecipeProgressBarDecimalPoints && totalTick >= 1000) {
            //只有当启用了显示小数点且配方耗时超过 1000 tick 才会显示小数点
            progressStr = String.format("%.2f", progress) + "%";
        } else {
            progressStr = String.format("%.0f", progress) + "%";
        }

        IProbeInfo progressLine = probeInfo.horizontal(probeInfo.defaultLayoutStyle().alignment(ElementAlignment.ALIGN_CENTER));
        progressLine.text("{*top.recipe.progress*}:  ");
        progressLine.progress((int) progress, 100, probeInfo.defaultProgressStyle()
                .prefix(progressStr)
                .filledColor(progressBarFilledColor)
                .alternateFilledColor(progressBarAlternateFilledColor)
                .borderColor(progressBarBorderColor)
                .backgroundColor(ModIntegrationTOP.recipeProgressBarBackgroundColor)
                .numberFormat(NumberFormat.NONE)
        );
    }

}
