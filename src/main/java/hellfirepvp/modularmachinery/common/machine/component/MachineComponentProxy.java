package hellfirepvp.modularmachinery.common.machine.component;

import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import net.minecraft.tileentity.TileEntity;

public interface MachineComponentProxy {

    boolean isSupported(TileEntity te);

    MachineComponent<?> proxy(TileEntity te);

}
