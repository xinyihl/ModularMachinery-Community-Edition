package github.kasuminova.mmce.common.block.appeng;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public abstract class BlockMEItemBus extends BlockMEMachineComponent {

    @Override
    public void dropBlockAsItemWithChance(@Nonnull final World worldIn, @Nonnull final BlockPos pos, @Nonnull final IBlockState state, final float chance, final int fortune) {
    }

    //    @Override
//    public boolean hasComparatorInputOverride(@Nonnull IBlockState state) {
//        return true;
//    }
//
//    @Override
//    public int getComparatorInputOverride(@Nonnull IBlockState blockState, World worldIn, @Nonnull BlockPos pos) {
//        return RedstoneHelper.getRedstoneLevel(worldIn.getTileEntity(pos));
//    }
}
