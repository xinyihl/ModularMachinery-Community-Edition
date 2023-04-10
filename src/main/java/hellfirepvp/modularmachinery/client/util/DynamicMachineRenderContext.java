/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.client.util;

import hellfirepvp.modularmachinery.client.ClientScheduler;
import hellfirepvp.modularmachinery.common.block.BlockController;
import hellfirepvp.modularmachinery.common.lib.BlocksMM;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.util.BlockArray;
import hellfirepvp.modularmachinery.common.util.IBlockStateDescriptor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: DynamicMachineRenderContext
 * Created by HellFirePvP
 * Date: 09.07.2017 / 21:18
 */
public class DynamicMachineRenderContext {

    private final DynamicMachine machine;
    private final BlockArrayRenderHelper render;
    private final Vec3i moveOffset;

    private boolean render3D = true;
    private int renderSlice = 0;
    private float scale = 1F;

    private long shiftSnap = -1;

    private DynamicMachineRenderContext(DynamicMachine machine) {
        this.machine = machine;
        BlockArray pattern = machine.getPattern();
        Vec3i min = pattern.getMin();
        Vec3i max = pattern.getMax();
        this.moveOffset = new Vec3i(
                (min.getX() + (max.getX() - min.getX()) / 2) * -1,
                -min.getY(),
                (min.getZ() + (max.getZ() - min.getZ()) / 2) * -1);

        BlockArray copy = new BlockArray(pattern, this.moveOffset);

        addControllerToBlockArray(machine, copy, this.moveOffset);
        addReplacementToBlockArray(machine.getModifiersAsMatchingReplacements(), copy, this.moveOffset);

        this.render = new BlockArrayRenderHelper(copy);
    }

    private void addControllerToBlockArray(DynamicMachine machine, BlockArray copy, Vec3i moveOffset) {
        BlockController ctrl = BlockController.getControllerWithMachine(machine);
        if (ctrl == null) ctrl = BlocksMM.blockController;

        copy.addBlock(new BlockPos(moveOffset), new BlockArray.BlockInformation(
                Collections.singletonList(new IBlockStateDescriptor(ctrl.getDefaultState()))));
    }

    public static void addReplacementToBlockArray(
            DynamicMachine.ModifierReplacementMap replacementMap,
            BlockArray blockArray,
            Vec3i moveOffset) {
        for (Map.Entry<BlockPos, List<BlockArray.BlockInformation>> entry : replacementMap.entrySet()) {
            BlockPos pos = entry.getKey().add(moveOffset);

            List<BlockArray.BlockInformation> informationList = entry.getValue();
            for (BlockArray.BlockInformation info : informationList) {
                Map<BlockPos, BlockArray.BlockInformation> pattern = blockArray.getPattern();
                if (pattern.containsKey(pos)) {
                    pattern.get(pos).addMatchingStates(info.matchingStates);
                } else {
                    pattern.put(pos, info);
                }
            }
        }
    }

    public static DynamicMachineRenderContext createContext(DynamicMachine machine) {
        return new DynamicMachineRenderContext(machine);
    }

    BlockArrayRenderHelper getRender() {
        return render;
    }

    public Vec3i getMoveOffset() {
        return moveOffset;
    }

    public long getShiftSnap() {
        return shiftSnap;
    }

    public void snapSamples() {
        this.shiftSnap = ClientScheduler.getClientTick();
    }

    public void releaseSamples() {
        this.shiftSnap = -1;
    }

    public void resetRender() {
        setTo2D();
        setTo3D();
    }

    public void setTo2D() {
        if (!render3D) return;
        render3D = false;
        renderSlice = render.getBlocks().getMin().getY();
        render.resetRotation2D();
        scale = 1F;
    }

    public void setTo3D() {
        if (render3D) return;
        render3D = true;
        renderSlice = 0;
        render.resetRotation();
        scale = 1F;
    }

    public float getScale() {
        return scale;
    }

    public Vec3d getCurrentMachineTranslate() {
        if (render3D) {
            return new Vec3d(0, 0, 0);
        }
        return this.render.getCurrentTranslation();
    }

    public Vec2f getCurrentRenderOffset(float x, float z) {
        Minecraft mc = Minecraft.getMinecraft();
        double sc = new ScaledResolution(mc).getScaleFactor();
        double oX = x + 16D / sc;
        double oZ = z + 16D / sc;
        Vec3d tr = getCurrentMachineTranslate();
        return new Vec2f((float) (oX + tr.x), (float) (oZ + tr.z));
    }

    public void zoomOut() {
        scale *= 0.85F;
    }

    public void zoomIn() {
        scale *= 1.15F;
    }

    public boolean doesRenderIn3D() {
        return render3D;
    }

    public int getRenderSlice() {
        return renderSlice - this.moveOffset.getY();
    }

    public boolean hasSliceDown() {
        return render.getBlocks().getMin().getY() < renderSlice;
    }

    public boolean hasSliceUp() {
        return render.getBlocks().getMax().getY() > renderSlice;
    }

    public void sliceUp() {
        if (hasSliceUp()) {
            renderSlice++;
        }
    }

    public void sliceDown() {
        if (hasSliceDown()) {
            renderSlice--;
        }
    }

    public DynamicMachine getDisplayedMachine() {
        return machine;
    }

    @SideOnly(Side.CLIENT)
    public List<ItemStack> getDescriptiveStacks() {
        return this.machine.getPattern().getAsDescriptiveStacks(shiftSnap == -1 ? Optional.empty() : Optional.of(shiftSnap));
    }

    public void renderAt(int x, int z) {
        renderAt(x, z, 1F);
    }

    public void renderAt(int x, int z, float partialTicks) {
        render.sampleSnap = shiftSnap;
        if (render3D) {
            render.render3DGUI(x, z, scale, partialTicks);
        } else {
            render.render3DGUI(x, z, scale, partialTicks, Optional.of(renderSlice));
        }
    }

    public void rotateRender(double x, double y, double z) {
        this.render.rotate(x, y, z);
    }

    public void moveRender(double x, double y, double z) {
        this.render.translate(x, y, z);
    }

}
