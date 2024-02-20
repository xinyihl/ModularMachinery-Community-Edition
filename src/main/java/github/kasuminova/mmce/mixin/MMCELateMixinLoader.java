package github.kasuminova.mmce.mixin;

import net.minecraftforge.fml.common.Loader;
import zone.rong.mixinbooter.ILateMixinLoader;

import java.util.Arrays;
import java.util.List;

@SuppressWarnings("unused")
public class MMCELateMixinLoader implements ILateMixinLoader {

    @Override
    public List<String> getMixinConfigs() {
        return Arrays.asList(
                "mixins.mmce_jei_hacky.json"
        );
    }

    @Override
    public boolean shouldMixinConfigQueue(final String mixinConfig) {
        return switch (mixinConfig) {
            case "mixins.mmce_jei_hacky.json" -> Loader.isModLoaded("jei");
            default -> true;
        };
    }
}
