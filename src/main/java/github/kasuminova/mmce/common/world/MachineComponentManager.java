package github.kasuminova.mmce.common.world;

import com.github.bsideup.jabel.Desugar;
import github.kasuminova.mmce.common.util.concurrent.ExecuteGroup;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MachineComponentManager {
    public static final MachineComponentManager INSTANCE = new MachineComponentManager();

    private MachineComponentManager() {
    }

    private final Map<World, Map<BlockPos, ComponentInfo>> componentMap = new ConcurrentHashMap<>();

    public void addWorld(World world) {
        componentMap.put(world, new ConcurrentHashMap<>());
    }

    public void removeWorld(World world) {
        Map<BlockPos, ComponentInfo> removed = componentMap.remove(world);
        if (removed == null) {
            return;
        }
        for (final ComponentInfo info : removed.values()) {
            info.owners.clear();
        }
        removed.clear();
    }

    public void checkComponentShared(TileEntity component, TileMultiblockMachineController ctrl) {
        World world = component.getWorld();
        BlockPos pos = component.getPos();

        Map<BlockPos, ComponentInfo> posComponentMap = componentMap.computeIfAbsent(world, v -> new ConcurrentHashMap<>());

        ComponentInfo info = posComponentMap.computeIfAbsent(pos, v -> new ComponentInfo(
                component, pos, new ObjectArraySet<>(Collections.singleton(ctrl))));

        if (!info.areTileEntityEquals(component)) {
            ComponentInfo newInfo = new ComponentInfo(component, pos, new ObjectArraySet<>(Collections.singleton(ctrl)));
            posComponentMap.put(pos, newInfo);
            return;
        }

        Set<TileMultiblockMachineController> owners = info.owners;
        if (owners.contains(ctrl)) {
            return;
        }

        synchronized (owners) {
            owners.add(ctrl);
            if (owners.size() <= 1) {
                return;
            }

            long groupId = -1;
            for (final TileMultiblockMachineController owner : owners) {
                if (owner.getExecuteGroupId() != -1) {
                    groupId = owner.getExecuteGroupId();
                }
            }

            if (groupId == -1) {
                groupId = ExecuteGroup.newGroupId();
            }

            for (final TileMultiblockMachineController owner : owners) {
                owner.setExecuteGroupId(groupId);
            }
        }
    }

    public void removeOwner(TileEntity component, TileMultiblockMachineController ctrl) {
        World world = component.getWorld();
        BlockPos pos = component.getPos();

        Map<BlockPos, ComponentInfo> posComponentMap = componentMap.computeIfAbsent(world, v -> new ConcurrentHashMap<>());

        ComponentInfo info = posComponentMap.get(pos);
        if (info == null) {
            return;
        }

        if (!info.areTileEntityEquals(component)) {
            ComponentInfo newInfo = new ComponentInfo(component, pos, new ObjectArraySet<>());
            posComponentMap.put(pos, newInfo);
        } else {
            Set<TileMultiblockMachineController> owners = info.owners;
            synchronized (owners) {
                owners.remove(ctrl);
            }
        }
    }

    @Desugar
    public record ComponentInfo(TileEntity te, BlockPos pos, Set<TileMultiblockMachineController> owners) {

        public boolean areTileEntityEquals(TileEntity te) {
            return this.te == te;
        }

        @Override
        public int hashCode() {
            return pos.hashCode();
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj instanceof ComponentInfo componentInfo) {
                return pos.equals(componentInfo.pos);
            }
            return false;
        }
    }
}
