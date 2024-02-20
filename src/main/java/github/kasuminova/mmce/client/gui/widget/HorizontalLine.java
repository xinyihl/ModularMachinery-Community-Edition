package github.kasuminova.mmce.client.gui.widget;

import github.kasuminova.mmce.client.gui.util.MousePos;
import github.kasuminova.mmce.client.gui.util.RenderPos;
import github.kasuminova.mmce.client.gui.util.RenderSize;
import github.kasuminova.mmce.client.gui.widget.base.DynamicWidget;
import github.kasuminova.mmce.client.gui.widget.base.WidgetGui;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;

public class HorizontalLine extends DynamicWidget {
    protected int color = 0xFFFFFFFF;

    @Override
    public void render(final WidgetGui gui, final RenderSize renderSize, final RenderPos renderPos, final MousePos mousePos) {
        GuiContainer.drawRect(renderPos.posX(), renderPos.posY(), renderPos.posX() + width, renderPos.posY() + height, color);
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    public int getColor() {
        return color;
    }

    public HorizontalLine setColor(final int color) {
        this.color = color;
        return this;
    }
}
