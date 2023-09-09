/*******************************************************************************
 * HellFirePvP / Modular Machinery 2018
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.machine;

import crafttweaker.annotations.ZenRegister;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentSelectorTag;
import hellfirepvp.modularmachinery.common.util.BlockArray;
import hellfirepvp.modularmachinery.common.util.MiscUtils;
import net.minecraft.util.math.BlockPos;
import stanhebben.zenscript.annotations.ZenClass;

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
@ZenRegister
@ZenClass("mods.modularmachinery.TaggedPositionBlockArray")
public class TaggedPositionBlockArray extends BlockArray {
    private Map<BlockPos, ComponentSelectorTag> taggedPositions = new HashMap<>();

    public TaggedPositionBlockArray() {
    }

    public TaggedPositionBlockArray(long traitNum) {
        super(traitNum);
    }

    public TaggedPositionBlockArray(TaggedPositionBlockArray another) {
        super(another);
        taggedPositions.putAll(another.taggedPositions);
    }

    public void setTag(BlockPos pos, ComponentSelectorTag tag) {
        this.taggedPositions.put(pos, tag);
    }

    @Nullable
    public ComponentSelectorTag getTag(BlockPos pos) {
        return this.taggedPositions.get(pos);
    }

    public Map<BlockPos, ComponentSelectorTag> getTaggedPositions() {
        return taggedPositions;
    }

    @Override
    public void overwrite(final BlockArray other) {
        super.overwrite(other);
        if (other instanceof TaggedPositionBlockArray) {
            this.taggedPositions = new HashMap<>(((TaggedPositionBlockArray) other).taggedPositions);
        }
    }

    @Override
    public TaggedPositionBlockArray rotateYCCW() {
        TaggedPositionBlockArray out = new TaggedPositionBlockArray(traitNum);

        for (Map.Entry<BlockPos, BlockInformation> entry : pattern.entrySet()) {
            out.pattern.put(MiscUtils.rotateYCCW(entry.getKey()), entry.getValue().copyRotateYCCW());
        }

        for (final Map.Entry<BlockPos, ComponentSelectorTag> entry : taggedPositions.entrySet()) {
            out.taggedPositions.put(MiscUtils.rotateYCCW(entry.getKey()), entry.getValue());
        }

        return out;
    }

    @Override
    public TaggedPositionBlockArray rotateUp() {
        TaggedPositionBlockArray out = new TaggedPositionBlockArray(traitNum);

        Map<BlockPos, BlockInformation> outPattern = out.pattern;
        for (BlockPos pos : pattern.keySet()) {
            outPattern.put(MiscUtils.rotateUp(pos), pattern.get(pos).copy());
        }
        for (BlockPos pos : taggedPositions.keySet()) {
            out.taggedPositions.put(MiscUtils.rotateUp(pos), taggedPositions.get(pos));
        }

        return out;
    }

    @Override
    public TaggedPositionBlockArray rotateDown() {
        TaggedPositionBlockArray out = new TaggedPositionBlockArray(traitNum);

        Map<BlockPos, BlockInformation> outPattern = out.pattern;
        for (BlockPos pos : pattern.keySet()) {
            outPattern.put(MiscUtils.rotateDown(pos), pattern.get(pos).copy());
        }
        for (BlockPos pos : taggedPositions.keySet()) {
            out.taggedPositions.put(MiscUtils.rotateDown(pos), taggedPositions.get(pos));
        }

        return out;
    }
}
