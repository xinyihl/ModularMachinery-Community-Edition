package hellfirepvp.modularmachinery.common.integration;

import hellfirepvp.modularmachinery.common.integration.theoneprobe.MMInfoProvider;
import mcjty.theoneprobe.TheOneProbe;
import mcjty.theoneprobe.apiimpl.TheOneProbeImp;
import net.minecraftforge.common.config.Configuration;

public class ModIntegrationTOP {
    public static int recipeProgressBarFilledColor = 0xCC54FF9F;
    public static int recipeProgressBarAlternateFilledColor = 0xCC4EEE94;
    public static int recipeProgressBarBorderColor = 0xCC43CD80;
    public static int recipeProgressBarBackgroundColor = 0xFF000000;
    public static boolean showRecipeProgressBarDecimalPoints = true;
    public static boolean showParallelControllerInfo = true;

    public static void registerProvider() {
        TheOneProbeImp top = TheOneProbe.theOneProbeImp;

        top.registerProvider(new MMInfoProvider());
    }

    public static void loadFromConfig(Configuration cfg) {
        recipeProgressBarFilledColor = Integer.parseUnsignedInt(
                cfg.getString("RECIPE_PROGRESSBAR_FILLED_COLOR", "display.theoneprobe",
                        "CC54FF9F", "Machine progressbar filled color if TheOneProbe mod is installed."),
                16);

        recipeProgressBarAlternateFilledColor = Integer.parseUnsignedInt(
                cfg.getString("RECIPE_PROGRESSBAR_ALTERNATE_FILLED_COLOR", "display.theoneprobe",
                        "CC4EEE94", "Machine progressbar filled color if TheOneProbe mod is installed."),
                16);

        recipeProgressBarBorderColor = Integer.parseUnsignedInt(
                cfg.getString("RECIPE_PROGRESSBAR_BORDER_COLOR", "display.theoneprobe",
                        "CC43CD80", "Machine progressbar filled color if TheOneProbe mod is installed."),
                16);

        recipeProgressBarBackgroundColor = Integer.parseUnsignedInt(
                cfg.getString("RECIPE_PROGRESSBAR_BACKGROUND_COLOR", "display.theoneprobe",
                        "FF000000", "Machine progressbar filled color if TheOneProbe mod is installed."),
                16);
        showRecipeProgressBarDecimalPoints = cfg.getBoolean("SHOW_RECIPE_PROGRESSBAR_DECIMAL_POINTS",
                "display.theoneprobe", true,
                "Show recipe progressbar decimal points if TheOneProbe mod is installed.");
        showParallelControllerInfo = cfg.getBoolean("SHOW_PARALLEL_CONTROLLER_INFO", "display.theoneprobe",
                true, "Show parallel controller info if TheOneProbe mod is installed.");
    }
}
