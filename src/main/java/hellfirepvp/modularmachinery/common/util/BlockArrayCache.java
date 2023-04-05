package hellfirepvp.modularmachinery.common.util;

import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.machine.TaggedPositionBlockArray;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.util.EnumFacing;

import java.util.Collection;
import java.util.EnumMap;
import java.util.concurrent.atomic.AtomicLong;

public class BlockArrayCache {
    private static final Long2ObjectOpenHashMap<EnumMap<EnumFacing, TaggedPositionBlockArray>>
            BLOCK_ARRAY_CACHE_MAP = new Long2ObjectOpenHashMap<>();
    private static final AtomicLong TRAIT_NUM_COUNTER = new AtomicLong(0);

    public static TaggedPositionBlockArray getBlockArrayCache(long traitNum, EnumFacing facing) {
        return BLOCK_ARRAY_CACHE_MAP.computeIfAbsent(traitNum, e -> new EnumMap<>(EnumFacing.class))
                .get(facing);
    }

    public static synchronized void addBlockArrayCache(TaggedPositionBlockArray blockArray, EnumFacing facing) {
        BLOCK_ARRAY_CACHE_MAP.computeIfAbsent(blockArray.traitNum, e -> new EnumMap<>(EnumFacing.class))
                .put(facing, blockArray);
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
        }));
        ModularMachinery.log.info("Build Completed, Used " + (System.currentTimeMillis() - start) + "ms.");
    }
}
