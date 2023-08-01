package github.kasuminova.mmce.common.block.appeng;

import hellfirepvp.modularmachinery.common.CommonProxy;
import hellfirepvp.modularmachinery.common.block.BlockMachineComponent;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class BlockMEMachineComponent extends BlockMachineComponent {
    public BlockMEMachineComponent() {
        super(Material.IRON);
        setHardness(2F);
        setResistance(10F);
        setSoundType(SoundType.METAL);
        setHarvestLevel("pickaxe", 1);
        setCreativeTab(CommonProxy.creativeTabModularMachinery);
    }

    @Override
    public abstract boolean onBlockActivated(
            @Nonnull World worldIn, @Nonnull BlockPos pos, @Nonnull IBlockState state, @Nonnull EntityPlayer playerIn,
            @Nonnull EnumHand hand, @Nonnull EnumFacing facing,
            float hitX, float hitY, float hitZ);

    @Nullable
    @Override
    public abstract TileEntity createTileEntity(World world, IBlockState state);

    @Nonnull
    @Override
    public EnumBlockRenderType getRenderType(@Nonnull IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }

    @Nonnull
    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }
}
