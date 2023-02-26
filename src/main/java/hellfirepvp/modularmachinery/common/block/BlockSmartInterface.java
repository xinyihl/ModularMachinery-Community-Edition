package hellfirepvp.modularmachinery.common.block;

import hellfirepvp.modularmachinery.common.CommonProxy;
import hellfirepvp.modularmachinery.common.block.prop.SmartInterfaceType;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.LinkedList;

public class BlockSmartInterface extends BlockMachineComponent implements BlockCustomName, BlockVariants {
    public static final PropertyEnum<EnumFacing> FACING = PropertyEnum.create("facing", EnumFacing.class, EnumFacing.HORIZONTALS);
    protected static final PropertyEnum<SmartInterfaceType> INTERFACE_TYPE = PropertyEnum.create("type", SmartInterfaceType.class);

    public BlockSmartInterface() {
        super(Material.IRON);
        setHardness(5F);
        setResistance(10F);
        setSoundType(SoundType.METAL);
        setHarvestLevel("pickaxe", 1);
        setCreativeTab(CommonProxy.creativeTabModularMachinery);
    }

    @Override
    public void getSubBlocks(CreativeTabs itemIn, NonNullList<ItemStack> items) {
        for (SmartInterfaceType size : SmartInterfaceType.values()) {
            items.add(new ItemStack(this, 1, size.ordinal()));
        }
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    public int damageDropped(IBlockState state) {
        return getMetaFromState(state);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(INTERFACE_TYPE).ordinal();
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, INTERFACE_TYPE);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(INTERFACE_TYPE, SmartInterfaceType.values()[meta]);
    }

    @Override
    public String getIdentifierForMeta(int meta) {
        return getStateFromMeta(meta).getValue(INTERFACE_TYPE).getName();
    }

    @Override
    public Iterable<IBlockState> getValidStates() {
        LinkedList<IBlockState> states = new LinkedList<>();
        for (SmartInterfaceType type : SmartInterfaceType.values()) {
            states.add(getDefaultState().withProperty(INTERFACE_TYPE, type));
        }
        return states;
    }

    @Override
    public String getBlockStateName(IBlockState state) {
        return state.getValue(INTERFACE_TYPE).getName();
    }
}
