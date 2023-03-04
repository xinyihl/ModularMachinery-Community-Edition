package hellfirepvp.modularmachinery.common.util;

import hellfirepvp.modularmachinery.common.machine.TaggedPositionBlockArray;
import net.minecraft.util.EnumFacing;

import java.util.EnumMap;
import java.util.Random;
import java.util.WeakHashMap;

public class BlockArrayCache {
    private static final WeakHashMap<Long, EnumMap<EnumFacing, BlockArray>> CACHE = new WeakHashMap<>();
    private static final WeakHashMap<Long, EnumMap<EnumFacing, TaggedPositionBlockArray>> TAGGED_POSITION_CACHE = new WeakHashMap<>();
    protected static final Random TRAIT_NUM_GENERATOR = new Random();

    public static BlockArray getBlockArrayCache(long traitNum, EnumFacing facing) {
        EnumMap<EnumFacing, BlockArray> blockArrayEnums = CACHE.get(traitNum);
        if (blockArrayEnums == null) {
            return null;
        }
        return blockArrayEnums.get(facing);
    }

    public static void putBlockArrayCache(long traitNum, BlockArray blockArray) {
        CACHE.putIfAbsent(traitNum, new EnumMap<>(EnumFacing.class));
        EnumMap<EnumFacing, BlockArray> blockArrayEnums = CACHE.get(traitNum);
        if (blockArrayEnums != null) {
            blockArrayEnums.put(blockArray.facing, blockArray);
        }
    }

    public static TaggedPositionBlockArray getTaggedPositionBlockArrayCache(long traitNum, EnumFacing facing) {
        EnumMap<EnumFacing, TaggedPositionBlockArray> blockArrayEnums = TAGGED_POSITION_CACHE.get(traitNum);
        if (blockArrayEnums == null) {
            return null;
        }
        return blockArrayEnums.get(facing);
    }

    public static void putTaggedPositionBlockArrayCache(long traitNum, TaggedPositionBlockArray blockArray) {
        TAGGED_POSITION_CACHE.putIfAbsent(traitNum, new EnumMap<>(EnumFacing.class));
        EnumMap<EnumFacing, TaggedPositionBlockArray> blockArrayEnums = TAGGED_POSITION_CACHE.get(traitNum);
        if (blockArrayEnums != null) {
            blockArrayEnums.put(blockArray.facing, blockArray);
        }
    }
}
