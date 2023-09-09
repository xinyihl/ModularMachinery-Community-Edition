package hellfirepvp.modularmachinery.common.util;

import github.kasuminova.mmce.common.util.DynamicPattern;
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
    private static final LongObjectHashMap<EnumMap<EnumFacing, EnumMap<EnumFacing, BlockArray>>>
            HORIZONTAL_BLOCK_ARRAY_CACHE_MAP = new LongObjectHashMap<>();

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

    public static synchronized void addHorizontalBlockArrayCache(final TaggedPositionBlockArray blockArray,
                                                                 final EnumFacing upDown,
                                                                 final EnumFacing facing)
    {
        HORIZONTAL_BLOCK_ARRAY_CACHE_MAP.computeIfAbsent(
                blockArray.traitNum, e -> new EnumMap<>(EnumFacing.class)).computeIfAbsent(
                        upDown, e -> new EnumMap<>(EnumFacing.class)).put(facing, blockArray);
    }

    public static TaggedPositionBlockArray getHorizontalBlockArrayCache(final TaggedPositionBlockArray blockArray,
                                                                        final EnumFacing upDown,
                                                                        final EnumFacing facing)
    {
        return (TaggedPositionBlockArray) HORIZONTAL_BLOCK_ARRAY_CACHE_MAP.computeIfAbsent(
                blockArray.traitNum, e -> new EnumMap<>(EnumFacing.class)).computeIfAbsent(
                        upDown, e -> new EnumMap<>(EnumFacing.class)).get(facing);
    }

    public static long nextTraitNum() {
        return TRAIT_NUM_COUNTER.getAndIncrement();
    }

    public static void buildCache(Collection<DynamicMachine> machines) {
        BLOCK_ARRAY_CACHE_MAP.clear();
        HORIZONTAL_BLOCK_ARRAY_CACHE_MAP.clear();

        long start = System.currentTimeMillis();
        ModularMachinery.log.info("Building Machine Structure Cache...");

        machines.parallelStream().forEach((machine -> {
            TaggedPositionBlockArray blockArray = machine.getPattern();
            buildBlockArrayCache(blockArray);
            buildMultiBlockModifierCache(machine.getMultiBlockModifiers());

            for (final DynamicPattern pattern : machine.getDynamicPatterns().values()) {
                buildDynamicPatternCache(pattern.getPattern());

                TaggedPositionBlockArray patternEnd = pattern.getPatternEnd();
                if (patternEnd != null) {
                    buildDynamicPatternCache(patternEnd);
                }
            }
        }));

        ModularMachinery.log.info("Build Completed, Used " + (System.currentTimeMillis() - start) + "ms.");
    }

    private static void buildMultiBlockModifierCache(List<MultiBlockModifierReplacement> replacementList) {
        for (MultiBlockModifierReplacement replacement : replacementList) {
            BlockArray blockArray = replacement.getBlockArray();
            buildBlockArrayCache(blockArray);
        }
    }

    private static void buildBlockArrayCache(BlockArray blockArray) {
        EnumFacing facing = EnumFacing.NORTH;
        blockArray.flushTileBlocksCache();
        addBlockArrayCache(blockArray, facing);
        do {
            facing = facing.rotateYCCW();
            blockArray = blockArray.rotateYCCW();
            blockArray.flushTileBlocksCache();
            addBlockArrayCache(blockArray, facing);
        } while (facing != EnumFacing.NORTH);
    }

    private static void buildDynamicPatternCache(TaggedPositionBlockArray blockArray) {
        blockArray.flushTileBlocksCache();

        for (final EnumFacing facing : EnumFacing.values()) {
            BlockArray rotated = blockArray;
            EnumFacing rotatedFacing = EnumFacing.NORTH;
            if (facing == EnumFacing.UP || facing == EnumFacing.DOWN) {
//                buildHorizontalPatternCache(blockArray, facing);
                continue;
            }

            while (rotatedFacing != facing) {
                rotatedFacing = rotatedFacing.rotateYCCW();
                rotated = rotated.rotateYCCW();
                rotated.flushTileBlocksCache();
            }

            addBlockArrayCache(rotated, rotatedFacing);
        }
    }

    private static void buildHorizontalPatternCache(TaggedPositionBlockArray blockArray, EnumFacing upDown) {
        EnumFacing facing = EnumFacing.NORTH;
        TaggedPositionBlockArray rotated = blockArray;
        TaggedPositionBlockArray upDownRotated;

        switch (upDown) {
            case UP:
                do {
                    facing = facing.rotateYCCW();
                    rotated = rotated.rotateYCCW();
                    upDownRotated = rotated.rotateUp();
                    upDownRotated.flushTileBlocksCache();
                    addHorizontalBlockArrayCache(upDownRotated, upDown, facing);
                } while (facing != EnumFacing.NORTH);
                break;
            case DOWN:
                do {
                    facing = facing.rotateYCCW();
                    rotated = rotated.rotateYCCW();
                    upDownRotated = rotated.rotateDown();
                    upDownRotated.flushTileBlocksCache();
                    addHorizontalBlockArrayCache(upDownRotated, upDown, facing);
                } while (facing != EnumFacing.NORTH);
                break;
        }
    }

}
