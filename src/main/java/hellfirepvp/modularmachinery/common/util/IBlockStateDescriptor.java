package hellfirepvp.modularmachinery.common.util;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraftforge.fluids.BlockFluidBase;

import java.util.List;

public class IBlockStateDescriptor {

    public final List<IBlockState> applicable = new ObjectArrayList<>();

    public IBlockStateDescriptor() {
    }

    public IBlockStateDescriptor(Block block) {
        IntList usedMetas = new IntArrayList();
        if (!(block instanceof BlockLiquid) && !(block instanceof BlockFluidBase)) {
            for (IBlockState state : block.getBlockState().getValidStates()) {
                int meta = block.getMetaFromState(state);
                if (!usedMetas.contains(meta)) {
                    usedMetas.add(meta);
                    this.applicable.add(state);
                }
            }
        }
        if (applicable.isEmpty()) {
            applicable.add(block.getDefaultState());
        }
    }

    public IBlockStateDescriptor(IBlockState state) {
        this.applicable.add(state);
    }
}
