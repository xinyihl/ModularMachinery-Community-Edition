package hellfirepvp.modularmachinery.common.integration.theoneprobe;

import hellfirepvp.modularmachinery.common.integration.ModIntegrationTOP;
import hellfirepvp.modularmachinery.common.tiles.TileMachineController;
import mcjty.theoneprobe.api.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.client.model.animation.Animation;

public class DynamicMachineInfoProvider implements IProbeInfoProvider {
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
        TileMachineController machine = tileEntity instanceof TileMachineController ? (TileMachineController) tileEntity : null;
        if (machine == null) return;

        IProbeInfo statusLine = probeInfo.horizontal(probeInfo.defaultLayoutStyle().alignment(ElementAlignment.ALIGN_CENTER));

        //是否在工作
        if (machine.hasActiveRecipe() && machine.getFoundMachine() != null) {
            statusLine.text(TextFormatting.GREEN + I18n.format("top.machine.working"));

            float progress = machine.getCurrentActiveRecipeProgressWithModifier(Animation.getPartialTickTime()) * 100;

            int recipeTotalTickTime = machine.getRecipeTotalTime();

            String progressStr;
            if (player.isSneaking()) {
                //如：20.5 秒 / 40.0 秒
                //Example: 20.5 Sec / 40.0 Sec
                progressStr = I18n.format("top.recipe.progress.second",
                        String.format("%.1f", ((progress / 100) * recipeTotalTickTime) / 20),
                        String.format("%.1f", (float) recipeTotalTickTime / 20)
                );
            } else if (ModIntegrationTOP.showRecipeProgressBarDecimalPoints &&
                    recipeTotalTickTime >= 1000) {
                //只有当启用了显示小数点且配方耗时超过 1000 tick 才会显示小数点
                progressStr = String.format("%.2f", progress) + "%";
            } else {
                progressStr = String.format("%.0f", progress) + "%";
            }

            probeInfo.progress((int) progress, 100, probeInfo.defaultProgressStyle()
                    .prefix(I18n.format("top.recipe.progress"))
                    .suffix(progressStr)
                    .filledColor(ModIntegrationTOP.recipeProgressBarFilledColor)
                    .alternateFilledColor(ModIntegrationTOP.recipeProgressBarAlternateFilledColor)
                    .borderColor(ModIntegrationTOP.recipeProgressBarBorderColor)
                    .backgroundColor(ModIntegrationTOP.recipeProgressBarBackgroundColor)
                    .numberFormat(NumberFormat.NONE)
            );
        } else {
            //是否形成结构
            if (machine.getFoundMachine() != null) {
                statusLine.text(TextFormatting.GREEN + I18n.format("top.machine.structure.found"));

                IProbeInfo errorLine = probeInfo.horizontal(probeInfo.defaultLayoutStyle().alignment(ElementAlignment.ALIGN_CENTER));
                errorLine.text(TextFormatting.RED + I18n.format(machine.getCraftingStatus().getUnlocMessage()));
            } else {
                statusLine.text(TextFormatting.RED + I18n.format(machine.getCraftingStatus().getUnlocMessage()));
            }
        }
    }

}
