package github.kasuminova.mmce.common.block.appeng;

import github.kasuminova.mmce.common.tile.base.MEItemBus;
import hellfirepvp.modularmachinery.common.util.IOInventory;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public abstract class BlockMEItemBus extends BlockMEMachineComponent {

    @Override
    public void breakBlock(World worldIn, @Nonnull BlockPos pos, @Nonnull IBlockState state) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof MEItemBus) {
            IOInventory inv = ((MEItemBus) te).getInternalInventory();
            for (int i = 0; i < inv.getSlots(); i++) {
                ItemStack stack = inv.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    spawnAsEntity(worldIn, pos, stack);
                    inv.setStackInSlot(i, ItemStack.EMPTY);
                }
            }
        }
        super.breakBlock(worldIn, pos, state);
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
