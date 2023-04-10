package hellfirepvp.modularmachinery.client.gui;

import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.container.ContainerFactoryController;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

public class GuiFactoryController extends GuiContainerBase<ContainerFactoryController> {
    public static final ResourceLocation TEXTURES_FACTORY = new ResourceLocation(ModularMachinery.MODID, "textures/gui/guifactorycontroller.png");

    public GuiFactoryController(ContainerFactoryController container) {
        super(container);
    }

    @Override
    protected void setWidthHeight() {
        this.width = 280;
        this.height = 214;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(TEXTURES_FACTORY);
        int i = (this.width - this.xSize) / 2;
        int j = (this.height - this.ySize) / 2;
        this.drawTexturedModalRect(i, j, 0, 0, this.xSize, this.ySize);
    }
}
