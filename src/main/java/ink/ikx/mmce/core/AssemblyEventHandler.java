package ink.ikx.mmce.core;

import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.block.BlockController;
import hellfirepvp.modularmachinery.common.block.BlockFactoryController;
import hellfirepvp.modularmachinery.common.lib.ItemsMM;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import hellfirepvp.modularmachinery.common.util.BlockArrayCache;
import ink.ikx.mmce.common.assembly.MachineAssembly;
import ink.ikx.mmce.common.assembly.MachineAssemblyManager;
import ink.ikx.mmce.common.utils.StructureIngredient;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Collection;

@SuppressWarnings("MethodMayBeStatic")
public class AssemblyEventHandler {
    public static final AssemblyEventHandler INSTANCE = new AssemblyEventHandler();

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

        if (player.isSneaking()) {
            return;
        }

        if (!(tileEntity instanceof final TileMultiblockMachineController controller)) {
            return;
        }

        if (stack.getItem().equals(ItemsMM.blueprint)) {
            if (getBlueprint(controller).isEmpty()) {
                ItemStack copy = stack.copy();
                copy.setCount(1);
                if (!player.isCreative()) stack.setCount(stack.getCount() - 1);
                controller.getInventory().setStackInSlot(TileMultiblockMachineController.BLUEPRINT_SLOT, copy);
            }
            event.setCanceled(true);
        } else if (stack.isItemEqual(new ItemStack(item, 1, AssemblyConfig.itemMeta))) {
            DynamicMachine machine = controller.getBlueprintMachine();
            if (machine == null) {
                if (block instanceof BlockController) {
                    machine = ((BlockController) block).getParentMachine();
                }
                if (block instanceof BlockFactoryController) {
                    machine = ((BlockFactoryController) block).getParentMachine();
                }
            }

            assemblyBefore(machine, player, blockPos);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        EntityPlayer player = event.player;
        World world = player.world;
        if (event.phase == TickEvent.Phase.START || world.isRemote || world.getWorldTime() % AssemblyConfig.tickBlock != 0) {
            return;
        }

        Collection<MachineAssembly> assemblies = MachineAssemblyManager.getMachineAssemblyListFromPlayer(player);

        if (assemblies == null || assemblies.isEmpty()) {
            return;
        }

        ModularMachinery.EXECUTE_MANAGER.addSyncTask(() -> {
            for (final MachineAssembly assembly : assemblies) {
                assembly.assembly(true);
                if (assembly.isCompleted()) {
                    MachineAssemblyManager.removeMachineAssembly(assembly.getCtrlPos());
                    player.sendMessage(new TextComponentTranslation("message.assembly.tip.success"));
                }
            }
        });
    }

    @SubscribeEvent
    public void onPlayerLogOut(PlayerEvent.PlayerLoggedOutEvent event) {
        MachineAssemblyManager.removeMachineAssembly(event.player);
    }

    private static void assemblyBefore(DynamicMachine machine, EntityPlayer player, BlockPos pos) {
        if (machine == null) {
            player.sendMessage(new TextComponentTranslation("message.assembly.tip.no_machine"));
            return;
        }

        if (MachineAssemblyManager.checkMachineExist(pos)) {
            player.sendMessage(new TextComponentTranslation("message.assembly.tip.already_assembly"));
            return;
        }

        EnumFacing controllerFacing = player.world.getBlockState(pos).getValue(BlockController.FACING);
        MachineAssembly assembly = new MachineAssembly(
                player.world, pos, player,
                StructureIngredient.of(player.world, pos,
                        BlockArrayCache.getBlockArrayCache(machine.getPattern(), controllerFacing))
        );

        if (player.isCreative()) {
            assembly.assemblyCreative();
            return;
        }

        if (MachineAssembly.checkAllItems(player, assembly.getIngredient().copy())) {
            assembly.buildIngredients(false);
            MachineAssemblyManager.addMachineAssembly(assembly);
            return;
        }

        if (!AssemblyConfig.needAllBlocks) {
            player.sendMessage(new TextComponentTranslation("message.assembly.tip.partial_assembly"));
            assembly.buildIngredients(false);
            MachineAssemblyManager.addMachineAssembly(assembly);
        }
    }

    private static ItemStack getBlueprint(TileMultiblockMachineController controller) {
        return controller.getInventory().getStackInSlot(TileMultiblockMachineController.BLUEPRINT_SLOT);
    }
}
