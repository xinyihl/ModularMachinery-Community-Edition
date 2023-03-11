package ink.ikx.mmce.core;

import hellfirepvp.modularmachinery.common.block.BlockController;
import hellfirepvp.modularmachinery.common.lib.ItemsMM;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.tiles.TileMachineController;
import hellfirepvp.modularmachinery.common.util.BlockArray;
import ink.ikx.mmce.common.assembly.MachineAssembly;
import ink.ikx.mmce.common.assembly.MachineAssemblyManager;
import ink.ikx.mmce.common.utils.MiscUtils;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Set;

public class AssemblyEventHandler {

    public static AssemblyEventHandler INSTANCE = new AssemblyEventHandler();

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        World world = event.getWorld();
        BlockPos blockPos = event.getPos();
        ItemStack stack = event.getItemStack();
        EntityPlayer player = event.getEntityPlayer();
        TileEntity tileEntity = world.getTileEntity(blockPos);
        Block block = world.getBlockState(blockPos).getBlock();

        if (world.isRemote) return;

        Item item = Item.getByNameOrId(AssemblyConfig.itemName);
        if (item == null) item = Items.STICK;

        if (tileEntity instanceof TileMachineController && !player.isSneaking()) {
            TileMachineController controller = (TileMachineController) tileEntity;
            if (stack.getItem().equals(ItemsMM.blueprint)) {
                if (getBlueprint(controller).isEmpty()) {
                    ItemStack copy = stack.copy();
                    copy.setCount(1);
                    if (isPlayerNotCreative(player)) stack.setCount(stack.getCount() - 1);
                    controller.getInventory().setStackInSlot(TileMachineController.BLUEPRINT_SLOT, copy);
                }
                event.setCanceled(true);
            } else if (stack.isItemEqual(new ItemStack(item, 1, AssemblyConfig.itemMeta))) {
                DynamicMachine machine = controller.getParentMachine() != null
                        ? controller.getParentMachine()
                        : controller.getBlueprintMachine();

                assemblyBefore(machine, player, blockPos);
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        EntityPlayer player = event.player;
        World world = player.world;
        if (event.phase == TickEvent.Phase.START || event.side.isClient() || world.getWorldTime() % AssemblyConfig.tickBlock != 0)
            return;

        Set<MachineAssembly> machineAssemblyListFromPlayer = MachineAssemblyManager.getMachineAssemblyListFromPlayer(player);

        if (MiscUtils.isNotEmpty(machineAssemblyListFromPlayer)) {
            machineAssemblyListFromPlayer.stream().filter(MachineAssembly::isFilter).forEach(MachineAssembly::assembly);
        }
    }

    private boolean assemblyBefore(DynamicMachine machine, EntityPlayer player, BlockPos pos) {
        if (machine == null) {
            player.sendMessage(MiscUtils.translate(1));
            return false;
        }

        EnumFacing controllerFacing = player.world.getBlockState(pos).getValue(BlockController.FACING);
        BlockArray blockArray = hellfirepvp.modularmachinery.common.util.MiscUtils.rotateYCCWNorthUntil(machine.getPattern(), controllerFacing);
        MachineAssembly Machine = new MachineAssembly(pos, player, blockArray.getPattern());

        if (MachineAssemblyManager.checkMachineExist(Machine)) {
            player.sendMessage(MiscUtils.translate(2));
            return false;
        }
        if (isPlayerNotCreative(player)) {
            if (!Machine.isAllItemsContains() && AssemblyConfig.needAllBlocks) {
                player.sendMessage(MiscUtils.translate(3));
                return false;
            } else {
                MachineAssemblyManager.addMachineAssembly(Machine);
            }
        } else {
            Machine.buildWithCreative();
        }
        return true;
    }

    private ItemStack getBlueprint(TileMachineController controller) {
        return controller.getInventory().getStackInSlot(TileMachineController.BLUEPRINT_SLOT);
    }

    private boolean isPlayerNotCreative(EntityPlayer player) {
        return !player.isCreative();
    }

}
