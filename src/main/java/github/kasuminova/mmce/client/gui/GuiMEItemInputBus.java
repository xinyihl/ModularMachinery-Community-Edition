package github.kasuminova.mmce.client.gui;

import appeng.client.gui.AEBaseGui;
import appeng.core.localization.GuiText;
import github.kasuminova.mmce.common.container.ContainerMEItemInputBus;
import github.kasuminova.mmce.common.tile.MEItemInputBus;
import hellfirepvp.modularmachinery.ModularMachinery;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

public class GuiMEItemInputBus extends AEBaseGui {
    private static final ResourceLocation TEXTURES_INPUT_BUS = new ResourceLocation(ModularMachinery.MODID, "textures/gui/meiteminputbus.png");

    public GuiMEItemInputBus(final MEItemInputBus te, final EntityPlayer player) {
        super(new ContainerMEItemInputBus(te, player));
        this.ySize = 204;
    }

    @Override
    public void drawFG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        this.fontRenderer.drawString(I18n.format("gui.meiteminputbus.title"), 8, 8, 0x404040);
        this.fontRenderer.drawString(GuiText.Config.getLocal(), 8, 6 + 11 + 7, 0x404040);
        this.fontRenderer.drawString(GuiText.StoredItems.getLocal(), 97, 6 + 11 + 7, 0x404040);
        this.fontRenderer.drawString(GuiText.inventory.getLocal(), 8, this.ySize - 93, 0x404040);
    }

    @Override
    public void drawBG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(TEXTURES_INPUT_BUS);
        this.drawTexturedModalRect(offsetX, offsetY, 0, 0, this.xSize, this.ySize);
    }
}
