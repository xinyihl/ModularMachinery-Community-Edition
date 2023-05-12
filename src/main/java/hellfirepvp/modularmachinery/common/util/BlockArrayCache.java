package hellfirepvp.modularmachinery.common.util;

import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.machine.TaggedPositionBlockArray;
import hellfirepvp.modularmachinery.common.modifier.MultiBlockModifierReplacement;
import io.netty.util.collection.LongObjectHashMap;
import net.minecraft.util.EnumFacing;

import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class BlockArrayCache {
    private static final LongObjectHashMap<EnumMap<EnumFacing, BlockArray>>
            BLOCK_ARRAY_CACHE_MAP = new LongObjectHashMap<>();

    private static final AtomicLong TRAIT_NUM_COUNTER = new AtomicLong(0);

    public static TaggedPositionBlockArray getBlockArrayCache(TaggedPositionBlockArray blockArray, EnumFacing facing) {
        return (TaggedPositionBlockArray) BLOCK_ARRAY_CACHE_MAP.computeIfAbsent(
                blockArray.traitNum, e -> new EnumMap<>(EnumFacing.class)).get(facing);
    }

    public static BlockArray getBlockArrayCache(BlockArray blockArray, EnumFacing facing) {
        return BLOCK_ARRAY_CACHE_MAP.computeIfAbsent(
                blockArray.traitNum, e -> new EnumMap<>(EnumFacing.class)).get(facing);
    }

    public static synchronized void addBlockArrayCache(TaggedPositionBlockArray blockArray, EnumFacing facing) {
        BLOCK_ARRAY_CACHE_MAP.computeIfAbsent(
                blockArray.traitNum, e -> new EnumMap<>(EnumFacing.class)).put(facing, blockArray);
    }

    public static synchronized void addBlockArrayCache(BlockArray blockArray, EnumFacing facing) {
        BLOCK_ARRAY_CACHE_MAP.computeIfAbsent(
                blockArray.traitNum, e -> new EnumMap<>(EnumFacing.class)).put(facing, blockArray);
    }

    public static long nextTraitNum() {
        return TRAIT_NUM_COUNTER.getAndIncrement();
    }

    public static void buildCache(Collection<DynamicMachine> machines) {
        BLOCK_ARRAY_CACHE_MAP.clear();

        long start = System.currentTimeMillis();
        ModularMachinery.log.info("Building Machine Structure Cache...");

        machines.parallelStream().forEach((machine -> {
            TaggedPositionBlockArray blockArray = machine.getPattern();
            EnumFacing facing = EnumFacing.NORTH;
            addBlockArrayCache(blockArray, facing);
            do {
                facing = facing.rotateYCCW();
                blockArray = blockArray.rotateYCCW();
                addBlockArrayCache(blockArray, facing);
            } while (facing != EnumFacing.NORTH);

            buildMultiBlockModifierCache(machine.getMultiBlockModifiers());
        }));

        ModularMachinery.log.info("Build Completed, Used " + (System.currentTimeMillis() - start) + "ms.");
    }

    private static void buildMultiBlockModifierCache(List<MultiBlockModifierReplacement> replacementList) {
        for (MultiBlockModifierReplacement replacement : replacementList) {
            BlockArray blockArray = replacement.getBlockArray();
            EnumFacing facing = EnumFacing.NORTH;
            addBlockArrayCache(blockArray, facing);
            do {
                facing = facing.rotateYCCW();
                blockArray = blockArray.rotateYCCW();
                addBlockArrayCache(blockArray, facing);
            } while (facing != EnumFacing.NORTH);
        }
    }
}
