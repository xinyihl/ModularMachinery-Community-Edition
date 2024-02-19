package github.kasuminova.mmce.client.gui.widget.slot;

import github.kasuminova.mmce.client.gui.util.MousePos;
import github.kasuminova.mmce.client.gui.util.RenderPos;
import github.kasuminova.mmce.client.gui.util.RenderSize;
import github.kasuminova.mmce.client.gui.widget.base.DynamicWidget;
import github.kasuminova.mmce.client.gui.widget.base.WidgetGui;
import github.kasuminova.mmce.client.gui.widget.container.WidgetContainer;
import hellfirepvp.modularmachinery.common.base.Mods;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.config.GuiUtils;

import java.awt.*;

public class SlotVirtual extends DynamicWidget {

    protected ResourceLocation slotTexLocation = null;
    protected int slotTexX = 0;
    protected int slotTexY = 0;

    protected ItemStack stackInSlot = ItemStack.EMPTY;

    public SlotVirtual() {
        setWidthHeight(18, 18);
    }

    public SlotVirtual(final ItemStack stackInSlot) {
        this();
        this.stackInSlot = stackInSlot;
    }

    public static SlotVirtual ofJEI() {
        return Mods.JEI.isPresent() ? new SlotVirtualJEI() : new SlotVirtual();
    }

    public static SlotVirtual ofJEI(final ItemStack stackInSlot) {
        return Mods.JEI.isPresent() ? new SlotVirtualJEI(stackInSlot) : new SlotVirtual(stackInSlot);
    }

    @Override
    public void render(final WidgetGui widgetGui, final RenderSize renderSize, final RenderPos renderPos, final MousePos mousePos) {
        GuiScreen gui = widgetGui.getGui();
        Minecraft mc = gui.mc;
        int rx = renderPos.posX();
        int ry = renderPos.posY();

        if (slotTexLocation != null) {
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            mc.getTextureManager().bindTexture(slotTexLocation);
            gui.drawTexturedModalRect(rx, ry, slotTexX, slotTexY, getWidth(), getHeight());
        }

        if (stackInSlot.isEmpty()) {
            return;
        }

        RenderHelper.disableStandardItemLighting();
        GlStateManager.enableDepth();
        RenderHelper.enableGUIStandardItemLighting();

        RenderItem ri = mc.getRenderItem();
        rx += 1;
        ry += 1;
        ri.renderItemAndEffectIntoGUI(stackInSlot, rx, ry);
        ri.renderItemOverlays(mc.fontRenderer, stackInSlot, rx, ry);
        drawHoverOverlay(mousePos, rx, ry);

        GlStateManager.disableBlend();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.color(1F, 1F, 1F, 1F);
    }

    protected void drawHoverOverlay(final MousePos mousePos, final int rx, final int ry) {
        if (isMouseOver(mousePos)) {
            GlStateManager.disableLighting();
            GlStateManager.disableDepth();
            GlStateManager.colorMask(true, true, true, false);
            GuiScreen.drawRect(rx, ry, rx + 16, ry + 16, new Color(255, 255, 255, 150).getRGB());
            GlStateManager.colorMask(true, true, true, true);
            GlStateManager.enableLighting();
            GlStateManager.enableDepth();
        }
    }

    @Override
    public void postRender(final WidgetGui gui, final RenderSize renderSize, final RenderPos renderPos, final MousePos mousePos) {
        if (!stackInSlot.isEmpty() && isMouseOver(mousePos)) {
            WidgetContainer.disableScissor();
            GuiScreen g = gui.getGui();
            GuiUtils.preItemToolTip(stackInSlot);
            g.drawHoveringText(g.getItemToolTip(stackInSlot), mousePos.mouseX() + renderPos.posX(), mousePos.mouseY() + renderPos.posY());
            GuiUtils.postItemToolTip();
            WidgetContainer.enableScissor();
        }
    }

    public SlotVirtual setSlotTexLocation(final ResourceLocation slotTexLocation) {
        this.slotTexLocation = slotTexLocation;
        return this;
    }

    public SlotVirtual setSlotTexX(final int slotTexX) {
        this.slotTexX = slotTexX;
        return this;
    }

    public SlotVirtual setSlotTexY(final int slotTexY) {
        this.slotTexY = slotTexY;
        return this;
    }

    public ItemStack getStackInSlot() {
        return stackInSlot;
    }

    public SlotVirtual setStackInSlot(final ItemStack stackInSlot) {
        this.stackInSlot = stackInSlot;
        return this;
    }
}