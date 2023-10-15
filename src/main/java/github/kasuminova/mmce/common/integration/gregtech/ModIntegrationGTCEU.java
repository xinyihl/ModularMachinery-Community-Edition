package github.kasuminova.mmce.common.integration.gregtech;

import github.kasuminova.mmce.common.integration.gregtech.componentproxy.GTEnergyHatchProxy;
import github.kasuminova.mmce.common.integration.gregtech.componentproxy.GTFluidHatchProxy;
import github.kasuminova.mmce.common.integration.gregtech.componentproxy.GTItemBusProxy;
import github.kasuminova.mmce.common.machine.component.MachineComponentProxyRegistry;

public class ModIntegrationGTCEU {

    public static void initialize() {
        MachineComponentProxyRegistry.INSTANCE.register("GTEnergyHatchProxy", GTEnergyHatchProxy.INSTANCE);
        MachineComponentProxyRegistry.INSTANCE.register("GTItemBusProxy", GTItemBusProxy.INSTANCE);
        MachineComponentProxyRegistry.INSTANCE.register("GTFluidHatchProxy", GTFluidHatchProxy.INSTANCE);
    }

}
