package hellfirepvp.modularmachinery.client.gui;

import hellfirepvp.modularmachinery.common.container.ContainerParallelController;
import hellfirepvp.modularmachinery.common.tiles.TileParallelController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.model.animation.Animation;

import java.util.ArrayList;
import java.util.List;

public class GuiContainerParallelController extends GuiContainerBase<ContainerParallelController> {
    private final TileParallelController parallelCtrlInterface;
    private List<GuiButton> buttons = new ArrayList<>();
    private GuiButton increment_1;
    private GuiButton increment_10;
    private GuiButton increment_100;
    private GuiButton decrement_1;
    private GuiButton decrement_10;
    private GuiButton decrement_100;
    private GuiTextField textField;
    public GuiContainerParallelController(TileParallelController te, EntityPlayer opening) {
        super(new ContainerParallelController(te, opening));
        this.parallelCtrlInterface = te;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);

    }

    @Override
    protected void setWidthHeight() {

    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(TEXTURES_EMPTY_GUI);
        int i = (this.width - this.xSize) / 2;
        int j = (this.height - this.ySize) / 2;
        this.drawTexturedModalRect(i, j, 0, 0, this.xSize, this.ySize);

        textField.drawTextBox();
        for (GuiButton button : buttons) {
            button.drawButton(Minecraft.getMinecraft(), mouseX, mouseY, Animation.getPartialTickTime());
            button.drawButtonForegroundLayer(mouseX, mouseY);
        }
    }

    @Override
    public void initGui() {
        super.initGui();
        textField = new GuiTextField(0, fontRenderer, 10, this.height / 2 - 48, 160, 10);
        textField.setMaxStringLength(16);

        decrement_1 = new GuiButton(1, this.width / 2 - 81, this.height / 2 + 25, 20, 20,
                "-1");
        buttons.add(decrement_1);
        decrement_10 = new GuiButton(2, this.width / 2 - 51, this.height / 2 + 25, 30, 20,
                "-10");
        buttons.add(decrement_10);
        decrement_100 = new GuiButton(3, this.width / 2 - 1, this.height / 2 + 25, 40, 20,
                "-100");
        buttons.add(decrement_100);

        increment_1 = new GuiButton(4, this.width / 2 - 81, this.height / 2 - 25, 20, 20,
                "+1");
        buttons.add(increment_1);
        increment_10 = new GuiButton(5, this.width / 2 - 51, this.height / 2 - 25, 30, 20,
                "+10");
        buttons.add(increment_10);
        increment_100 = new GuiButton(6, this.width / 2 - 1, this.height / 2 - 25, 40, 20,
                "+100");
        buttons.add(increment_100);
    }
}
