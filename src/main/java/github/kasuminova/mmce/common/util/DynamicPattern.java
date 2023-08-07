package github.kasuminova.mmce.common.util;

import github.kasuminova.mmce.common.helper.IDynamicPatternInfo;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentSelectorTag;
import hellfirepvp.modularmachinery.common.machine.TaggedPositionBlockArray;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import hellfirepvp.modularmachinery.common.util.BlockArray;
import hellfirepvp.modularmachinery.common.util.BlockArrayCache;
import hellfirepvp.modularmachinery.common.util.MiscUtils;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>动态可扩展结构。</p>
 * <p>TODO: 实现垂直动态结构。</p>
 * <p></p>
 * <p>Dynamic scalable structures.</p>
 * <p>TODO: Realization of vertical dynamic structures.</p>
 */
@SuppressWarnings({"UnusedReturnValue", "unused"})
public class DynamicPattern {
    private final String name;
    private final Set<EnumFacing> faces = EnumSet.noneOf(EnumFacing.class);

    private int minSize;
    private int maxSize;

    private BlockPos structureSizeOffsetStart = new BlockPos(0, 0, 0);
    private BlockPos structureSizeOffset = new BlockPos(0, 0, 0);

//    private TaggedPositionBlockArray patternStart;
    private TaggedPositionBlockArray pattern;
    private TaggedPositionBlockArray patternEnd = null;

    public DynamicPattern(final String name,
                          final TaggedPositionBlockArray pattern,
                          final TaggedPositionBlockArray patternEnd,
                          final int minSize,
                          final int maxSize) {
        this.name = name;
        this.pattern = pattern;
        this.patternEnd = patternEnd;
        this.minSize = minSize;
        this.maxSize = maxSize;
    }

    public DynamicPattern(final String name) {
        this.name = name;
        this.pattern = new TaggedPositionBlockArray();
        this.minSize = 0;
        this.maxSize = 1;
    }

    public MatchResult matches(final TileMultiblockMachineController ctrl,
                               final boolean oldState,
                               final EnumFacing ctrlFace) {
        BlockPos ctrlPos = ctrl.getPos();
        Set<EnumFacing> faces = getTrueFacing(ctrlFace);

        Map<EnumFacing, Integer> matchResults = new EnumMap<>(EnumFacing.class);

        BlockPos facingOffset = ctrlPos.add(getStructureSizeOffsetStart(ctrlFace));

        for (final EnumFacing face : faces) {
            TaggedPositionBlockArray pattern = BlockArrayCache.getBlockArrayCache(this.pattern, ctrlFace);
            TaggedPositionBlockArray patternEnd = null;
            if (this.patternEnd != null) {
                patternEnd = BlockArrayCache.getBlockArrayCache(this.patternEnd, ctrlFace);
            }
//            if (face == EnumFacing.UP || face == EnumFacing.DOWN) {
//                pattern = BlockArrayCache.getHorizontalBlockArrayCache(this.pattern, face, ctrlFace);
//                if (this.patternEnd != null) {
//                    patternEnd = BlockArrayCache.getHorizontalBlockArrayCache(this.patternEnd, face, ctrlFace);
//                }
//            } else {
//                pattern = BlockArrayCache.getBlockArrayCache(this.pattern, ctrlFace);
//                if (this.patternEnd != null) {
//                    patternEnd = BlockArrayCache.getBlockArrayCache(this.patternEnd, ctrlFace);
//                }
//            }

            int size = matchesSize(ctrl, oldState, ctrlFace, facingOffset, pattern, patternEnd);
            if (size >= minSize) {
                matchResults.put(face, size);
            }
        }

        if (matchResults.isEmpty()) {
            return new MatchResult(0, null);
        }

        // Return the maximum size.
        return matchResults.entrySet()
                .stream()
                .max(Comparator.comparingInt(Map.Entry::getValue))
                .map(entry -> new MatchResult(entry.getValue(), entry.getKey()))
                .orElseGet(() -> new MatchResult(0, null));
    }

    private int matchesSize(final TileMultiblockMachineController ctrl,
                            final boolean oldState,
                            final EnumFacing face,
                            final BlockPos facingOffset,
                            final TaggedPositionBlockArray pattern,
                            @Nullable final TaggedPositionBlockArray patternEnd)
    {
        BlockPos offset = facingOffset;
        boolean first = true;
        int size = 0;
        for (;;) {
            if (!first) {
                offset = offset.add(getStructureSizeOffset(face));
            } else {
                first = false;
            }

            if (pattern.matches(ctrl.getWorld(), offset, oldState, null)) {
                size++;
                if (size > maxSize) {
                    return 0;
                }
            } else {
                if (patternEnd != null && !patternEnd.matches(ctrl.getWorld(), offset, oldState, null)) {
                    return 0;
                }
                break;
            }
        }
        return size;
    }

    public void addPatternToBlockArray(BlockArray toAdd, int maxSize, EnumFacing patternOffset, EnumFacing facing) {
        BlockPos offset = getStructureSizeOffsetStart(facing);

        TaggedPositionBlockArray pattern = BlockArrayCache.getBlockArrayCache(this.pattern, facing);
//        if (patternOffset == EnumFacing.UP || patternOffset == EnumFacing.DOWN) {
//            pattern = BlockArrayCache.getHorizontalBlockArrayCache(this.pattern, patternOffset, facing);
//        } else {
//            pattern = BlockArrayCache.getBlockArrayCache(this.pattern, facing);
//        }

        boolean first = true;
        for (int i = 0; i < maxSize; i++) {
            if (!first) {
                offset = offset.add(getStructureSizeOffset(facing));
            } else {
                first = false;
            }

            final BlockPos finalOffset = offset;
            pattern.getPattern().forEach((pos, info) -> toAdd.addBlock(pos.add(finalOffset), info));

            if (toAdd instanceof TaggedPositionBlockArray) {
                final int patternIndex = i;
                // DynamicComponentSelectorTag
                pattern.getTaggedPositions().forEach((pos, tag) -> ((TaggedPositionBlockArray) toAdd).setTag(
                        pos, new ComponentSelectorTag(String.format("%s_%s_%d", tag.getTag(), name, patternIndex))));
            }
        }

        if (patternEnd != null) {
            BlockArray patternEnd = BlockArrayCache.getBlockArrayCache(this.patternEnd, facing);
//            if (patternOffset == EnumFacing.UP || patternOffset == EnumFacing.DOWN) {
//                patternEnd = BlockArrayCache.getHorizontalBlockArrayCache(this.patternEnd, patternOffset, facing);
//            } else {
//                patternEnd = BlockArrayCache.getBlockArrayCache(this.patternEnd, facing);
//            }

            final BlockPos finalOffset = offset.add(getStructureSizeOffset(facing));
            patternEnd.getPattern().forEach((pos, info) -> toAdd.addBlock(pos.add(finalOffset), info));

            if (toAdd instanceof TaggedPositionBlockArray) {
                // DynamicComponentSelectorTag
                pattern.getTaggedPositions().forEach((pos, tag) -> ((TaggedPositionBlockArray) toAdd).setTag(
                        pos, new ComponentSelectorTag(String.format("%s_%s_end", tag.getTag(), name))));
            }
        }
    }

    private Set<EnumFacing> getTrueFacing(final EnumFacing facingOffset) {
        Set<EnumFacing> faces = this.faces;

        if (facingOffset != EnumFacing.NORTH) {
            faces = faces.stream().map(face -> {
                if (face == EnumFacing.UP || face == EnumFacing.DOWN) {
                    return face;
                }

                EnumFacing facing = facingOffset;
                EnumFacing rotated = face;

                while (facing != EnumFacing.NORTH) {
                    facing = facing.rotateYCCW();
                    rotated = rotated.rotateYCCW();
                }

                return rotated;
            }).collect(Collectors.toSet());
        }

        return faces;
    }

    public String getName() {
        return name;
    }

    public BlockPos getStructureSizeOffsetStart() {
        return structureSizeOffsetStart;
    }

    public BlockPos getStructureSizeOffsetStart(EnumFacing facingOffset) {
        return rotatePosTo(facingOffset, structureSizeOffsetStart);
    }

    public DynamicPattern setStructureSizeOffsetStart(final BlockPos structureSizeOffsetStart) {
        this.structureSizeOffsetStart = structureSizeOffsetStart;
        return this;
    }

    public BlockPos getStructureSizeOffset() {
        return structureSizeOffset;
    }

    public BlockPos getStructureSizeOffset(EnumFacing facingOffset) {
        return rotatePosTo(facingOffset, structureSizeOffset);
    }

    private static BlockPos rotatePosTo(final EnumFacing target, final BlockPos pos) {
        if (target == EnumFacing.NORTH) {
            return pos;
        }

        EnumFacing facing = EnumFacing.NORTH;
        BlockPos rotated = pos;
        while (facing != target) {
            facing = facing.rotateYCCW();
            rotated = MiscUtils.rotateYCCW(rotated);
        }

        return rotated;
    }

    public DynamicPattern setStructureSizeOffset(final BlockPos structureSizeOffset) {
        this.structureSizeOffset = structureSizeOffset;
        return this;
    }

    public Set<EnumFacing> getFaces() {
        return faces;
    }

    public DynamicPattern addFaces(final Set<EnumFacing> faces) {
        this.faces.addAll(faces);
        return this;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public DynamicPattern setMaxSize(final int maxSize) {
        this.maxSize = maxSize;
        return this;
    }

    public int getMinSize() {
        return minSize;
    }

    public DynamicPattern setMinSize(final int minSize) {
        this.minSize = minSize;
        return this;
    }

    public TaggedPositionBlockArray getPattern() {
        return pattern;
    }

    public TaggedPositionBlockArray getPattern(EnumFacing patternOffset, EnumFacing facingOffset) {
        if (patternOffset == EnumFacing.UP || patternOffset == EnumFacing.DOWN) {
            return BlockArrayCache.getHorizontalBlockArrayCache(pattern, patternOffset, facingOffset);
        } else {
            return BlockArrayCache.getBlockArrayCache(pattern, facingOffset);
        }
    }

    public DynamicPattern setPattern(final TaggedPositionBlockArray pattern) {
        this.pattern = pattern;
        return this;
    }

    public TaggedPositionBlockArray getPatternEnd() {
        return patternEnd;
    }

    public TaggedPositionBlockArray getPatternEnd(EnumFacing patternOffset, EnumFacing facingOffset) {
        if (patternEnd == null) {
            return null;
        }

        if (patternOffset == EnumFacing.UP || patternOffset == EnumFacing.DOWN) {
            return BlockArrayCache.getHorizontalBlockArrayCache(patternEnd, patternOffset, facingOffset);
        } else {
            return BlockArrayCache.getBlockArrayCache(patternEnd, patternOffset);
        }
    }

    public DynamicPattern setPatternEnd(final TaggedPositionBlockArray patternEnd) {
        this.patternEnd = patternEnd;
        return this;
    }

    public static class Status implements IDynamicPatternInfo {
        private final DynamicPattern pattern;
        private final EnumFacing matchFacing;
        private final int size;

        public Status(final DynamicPattern pattern, final EnumFacing matchFacing, final int size) {
            this.pattern = pattern;
            this.matchFacing = matchFacing;
            this.size = size;
        }

        @Override
        public DynamicPattern getPattern() {
            return pattern;
        }

        @Override
        public EnumFacing getMatchFacing() {
            return matchFacing;
        }

        @Override
        public String getFacing() {
            return matchFacing.name();
        }

        @Override
        public String getPatternName() {
            return pattern.name;
        }

        @Override
        public int getSize() {
            return size;
        }

        @Override
        public int getMinSize() {
            return pattern.minSize;
        }

        @Override
        public int getMaxSize() {
            return pattern.maxSize;
        }
    }

    public static class MatchResult {
        private final int size;
        private final EnumFacing matchFacing;

        public MatchResult(final int size, final EnumFacing matchFacing) {
            this.size = size;
            this.matchFacing = matchFacing;
        }

        /**
         * @return the dynamic structure size, may be 0.
         */
        public int getSize() {
            return size;
        }

        public EnumFacing getMatchFacing() {
            return matchFacing;
        }

        public boolean isMatched() {
            return matchFacing != null;
        }
    }
}
