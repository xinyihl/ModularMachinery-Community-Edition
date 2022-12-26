package hellfirepvp.modularmachinery.common.util;

import net.minecraftforge.common.config.Configuration;

public class CatalystNameUtil {

    private static String[] catalystName = {"Catalyst00"};

    public static void loadFromConfig(Configuration cfg) {
        catalystName = cfg.getStringList("Solid_Catalyst_Name", "display.catalyst", catalystName, "Display name for solid catalyst bus, will be shown in the item tooltip.");
    }

    public static String getNameAt(int k) {
        if (catalystName.length <= k) return "Catalyst" + k;
        return catalystName[k];
    }
}
