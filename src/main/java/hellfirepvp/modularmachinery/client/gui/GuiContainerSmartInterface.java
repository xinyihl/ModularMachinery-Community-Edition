package hellfirepvp.modularmachinery.client.gui;

import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.container.ContainerSmartInterface;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.machine.MachineRegistry;
import hellfirepvp.modularmachinery.common.network.PktSmartInterfaceUpdate;
import hellfirepvp.modularmachinery.common.tiles.TileSmartInterface;
import hellfirepvp.modularmachinery.common.util.MiscUtils;
import hellfirepvp.modularmachinery.common.util.SmartInterfaceData;
import hellfirepvp.modularmachinery.common.util.SmartInterfaceType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.animation.Animation;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

public class GuiContainerSmartInterface extends GuiContainerBase<ContainerSmartInterface> {
    public static final ResourceLocation TEXTURES_SMART_INTERFACE = new ResourceLocation(ModularMachinery.MODID, "textures/gui/guismartinterface.png");
    private final TileSmartInterface smartInterface;
    private GuiTextField textField;
    private GuiButton prev;
    private GuiButton next;
    private int showing = 0;

    public GuiContainerSmartInterface(TileSmartInterface owner, EntityPlayer opening) {
        super(new ContainerSmartInterface(owner, opening));
        this.smartInterface = owner;
    }

    @Override
    protected void setWidthHeight() {
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);
        TileSmartInterface.SmartInterfaceProvider component = smartInterface.provideComponent();

        int offsetX = 4;
        int offsetY = 4;
        GlStateManager.pushMatrix();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        FontRenderer fr = this.fontRenderer;
        fr.drawStringWithShadow(I18n.format("gui.smartinterface.title", component.getBoundSize(), showing), offsetX, offsetY, 0xFFFFFF);
        offsetX += 4;
        offsetY += 12;

        if (component.getBoundSize() <= 0) {
            fr.drawStringWithShadow(I18n.format("gui.smartinterface.notfound"), offsetX, offsetY, 0xFFFFFF);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.popMatrix();
            return;
        }
        SmartInterfaceData data = component.getMachineData(showing);
        if (data == null) {
            fr.drawStringWithShadow(I18n.format("gui.smartinterface.notfound"), offsetX, offsetY, 0xFFFFFF);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.popMatrix();
            return;
        }
        DynamicMachine machine = MachineRegistry.getRegistry().getMachine(data.getParent());
        if (machine == null) {
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.popMatrix();
            return;
        }
        SmartInterfaceType type = machine.getSmartInterfaceType(data.getType());
        if (type == null) {
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.popMatrix();
            return;
        }

        fr.drawStringWithShadow(machine.getLocalizedName(), offsetX, offsetY, 0xFFFFFF);

        offsetY += 10;
        fr.drawStringWithShadow(type.getHeaderInfo(), offsetX, offsetY, 0xFFFFFF);

        offsetY += 10;
        String valueInfo = type.getValueInfo().isEmpty()
                ? String.format(type.getValueInfo(), data.getValue())
                : I18n.format("gui.smartinterface.value", data.getValue());
        fr.drawStringWithShadow(valueInfo, offsetX, offsetY, 0xFFFFFF);

        offsetY += 10;
        fr.drawStringWithShadow(type.getFooterInfo(), offsetX, offsetY, 0xFFFFFF);

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(TEXTURES_SMART_INTERFACE);
        int i = (this.width - this.xSize) / 2;
        int j = (this.height - this.ySize) / 2;
        this.drawTexturedModalRect(i, j, 0, 0, this.xSize, this.ySize);

        if (smartInterface.provideComponent().getBoundSize() <= 0) {
            return;
        }

        textField.drawTextBox();
        prev.drawButton(Minecraft.getMinecraft(), mouseX, mouseY, Animation.getPartialTickTime());
        prev.drawButtonForegroundLayer(mouseX, mouseY);
        next.drawButton(Minecraft.getMinecraft(), mouseX, mouseY, Animation.getPartialTickTime());
        next.drawButtonForegroundLayer(mouseX, mouseY);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        textField.updateCursorCounter();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton != 0) {
            return;
        }

        textField.mouseClicked(mouseX, mouseY, mouseButton);

        TileSmartInterface.SmartInterfaceProvider component = smartInterface.provideComponent().getContainerProvider();
        Minecraft minecraft = Minecraft.getMinecraft();
        SoundHandler soundHandler = minecraft.getSoundHandler();

        if (prev.mousePressed(minecraft, mouseX, mouseY)) {
            if (component.getBoundSize() > showing && showing > 1) {
                showing--;
            }
            prev.playPressSound(soundHandler);
            return;
        }
        if (next.mousePressed(minecraft, mouseX, mouseY)) {
            if (component.getBoundSize() > showing) {
                showing++;
            }
            next.playPressSound(soundHandler);
        }
    }

    @Override
    public void keyTyped(char c, int i) throws IOException {
        if (!textField.isFocused()) {
            super.keyTyped(c, i);
        }

        if (i == Keyboard.KEY_RETURN) {
            TileSmartInterface.SmartInterfaceProvider provider = smartInterface.provideComponent().getContainerProvider();
            SmartInterfaceData data = provider.getMachineData(showing);
            if (data == null) {
                return;
            }

            try {
                SmartInterfaceData newData = new SmartInterfaceData(data.getPos(), data.getParent(), data.getType(), Float.parseFloat(textField.getText()));
                ModularMachinery.NET_CHANNEL.sendToServer(new PktSmartInterfaceUpdate(newData));
            } catch (NumberFormatException ex) {
                ModularMachinery.log.warn(ex);
            }
            return;
        }

        if (Character.isDigit(c) || c == '.' || c == 'E' || MiscUtils.isTextBoxKey(i)) {
            textField.textboxKeyTyped(c, i);
        }
    }

    @Override
    public void initGui() {
        super.initGui();
        textField = new GuiTextField(0, fontRenderer, this.width / 2, this.height / 2 - 47, 80, 10);
        textField.setMaxStringLength(16);
        prev = new GuiButton(1, this.width / 2 - 80, this.height / 2 - 25, 40, 20,
                I18n.format("gui.smartinterface.prev"));
        next = new GuiButton(2, this.width / 2 + 40, this.height / 2 - 25, 40, 20,
                I18n.format("gui.smartinterface.next"));
    }
}
