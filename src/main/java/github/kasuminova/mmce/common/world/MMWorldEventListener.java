package github.kasuminova.mmce.common.world;

import github.kasuminova.mmce.client.world.BlockModelHider;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.IWorldEventListener;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class MMWorldEventListener implements IWorldEventListener {

    public static final MMWorldEventListener INSTANCE = new MMWorldEventListener();

    private final Map<World, Map<ChunkPos, StructureBoundingBox>> worldChangedChunksLastTick = new HashMap<>();
    private final Map<World, Map<ChunkPos, StructureBoundingBox>> worldChangedChunks = new HashMap<>();

    private MMWorldEventListener() {
    }

    @SubscribeEvent
    public void onWorldLoaded(WorldEvent.Load event) {
        World world = event.getWorld();
        if (world.isRemote) {
            return;
        }
        world.addEventListener(this);
        MachineComponentManager.INSTANCE.addWorld(world);
    }

    @SubscribeEvent
    public void onWorldUnloaded(WorldEvent.Unload event) {
        World world = event.getWorld();
        if (world.isRemote) {
            BlockModelHider.onWorldUnload(world);
            return;
        }
        worldChangedChunks.remove(world);
        worldChangedChunksLastTick.remove(world);
        MachineComponentManager.INSTANCE.removeWorld(world);
    }

    @SubscribeEvent
    public void onServerTickStart(TickEvent.ServerTickEvent event) {
        if (event.side != Side.SERVER || event.phase != TickEvent.Phase.START) {
            return;
        }

        worldChangedChunksLastTick.putAll(worldChangedChunks);
        worldChangedChunks.clear();
    }

    public boolean isAreaChanged(@Nonnull final World worldIn,
                                 @Nonnull final BlockPos min,
                                 @Nonnull final BlockPos max) {
        int minChunkX = min.getX() >> 4;
        int maxChunkX = max.getX() >> 4;
        int minChunkZ = min.getZ() >> 4;
        int maxChunkZ = max.getZ() >> 4;

        StructureBoundingBox structureArea = new StructureBoundingBox(min, max);

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                StructureBoundingBox changedArea = worldChangedChunksLastTick.computeIfAbsent(worldIn, v -> new HashMap<>()).get(new ChunkPos(chunkX, chunkZ));
                if (changedArea != null && changedArea.intersectsWith(structureArea)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void notifyBlockUpdate(@Nonnull final World worldIn,
                                  @Nonnull final BlockPos pos,
                                  @Nonnull final IBlockState oldState,
                                  @Nonnull final IBlockState newState, final int flags) {
        if ((flags != 1 && flags != 3) || oldState == newState) {
            return;
        }

        Map<ChunkPos, StructureBoundingBox> chunkPosHeightSetMap = worldChangedChunks.computeIfAbsent(worldIn, v -> new HashMap<>());
        ChunkPos chunkPos = new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4);
        StructureBoundingBox changedArea = chunkPosHeightSetMap.get(chunkPos);

        if (changedArea == null) {
            chunkPosHeightSetMap.put(chunkPos, new StructureBoundingBox(pos, pos));
        } else {
            changedArea.expandTo(new StructureBoundingBox(pos, pos));
        }
    }

    // Noop

    @Override
    public void notifyLightSet(@Nonnull final BlockPos pos) {

    }

    @Override
    public void markBlockRangeForRenderUpdate(final int x1,
                                              final int y1,
                                              final int z1,
                                              final int x2,
                                              final int y2,
                                              final int z2) {

    }

    @Override
    public void playSoundToAllNearExcept(@Nullable final EntityPlayer player,
                                         @Nonnull final SoundEvent soundIn,
                                         @Nonnull final SoundCategory category,
                                         final double x,
                                         final double y,
                                         final double z,
                                         final float volume,
                                         final float pitch) {

    }

    @Override
    public void playRecord(@Nonnull final SoundEvent soundIn, @Nonnull final BlockPos pos) {

    }

    @Override
    public void spawnParticle(final int particleID,
                              final boolean ignoreRange,
                              final double xCoord,
                              final double yCoord,
                              final double zCoord,
                              final double xSpeed,
                              final double ySpeed,
                              final double zSpeed,
                              @Nonnull final int... parameters) {

    }

    @Override
    public void spawnParticle(final int id,
                              final boolean ignoreRange,
                              final boolean minimiseParticleLevel,
                              final double x,
                              final double y,
                              final double z,
                              final double xSpeed,
                              final double ySpeed,
                              final double zSpeed,
                              @Nonnull final int... parameters) {

    }

    @Override
    public void onEntityAdded(@Nonnull final Entity entityIn) {

    }

    @Override
    public void onEntityRemoved(@Nonnull final Entity entityIn) {

    }

    @Override
    public void broadcastSound(final int soundID, final BlockPos pos, final int data) {

    }

    @Override
    public void playEvent(@Nonnull final EntityPlayer player,
                          final int type,
                          @Nonnull final BlockPos blockPosIn,
                          final int data) {

    }

    @Override
    public void sendBlockBreakProgress(final int breakerId, @Nonnull final BlockPos pos, final int progress) {

    }
}
