package github.kasuminova.mmce.common.integration.gregtech.componentproxy;

import github.kasuminova.mmce.common.integration.gregtech.handlerproxy.GTEnergyHandlerProxy;
import github.kasuminova.mmce.common.machine.component.MachineComponentProxy;
import gregtech.api.capability.IEnergyContainer;
import gregtech.api.capability.ILaserContainer;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.common.metatileentities.multi.multiblockpart.MetaTileEntityEnergyHatch;
import gregtech.common.metatileentities.multi.multiblockpart.MetaTileEntityLaserHatch;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.util.IEnergyHandlerAsync;
import net.minecraft.tileentity.TileEntity;

import javax.annotation.Nullable;
import java.util.ArrayList;

public class GTEnergyHatchProxy implements MachineComponentProxy<GTEnergyHatchProxy.GTEnergyHatchMachineComponent> {
    public static final GTEnergyHatchProxy INSTANCE = new GTEnergyHatchProxy();

    private GTEnergyHatchProxy() {
    }

    @Override
    public boolean isSupported(final TileEntity te) {
        if (te instanceof MetaTileEntityHolder metaTEHolder) {
            MetaTileEntity metaTE = metaTEHolder.getMetaTileEntity();
            return metaTE instanceof MetaTileEntityEnergyHatch || metaTE instanceof MetaTileEntityLaserHatch;
        }
        return false;
    }

    @Nullable
    @Override
    public GTEnergyHatchMachineComponent proxyComponent(final TileEntity te) {
        if (!(te instanceof MetaTileEntityHolder metaTEHolder)) {
            return null;
        }

        MetaTileEntity metaTE = metaTEHolder.getMetaTileEntity();
        if (metaTE instanceof MetaTileEntityEnergyHatch energyHatch) {
            ArrayList<IEnergyContainer> list = new ArrayList<>(2);
            MultiblockAbility<IEnergyContainer> ability = energyHatch.getAbility();
            if (ability == MultiblockAbility.INPUT_ENERGY) {
                energyHatch.registerAbilities(list);
                return new GTEnergyHatchMachineComponent(IOType.INPUT, list.get(0));
            }
            if (ability == MultiblockAbility.OUTPUT_ENERGY) {
                energyHatch.registerAbilities(list);
                return new GTEnergyHatchMachineComponent(IOType.OUTPUT, list.get(0));
            }
            return null;
        }
        if (metaTE instanceof MetaTileEntityLaserHatch laserHatch) {
            ArrayList<ILaserContainer> list = new ArrayList<>(2);
            MultiblockAbility<ILaserContainer> ability = laserHatch.getAbility();
            if (ability == MultiblockAbility.INPUT_LASER) {
                laserHatch.registerAbilities(list);
                return new GTEnergyHatchMachineComponent(IOType.INPUT, list.get(0));
            }
            if (ability == MultiblockAbility.OUTPUT_LASER) {
                laserHatch.registerAbilities(list);
                return new GTEnergyHatchMachineComponent(IOType.OUTPUT, list.get(0));
            }
            return null;
        }

        return null;
    }

    public static class GTEnergyHatchMachineComponent extends MachineComponent.EnergyHatch {
        private final IEnergyContainer energyContainer;

        public GTEnergyHatchMachineComponent(IOType ioType, final IEnergyContainer energyContainer) {
            super(ioType);
            this.energyContainer = energyContainer;
        }

        @Override
        public boolean isAsyncSupported() {
            return false;
        }

        @Override
        public IEnergyHandlerAsync getContainerProvider() {
            return new GTEnergyHandlerProxy(energyContainer);
        }
    }
}
