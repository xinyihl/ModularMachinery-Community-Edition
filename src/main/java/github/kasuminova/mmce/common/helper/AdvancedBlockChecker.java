package github.kasuminova.mmce.common.helper;

import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

@FunctionalInterface
public interface AdvancedBlockChecker {
    boolean isMatch(World world, BlockPos pos, IBlockState blockState, NBTTagCompound nbt);
}
