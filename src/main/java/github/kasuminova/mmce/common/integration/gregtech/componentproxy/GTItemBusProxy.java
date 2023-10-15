package github.kasuminova.mmce.common.integration.gregtech.componentproxy;

import github.kasuminova.mmce.common.machine.component.MachineComponentProxy;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.common.metatileentities.multi.multiblockpart.MetaTileEntityItemBus;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.items.IItemHandlerModifiable;

public class GTItemBusProxy implements MachineComponentProxy<GTItemBusProxy.GTItemBusMachineComponent> {
    public static final GTItemBusProxy INSTANCE = new GTItemBusProxy();

    private GTItemBusProxy() {
    }

    @Override
    public boolean isSupported(final TileEntity te) {
        if (te instanceof MetaTileEntityHolder metaTE) {
            return metaTE.getMetaTileEntity() instanceof MetaTileEntityItemBus;
        }
        return false;
    }

    @Override
    public GTItemBusMachineComponent proxyComponent(final TileEntity te) {
        if (!(te instanceof MetaTileEntityHolder metaTE)) {
            return null;
        }
        if (!(metaTE.getMetaTileEntity() instanceof MetaTileEntityItemBus itemBus)) {
            return null;
        }

        MultiblockAbility<IItemHandlerModifiable> ability = itemBus.getAbility();
        if (ability == MultiblockAbility.IMPORT_ITEMS) {
            return new GTItemBusMachineComponent(IOType.INPUT, itemBus.getImportItems());
        }
        if (ability == MultiblockAbility.EXPORT_ITEMS) {
            return new GTItemBusMachineComponent(IOType.OUTPUT, itemBus.getExportItems());
        }

        return null;
    }

    public static class GTItemBusMachineComponent extends MachineComponent.ItemBus {
        private final IItemHandlerModifiable itemHandler;

        public GTItemBusMachineComponent(final IOType ioType, IItemHandlerModifiable itemHandler) {
            super(ioType);
            this.itemHandler = itemHandler;
        }

        @Override
        public boolean isAsyncSupported() {
            return false;
        }

        @Override
        public IItemHandlerModifiable getContainerProvider() {
            return itemHandler;
        }
    }
}
