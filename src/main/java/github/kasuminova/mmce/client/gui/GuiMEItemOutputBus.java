package github.kasuminova.mmce.client.gui;

import appeng.core.localization.GuiText;
import github.kasuminova.mmce.client.gui.widget.base.WidgetController;
import github.kasuminova.mmce.client.gui.widget.base.WidgetGui;
import github.kasuminova.mmce.common.container.ContainerMEItemOutputBus;
import github.kasuminova.mmce.common.tile.MEItemOutputBus;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

public class GuiMEItemOutputBus extends GuiMEItemBus {
    // For convenience, the Sky Chest resource was used :P
    private static final ResourceLocation TEXTURES_OUTPUT_BUS = new ResourceLocation("appliedenergistics2", "textures/guis/skychest.png");

    public GuiMEItemOutputBus(final MEItemOutputBus te, final EntityPlayer player) {
        super(new ContainerMEItemOutputBus(te, player));
        this.ySize = 195;
        
        this.widgetController = new WidgetController(WidgetGui.of(this, this.xSize, this.ySize, guiLeft, guiTop));
    }

    @Override
    public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY) {
        this.fontRenderer.drawString(I18n.format("gui.meitemoutputbus.title"), 8, 8, 0x404040);
        this.fontRenderer.drawString(GuiText.inventory.getLocal(), 8, this.ySize - 96 + 2, 0x404040);
        super.drawFG(offsetX, offsetY, mouseX, mouseY);
    }

    @Override
    public void drawBG(int offsetX, int offsetY, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(TEXTURES_OUTPUT_BUS);
        this.drawTexturedModalRect(offsetX, offsetY, 0, 0, this.xSize, this.ySize);
        super.drawBG(offsetX, offsetY, mouseX, mouseY);
    }

}
