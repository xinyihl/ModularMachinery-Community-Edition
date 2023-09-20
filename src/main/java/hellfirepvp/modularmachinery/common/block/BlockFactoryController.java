package hellfirepvp.modularmachinery.common.block;

import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.CommonProxy;
import hellfirepvp.modularmachinery.common.data.Config;
import hellfirepvp.modularmachinery.common.item.ItemDynamicColor;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.tiles.TileFactoryController;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

import static hellfirepvp.modularmachinery.common.block.BlockController.FACING;

public class BlockFactoryController extends BlockMachineComponent implements ItemDynamicColor {
    public static final Map<DynamicMachine, BlockFactoryController> FACOTRY_CONTROLLERS = new HashMap<>();

    protected DynamicMachine parentMachine = null;

    public BlockFactoryController() {
        super(Material.IRON);
        setHardness(5F);
        setResistance(10F);
        setSoundType(SoundType.METAL);
        setHarvestLevel("pickaxe", 1);
        setCreativeTab(CommonProxy.creativeTabModularMachinery);
        setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
    }

    public BlockFactoryController(DynamicMachine parentMachine) {
        this();
        this.parentMachine = parentMachine;
        setRegistryName(new ResourceLocation(
                ModularMachinery.MODID, parentMachine.getRegistryName().getPath() + "_factory_controller")
        );
    }

    public static BlockFactoryController getControllerWithMachine(DynamicMachine machine) {
        return FACOTRY_CONTROLLERS.get(machine);
    }

    public DynamicMachine getParentMachine() {
        return parentMachine;
    }

    @Override
    public int getColorFromItemstack(ItemStack stack, int tintIndex) {
        if (parentMachine == null) return Config.machineColor;
        return parentMachine.getMachineColor();
    }

    @Override
    public int getColorMultiplier(IBlockState state, @Nullable IBlockAccess worldIn, @Nullable BlockPos pos, int tintIndex) {
        if (parentMachine == null) return Config.machineColor;
        return parentMachine.getMachineColor();
    }

    @Override
    public boolean canConnectRedstone(@Nonnull IBlockState state, @Nonnull IBlockAccess world, @Nonnull BlockPos pos, @Nullable EnumFacing side) {
        return true;
    }

    @Nonnull
    @Override
    public EnumBlockRenderType getRenderType(@Nonnull IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }

    @Nonnull
    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getStateForPlacement(@Nonnull World worldIn, @Nonnull BlockPos pos, @Nonnull EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
        return this.getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
    }

    @Override
    public boolean onBlockActivated(World worldIn, @Nonnull BlockPos pos, @Nonnull IBlockState state, @Nonnull EntityPlayer playerIn, @Nonnull EnumHand hand, @Nonnull EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!worldIn.isRemote) {
            TileEntity te = worldIn.getTileEntity(pos);
            if (te instanceof TileFactoryController) {
                playerIn.openGui(ModularMachinery.MODID, CommonProxy.GuiType.FACTORY.ordinal(), worldIn, pos.getX(), pos.getY(), pos.getZ());
            }
        }
        return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean hasComparatorInputOverride(@Nonnull IBlockState state) {
        return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getComparatorInputOverride(@Nonnull IBlockState blockState, World worldIn, @Nonnull BlockPos pos) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof final TileMultiblockMachineController ctrl) {
            return ctrl.isWorking() ? 15 : ctrl.getFoundMachine() != null ? 1 : 0;
        }
        return 0;
    }

    @Nonnull
    @Override
    @SideOnly(Side.CLIENT)
    public String getLocalizedName() {
        if (parentMachine != null) {
            return I18n.format("tile.modularmachinery.machinefactorycontroller.name", parentMachine.getLocalizedName());
        }
        return I18n.format("tile.modularmachinery.blockfactorycontroller.name");
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getHorizontalIndex();
    }

    @Nonnull
    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta));
    }

    @Nonnull
    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING);
    }

    @Nonnull
    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Nonnull
    @Override
    @SuppressWarnings("deprecation")
    public IBlockState withRotation(IBlockState state, Rotation rot) {
        return state.withProperty(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileFactoryController(state);
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileFactoryController(getStateFromMeta(meta));
    }

}
