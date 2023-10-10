package kport.modularmagic.common.block;

import kport.modularmagic.common.tile.TileImpetusComponent;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class BlockImpetusProviderOutput extends BlockImpetusProvider {

    //   public BlockImpetuProviderOutput() {
    //       super("blockimpetusprovideroutput");"blockimpetusproviderinput"
    //   }

    public static final BlockImpetusProviderOutput INSTANCE = new BlockImpetusProviderOutput();

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileImpetusComponent.Output();
    }

    @Override
    protected int getIOPresentationColor() {
        return 0xa14e08;
    }
}