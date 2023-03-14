/*******************************************************************************
 * HellFirePvP / Modular Machinery 2018
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.machine;

import hellfirepvp.modularmachinery.common.crafting.helper.ComponentSelectorTag;
import hellfirepvp.modularmachinery.common.util.BlockArray;
import hellfirepvp.modularmachinery.common.util.BlockArrayCache;
import hellfirepvp.modularmachinery.common.util.MiscUtils;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: TaggedPositionBlockArray
 * Created by HellFirePvP
 * Date: 04.03.2019 / 21:33
 */
public class TaggedPositionBlockArray extends BlockArray {
    public TaggedPositionBlockArray() {
    }
    public TaggedPositionBlockArray(long traitNum, EnumFacing facing) {
        super(traitNum, facing);
    }

    private final Map<BlockPos, ComponentSelectorTag> taggedPositions = new HashMap<>();

    public void setTag(BlockPos pos, ComponentSelectorTag tag) {
        this.taggedPositions.put(pos, tag);
    }

    @Nullable
    public ComponentSelectorTag getTag(BlockPos pos) {
        return this.taggedPositions.get(pos);
    }

    @Override
    public TaggedPositionBlockArray rotateYCCW(EnumFacing facing) {
        if (this.facing == facing) {
            return this;
        }
        TaggedPositionBlockArray rotated = BlockArrayCache.getTaggedPositionBlockArrayCache(traitNum, facing);
        if (rotated != null) {
            return rotated;
        }

        rotated = this;
        while (rotated.facing != facing) {
            rotated = rotated.rotateYCCWInternal();
        }

        BlockArrayCache.addTaggedPositionBlockArrayCache(traitNum, rotated);
        return rotated;
    }

    private TaggedPositionBlockArray rotateYCCWInternal() {
        TaggedPositionBlockArray out = new TaggedPositionBlockArray(traitNum, facing.rotateYCCW());

        Map<BlockPos, BlockInformation> outPattern = out.pattern;
        for (BlockPos pos : pattern.keySet()) {
            outPattern.put(MiscUtils.rotateYCCW(pos), pattern.get(pos).copyRotateYCCW());
        }
        for (BlockPos pos : taggedPositions.keySet()) {
            out.taggedPositions.put(MiscUtils.rotateYCCW(pos), taggedPositions.get(pos));
        }

        return out;
    }
}
