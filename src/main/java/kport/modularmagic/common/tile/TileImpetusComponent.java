package kport.modularmagic.common.tile;

import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.tiles.base.MachineComponentTile;
import hellfirepvp.modularmachinery.common.tiles.base.TileColorableMachineComponent;
import kport.modularmagic.common.tile.machinecomponent.MachineComponentImpetus;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import thaumcraft.api.aspects.Aspect;
import thecodex6824.thaumicaugmentation.ThaumicAugmentation;
import thecodex6824.thaumicaugmentation.api.impetus.node.*;
import thecodex6824.thaumicaugmentation.api.impetus.node.prefab.ImpetusNode;
import thecodex6824.thaumicaugmentation.api.impetus.node.prefab.SimpleImpetusConsumer;
import thecodex6824.thaumicaugmentation.api.util.DimensionalBlockPos;

import javax.annotation.Nullable;

/**
 * @author youyihj
 */
public abstract class TileImpetusComponent extends TileColorableMachineComponent implements MachineComponentTile {
    public static final int CAPACITY = 1000;
    protected int impetus = 0;
    protected ImpetusNode node;

    @Override
    public void readCustomNBT(NBTTagCompound compound) {
        super.readCustomNBT(compound);
        this.impetus = compound.getInteger("impetus");
        node.deserializeNBT(compound.getCompoundTag("node"));
    }

    @Override
    public void writeCustomNBT(NBTTagCompound compound) {
        super.writeCustomNBT(compound);
        compound.setInteger("impetus", impetus);
        compound.setTag("node", node.serializeNBT());
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityImpetusNode.IMPETUS_NODE) {
            return true;
        }
        return super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityImpetusNode.IMPETUS_NODE) {
            return CapabilityImpetusNode.IMPETUS_NODE.cast(this.node);
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public void invalidate() {
        if (!this.world.isRemote) {
            NodeHelper.syncDestroyedImpetusNode(this.node);
        }

        this.node.destroy();
        ThaumicAugmentation.proxy.deregisterRenderableImpetusNode(this.node);
        super.invalidate();
    }

    @Override
    public void setWorld(World worldIn) {
        super.setWorld(worldIn);
        node.setLocation(new DimensionalBlockPos(pos.toImmutable(), worldIn.provider.getDimension()));
    }

    @Override
    public void setPos(BlockPos posIn) {
        super.setPos(posIn);
        if (world != null)
            node.setLocation(new DimensionalBlockPos(posIn.toImmutable(), world.provider.getDimension()));
    }

    @Override
    public void onLoad() {
        node.init(world);
        ThaumicAugmentation.proxy.registerRenderableImpetusNode(node);
    }

    @Override
    public void onChunkUnload() {
        node.unload();
        ThaumicAugmentation.proxy.deregisterRenderableImpetusNode(node);
    }

    @Override
    public boolean receiveClientEvent(int id, int type) {
        ThaumicAugmentation.proxy.getRenderHelper().renderSpark(world, pos.getX() + world.rand.nextFloat(),
                pos.getY() + world.rand.nextFloat(), pos.getZ() + world.rand.nextFloat(), 1.5F, Aspect.ELDRITCH.getColor(), false);

        return true;
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }

    public static class Input extends TileImpetusComponent implements ITickable {

        public Input() {
            this.node = new SimpleImpetusConsumer(1, 0) {
                @Override
                public Vec3d getBeamEndpoint() {
                    return new Vec3d(pos).add(0.5f, 0.5f, 0.5f);
                }
            };
        }

        public boolean hasEnoughImpetus(int amount) {
            return impetus >= amount;
        }

        public void consumeImpetus(int amount) {
            if (hasEnoughImpetus(amount)) {
                impetus -= amount;
            }
        }

        @Override
        public void update() {
            if (world.isRemote || impetus >= CAPACITY)
                return;
            ConsumeResult result = ((IImpetusConsumer) node).consume(CAPACITY - impetus, false);
            if (result.energyConsumed > 0) {
                impetus += result.energyConsumed;
                this.markDirty();
            }
        }

        @Nullable
        @Override
        public MachineComponent<?> provideComponent() {
            return new MachineComponentImpetus(IOType.INPUT, this);
        }
    }

    public static class Output extends TileImpetusComponent {

        public Output() {
            this.node = new CustomImpetusProvider(0, 2);
        }


        public boolean hasEnoughCapacity(int amount) {
            return this.impetus + amount <= CAPACITY;
        }

        public void supplyImpetus(int amount) {
            if (hasEnoughCapacity(amount)) {
                this.impetus += amount;
            }
        }

        @Nullable
        @Override
        public MachineComponent<?> provideComponent() {
            return new MachineComponentImpetus(IOType.OUTPUT, this);
        }

        private class CustomImpetusProvider extends ImpetusNode implements IImpetusProvider {

            public CustomImpetusProvider(int totalInputs, int totalOutputs) {
                super(totalInputs, totalOutputs);
            }

            @Override
            public long provide(long energy, boolean simulate) {
                long amount = Math.min(impetus, energy);
                if (!simulate) {
                    impetus -= amount;
                    markDirty();
                }
                return amount;
            }

            @Override
            public Vec3d getBeamEndpoint() {
                return new Vec3d(pos).add(0.5, 0.5, 0.5);
            }
        }
    }
}
