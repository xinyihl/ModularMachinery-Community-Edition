package kport.modularmagic.common.tile;

import com.rwtema.extrautils2.backend.XUBlock;
import com.rwtema.extrautils2.network.XUPacketBuffer;
import com.rwtema.extrautils2.power.IWorldPowerMultiplier;
import com.rwtema.extrautils2.power.PowerManager;
import com.rwtema.extrautils2.tile.TilePower;
import hellfirepvp.modularmachinery.common.data.Config;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.tiles.base.ColorableMachineTile;
import hellfirepvp.modularmachinery.common.tiles.base.MachineComponentTile;
import kport.modularmagic.common.tile.machinecomponent.MachineComponentGridProvider;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class TileGridProvider extends TilePower implements IWorldPowerMultiplier, MachineComponentTile, ITickable, ColorableMachineTile {

    private volatile float power;
    private int tick;
    private int color = Config.machineColor;

    @Override
    public int getMachineColor() {
        return this.color;
    }

    @Override
    public void setMachineColor(int newColor) {
        this.color = newColor;
        IBlockState thisState = world.getBlockState(pos);
        world.notifyBlockUpdate(pos, thisState, thisState, 3);
        markDirty();
    }

    @Override
    public float multiplier(@Nullable World world) {
        return 1.0F;
    }

    @Override
    public void onPowerChanged() {
    }

    @Override
    public float getPower() {
        return this.power;
    }

    public void setPower(float power) {
        this.tick = 2;
        this.power = power;
    }

    public PowerManager.PowerFreq getFreq() {
        return PowerManager.instance.getPowerFreq(this.frequency);
    }

    @Override
    public void update() {
        if (this.tick > 0)
            this.tick--;
        else if (this.power != 0)
            this.power = 0;
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack, XUBlock xuBlock) {
        super.onBlockPlacedBy(worldIn, pos, state, placer, stack, xuBlock);
        PowerManager.instance.addPowerHandler(this);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        this.color = nbt.hasKey("casingColor") ? nbt.getInteger("casingColor") : Config.machineColor;
    }

    @Nonnull
    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setInteger("casingColor", this.color);
        return nbt;
    }

    @Override
    public void addToDescriptionPacket(XUPacketBuffer packet) {
        super.addToDescriptionPacket(packet);
        packet.writeInt(this.color);
    }

    @Override
    public void handleDescriptionPacket(XUPacketBuffer packet) {
        super.handleDescriptionPacket(packet);
        this.color = packet.readInt();
    }

    public static class Input extends TileGridProvider {

        @Override
        public IWorldPowerMultiplier getMultiplier() {
            return this;
        }

        @Nullable
        @Override
        public MachineComponent provideComponent() {
            return new MachineComponentGridProvider(this, IOType.INPUT);
        }
    }

    public static class Output extends TileGridProvider {

        @Override
        public IWorldPowerMultiplier getMultiplier() {
            return this;
        }

        @Nullable
        @Override
        public MachineComponent provideComponent() {
            return new MachineComponentGridProvider(this, IOType.OUTPUT);
        }
    }
}
