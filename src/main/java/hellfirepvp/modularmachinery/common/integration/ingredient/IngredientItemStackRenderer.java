package hellfirepvp.modularmachinery.common.integration.ingredient;

import hellfirepvp.modularmachinery.common.util.MiscUtils;
import mezz.jei.api.ingredients.IIngredientRenderer;
import mezz.jei.plugins.vanilla.ingredients.item.ItemStackRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.util.ITooltipFlag;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class IngredientItemStackRenderer implements IIngredientRenderer<IngredientItemStack> {

    private final ItemStackRenderer renderer = new ItemStackRenderer();

    public static void renderRequirementOverlyIntoGUI(final FontRenderer fr, final int xPos, final int yPos, int min, int max) {
        String s = String.format("%s~%s", MiscUtils.formatNumberToInt(min), MiscUtils.formatNumberToInt(max));

        float scale = 0.5F;

        GlStateManager.disableLighting();
        GlStateManager.disableBlend();
        GlStateManager.pushMatrix();
        GlStateManager.scale(scale, scale, 1.0F);
        GlStateManager.translate(0.0F, 0.0F, 160F);

        boolean unicodeFlag = fr.getUnicodeFlag();
        fr.setUnicodeFlag(false);
        fr.drawStringWithShadow(s, (xPos + 16 - (fr.getStringWidth(s) * scale)) / scale, (float) (yPos + 12) / scale, 0xFFFFFF);
        fr.setUnicodeFlag(unicodeFlag);

//        GlStateManager.scale(1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
        GlStateManager.enableBlend();
        GlStateManager.enableLighting();
    }

    @Override
    public void render(@Nonnull final Minecraft minecraft, final int xPos, final int yPos, @Nullable final IngredientItemStack ingredient) {
        if (ingredient != null) {
            int min = ingredient.min();
            int max = ingredient.max();

            GlStateManager.enableDepth();
            RenderHelper.enableGUIStandardItemLighting();
            FontRenderer font = getFontRenderer(minecraft, ingredient);

            minecraft.getRenderItem().renderItemAndEffectIntoGUI(null, ingredient.stack(), xPos, yPos);
            if (min != max) {
                renderRequirementOverlyIntoGUI(font, xPos, yPos, min, max);
                minecraft.getRenderItem().renderItemOverlayIntoGUI(font, ingredient.stack(), xPos, yPos, "");
            } else {
                minecraft.getRenderItem().renderItemOverlayIntoGUI(font, ingredient.stack(), xPos, yPos, null);
            }

            GlStateManager.disableBlend();
            RenderHelper.disableStandardItemLighting();
        }
    }

    @Nonnull
    @Override
    public List<String> getTooltip(@Nonnull final Minecraft minecraft, final IngredientItemStack ingredient, @Nonnull final ITooltipFlag tooltipFlag) {
        return renderer.getTooltip(minecraft, ingredient.stack(), tooltipFlag);
    }

    @Nonnull
    @Override
    public FontRenderer getFontRenderer(@Nonnull final Minecraft minecraft, final IngredientItemStack ingredient) {
        return renderer.getFontRenderer(minecraft, ingredient.stack());
    }
}
