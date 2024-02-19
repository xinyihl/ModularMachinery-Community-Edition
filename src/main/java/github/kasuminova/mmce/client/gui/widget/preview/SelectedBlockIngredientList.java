package github.kasuminova.mmce.client.gui.widget.preview;

import github.kasuminova.mmce.client.gui.util.MousePos;
import github.kasuminova.mmce.client.gui.util.RenderPos;
import github.kasuminova.mmce.client.gui.util.RenderSize;
import github.kasuminova.mmce.client.gui.widget.base.WidgetGui;
import github.kasuminova.mmce.client.gui.widget.slot.SlotVirtual;
import hellfirepvp.modularmachinery.ModularMachinery;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;

import java.util.List;

import static github.kasuminova.mmce.client.gui.widget.preview.MachineStructurePreviewPanel.WIDGETS_TEX_LOCATION;

public class SelectedBlockIngredientList extends IngredientList {
    protected int listBgTexX = 224;
    protected int listBgTexY = 125;
    protected int listBgTexWidth = 25;
    protected int listBgTexHeight = 126;

    public SelectedBlockIngredientList() {
        setWidthHeight(25, 126);
    }

    @Override
    public void initWidget(final WidgetGui gui) {
        super.initWidget(gui);
        scrollbar.setMargin(1, 1, 1, 1);
        scrollbar.setWidthHeight(6, 124);
        scrollbar.getScroll()
                .setMouseDownTextureXY(200, 159)
                .setHoveredTextureXY(192, 159)
                .setTextureXY(184, 159)
                .setUnavailableTextureXY(208, 159)
                .setTextureLocation(WIDGETS_TEX_LOCATION)
                .setWidthHeight(6, 17);
    }

    @Override
    protected void renderInternal(final WidgetGui gui, final RenderSize renderSize, final RenderPos renderPos, final MousePos mousePos) {
        drawListBg(gui, renderPos);

        RenderSize trueRenderSize = renderSize.subtract(new RenderSize(0, 2));
        pushScissor(gui, trueRenderSize, renderPos.add(new RenderPos(0, 1)), getWidth(), getHeight() - 2);

        try {
            super.renderInternal(gui, trueRenderSize, renderPos, mousePos);
        } catch (Exception e) {
            SCISSOR_STACK.get().clear();
            ModularMachinery.log.error("Error when rendering dynamic widgets!", e);
            throw e;
        } finally {
            popScissor(trueRenderSize);
        }
    }

    protected void drawListBg(final WidgetGui widgetGui, final RenderPos renderPos) {
        GuiScreen gui = widgetGui.getGui();
        gui.mc.getTextureManager().bindTexture(WIDGETS_TEX_LOCATION);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        gui.drawTexturedModalRect(
                renderPos.posX(), renderPos.posY(),
                listBgTexX, listBgTexY,
                listBgTexWidth, listBgTexHeight
        );
        GlStateManager.disableBlend();
    }

    @Override
    public SelectedBlockIngredientList setStackList(final List<ItemStack> list) {
        getWidgets().clear();
        list.stream().map(SlotVirtual::ofJEI).forEach(this::addWidget);
        return this;
    }
}
