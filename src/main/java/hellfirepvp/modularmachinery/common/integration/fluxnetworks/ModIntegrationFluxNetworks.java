package hellfirepvp.modularmachinery.common.integration.fluxnetworks;

import sonar.fluxnetworks.common.handler.TileEntityHandler;

public class ModIntegrationFluxNetworks {
    public static void preInit() {
        //在列表头部插入适配器，保证不被其他类型覆盖结果。
        //Insert adapters in the head of the list to ensure that the results are not overwritten by other types.
        TileEntityHandler.tileEnergyHandlers.add(0, MMEnergyHandler.INSTANCE);
    }
}
