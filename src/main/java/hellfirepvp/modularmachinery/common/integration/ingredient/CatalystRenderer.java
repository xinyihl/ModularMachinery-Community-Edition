package hellfirepvp.modularmachinery.common.integration.ingredient;

import mezz.jei.api.gui.IDrawable;
import mezz.jei.plugins.vanilla.ingredients.item.ItemStackRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;

public class CatalystRenderer extends ItemStackRenderer {
    private final IDrawable overlay;

    public CatalystRenderer(IDrawable overlay) {
        this.overlay = overlay;
    }

    @Override
    public void render(Minecraft minecraft, int x, int y, @Nullable ItemStack ingredient) {
        super.render(minecraft, x, y, ingredient);
        overlay.draw(minecraft, x, y);
    }
}
