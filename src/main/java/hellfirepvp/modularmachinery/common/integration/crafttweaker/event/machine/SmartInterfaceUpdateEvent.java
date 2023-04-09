package hellfirepvp.modularmachinery.common.integration.crafttweaker.event.machine;

import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.minecraft.CraftTweakerMC;
import crafttweaker.api.world.IBlockPos;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.event.IMachineController;
import hellfirepvp.modularmachinery.common.util.SmartInterfaceData;
import net.minecraft.util.math.BlockPos;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenGetter;

@ZenRegister
@ZenClass("mods.modularmachinery.SmartInterfaceUpdateEvent")
public class SmartInterfaceUpdateEvent extends MachineEvent {
    private final IBlockPos interfacePos;
    private final SmartInterfaceData newData;

    public SmartInterfaceUpdateEvent(IMachineController controller, BlockPos interfacePos, SmartInterfaceData newData) {
        super(controller);
        this.interfacePos = CraftTweakerMC.getIBlockPos(interfacePos);
        this.newData = newData;
    }

    @ZenGetter("interfacePos")
    public IBlockPos getInterfacePos() {
        return interfacePos;
    }

    @ZenGetter("newData")
    public SmartInterfaceData getNewData() {
        return newData;
    }
}
