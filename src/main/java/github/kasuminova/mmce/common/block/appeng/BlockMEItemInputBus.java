package github.kasuminova.mmce.common.block.appeng;

import github.kasuminova.mmce.common.tile.MEItemInputBus;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.CommonProxy;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BlockMEItemInputBus extends BlockMEItemBus {
    @Override
    public boolean onBlockActivated(
            @Nonnull World worldIn, @Nonnull BlockPos pos, @Nonnull IBlockState state, @Nonnull EntityPlayer playerIn,
            @Nonnull EnumHand hand, @Nonnull EnumFacing facing,
            float hitX, float hitY, float hitZ) {
        if (!worldIn.isRemote) {
            TileEntity te = worldIn.getTileEntity(pos);
            if (te instanceof MEItemInputBus) {
                playerIn.openGui(ModularMachinery.MODID, CommonProxy.GuiType.ME_ITEM_INPUT_BUS.ordinal(), worldIn, pos.getX(), pos.getY(), pos.getZ());
            }
        }
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(final World world, final IBlockState state) {
        return new MEItemInputBus();
    }

}
