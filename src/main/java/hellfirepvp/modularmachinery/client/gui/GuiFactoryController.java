package hellfirepvp.modularmachinery.client.gui;

import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.client.gui.widget.GuiScrollbar;
import hellfirepvp.modularmachinery.common.container.ContainerFactoryController;
import hellfirepvp.modularmachinery.common.tiles.TileFactoryController;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Mouse;

import java.io.IOException;

public class GuiFactoryController extends GuiContainerBase<ContainerFactoryController> {
    private static final ResourceLocation TEXTURES_FACTORY = new ResourceLocation(ModularMachinery.MODID, "textures/gui/guifactory.png");
    private static final ResourceLocation TEXTURES_FACTORY_ELEMENTS = new ResourceLocation(ModularMachinery.MODID, "textures/gui/guifactoryelements.png");
    private static final int SCROLLBAR_TOP = 8;
    private static final int SCROLLBAR_LEFT = 94;
    private static final int SCROLLBAR_HEIGHT = 197;
    private static final int MAX_PAGE_ELEMENTS = 6;
    private static final int FACTORY_ELEMENT_WIDTH = 86;
    private static final int FACTORY_ELEMENT_HEIGHT = 32;
    private static final int TEXT_DRAW_OFFSET_X = 117;
    private static final int TEXT_DRAW_OFFSET_Y = 12;
    private static final int RECIPE_QUEUE_OFFSET_X = 8;
    private static final int RECIPE_QUEUE_OFFSET_Y = 8;

    private final GuiScrollbar scrollbar = new GuiScrollbar();
    private final TileFactoryController factory;

    public GuiFactoryController(TileFactoryController factory, EntityPlayer player) {
        super(new ContainerFactoryController(factory, player));
        this.factory = factory;
        this.xSize = 280;
        this.ySize = 213;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        drawRecipeQueue();
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(TEXTURES_FACTORY);
        final int x = (this.width - this.xSize) / 2;
        final int y = (this.height - this.ySize) / 2;
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, this.xSize, this.ySize, this.xSize, this.ySize);

        updateScrollbar(x, y);
        scrollbar.draw(this, mc);
    }

    private void drawRecipeQueue() {
        int offsetY = RECIPE_QUEUE_OFFSET_Y;

        int currentScroll = scrollbar.getCurrentScroll();

        for (int i = currentScroll; i < MAX_PAGE_ELEMENTS + currentScroll; i++) {
            this.mc.getTextureManager().bindTexture(TEXTURES_FACTORY_ELEMENTS);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

            drawTexturedModalRect(RECIPE_QUEUE_OFFSET_X, offsetY, 0, 0, FACTORY_ELEMENT_WIDTH, FACTORY_ELEMENT_HEIGHT);
            drawRecipeStatus(i, RECIPE_QUEUE_OFFSET_X + 3, offsetY + 3);
            offsetY += FACTORY_ELEMENT_HEIGHT + 1;
        }
    }

    private void drawRecipeStatus(int id, int x, int y) {
        GlStateManager.pushMatrix();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        FontRenderer fr = this.fontRenderer;

        int offsetY = y;
        fr.drawString("#" + id, x, offsetY, 0x222222);
        offsetY += 9;

        fr.drawString(I18n.format("gui.controller.status.crafting"), x, offsetY, 0x222222);
        offsetY += 9;

        fr.drawString(I18n.format("gui.controller.status.crafting.progress",
                (5 * id) + "%"), x, offsetY, 0x222222);

        GlStateManager.popMatrix();
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();

        final int i = Mouse.getEventDWheel();
        if (i != 0) {
            scrollbar.wheel(i);
        }
    }

    private void updateScrollbar(int displayX, int displayY) {
        scrollbar.setLeft(SCROLLBAR_LEFT + displayX).setTop(SCROLLBAR_TOP + displayY).setHeight(SCROLLBAR_HEIGHT);
        scrollbar.setRange(0, 4, 1);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        scrollbar.click(mouseX, mouseY);
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        scrollbar.click(mouseX, mouseY);
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    @Override
    protected void setWidthHeight() {
    }
}
