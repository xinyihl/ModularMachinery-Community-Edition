package github.kasuminova.mmce.client.world;

import com.cleanroommc.multiblocked.persistence.MultiblockWorldSavedData;
import hellfirepvp.modularmachinery.common.base.Mods;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.machine.TaggedPositionBlockArray;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class BlockModelHider {

    @SideOnly(Side.CLIENT)
    public static void hideOrShowBlocks(TileMultiblockMachineController ctrl) {
        if (Mods.MBD.isPresent()) {
            hideOrShowBlocksMBD(ctrl);
        } else if (Mods.COMPONENT_MODEL_HIDER.isPresent()) {
            hideOrShowBlocksPlugin(ctrl);
        }
    }

    @Optional.Method(modid = "multiblocked")
    private static void hideOrShowBlocksMBD(TileMultiblockMachineController ctrl) {
        DynamicMachine foundMachine = ctrl.getFoundMachine();
        BlockPos pos = ctrl.getPos();
        if (foundMachine == null || !foundMachine.isHideComponentsWhenFormed()) {
            MultiblockWorldSavedData.removeDisableModel(pos);
        }

        if (!MultiblockWorldSavedData.multiDisabled.containsKey(pos)) {
            TaggedPositionBlockArray foundPattern = ctrl.getFoundPattern();
            MultiblockWorldSavedData.addDisableModel(pos, foundPattern.getPattern().keySet());
        }
    }

    @Optional.Method(modid = "component_model_hider")
    private static void hideOrShowBlocksPlugin(TileMultiblockMachineController ctrl) {
        DynamicMachine foundMachine = ctrl.getFoundMachine();
        BlockPos pos = ctrl.getPos();
        if (foundMachine == null || !foundMachine.isHideComponentsWhenFormed()) {
            MultiblockWorldSavedData.removeDisableModel(pos);
        }

        if (!MultiblockWorldSavedData.multiDisabled.containsKey(pos)) {
            TaggedPositionBlockArray foundPattern = ctrl.getFoundPattern();
            MultiblockWorldSavedData.addDisableModel(pos, foundPattern.getPattern().keySet());
        }
    }

}
