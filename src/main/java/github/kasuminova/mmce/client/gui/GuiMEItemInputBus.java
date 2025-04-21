package github.kasuminova.mmce.client.gui;

import appeng.container.slot.SlotDisabled;
import appeng.container.slot.SlotFake;
import appeng.core.localization.GuiText;
import github.kasuminova.mmce.client.gui.util.TextureProperties;
import github.kasuminova.mmce.client.gui.widget.ButtonElements;
import github.kasuminova.mmce.client.gui.widget.base.WidgetController;
import github.kasuminova.mmce.client.gui.widget.base.WidgetGui;
import github.kasuminova.mmce.client.gui.widget.container.Row;
import github.kasuminova.mmce.common.container.ContainerMEItemInputBus;
import github.kasuminova.mmce.common.network.PktMEInputBusInvAction;
import github.kasuminova.mmce.common.tile.MEItemInputBus;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.client.ClientProxy;
import hellfirepvp.modularmachinery.common.util.MiscUtils;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.client.config.GuiUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GuiMEItemInputBus extends GuiMEItemBus {
    private static final ResourceLocation TEXTURES_INPUT_BUS = new ResourceLocation(ModularMachinery.MODID, "textures/gui/meiteminputbus.png");

    private int invActionAmount = 0;
    private GuiTextField thresholdField;
    private int thresholdValue = 256;
    private boolean thresholdFieldInitialized = false;

    protected final ButtonElements<MEItemInputBus.MERequestMode> slotProcessModeSetting = new ButtonElements<>();

    protected final MEItemInputBus owner;

    public GuiMEItemInputBus(final MEItemInputBus owner, final EntityPlayer player) {
        super(new ContainerMEItemInputBus(owner, player));

        this.owner = owner;

        this.ySize = 205;
        this.xSize = 256;

        this.guiLeft = (this.width - this.ySize) / 2;
        this.guiTop = (this.height - this.ySize) / 2;

        this.widgetController = new WidgetController(WidgetGui.of(this, this.xSize, this.ySize, guiLeft, guiTop));

        // Init WorkModeSetting...
        slotProcessModeSetting
                // ButtonTexture 1
                .addElement(MEItemInputBus.MERequestMode.DEFAULT, TextureProperties.of(0, 239, 16, 16))
                // ButtonTexture 2
                .addElement(MEItemInputBus.MERequestMode.THRESHOLD, TextureProperties.of(16, 239, 16, 16))
                // ButtonTexture 5
                .setMouseDownTexture(32, 223)
                // ButtonTexture 5
                .setHoveredTexture(16, 223)
                // ButtonTexture 4
                .setTexture(0, 223)
                .setTextureLocation(GuiMEItemInputBus.TEXTURES_INPUT_BUS)
                .setTooltipFunction((btn) -> {
                    MEItemInputBus.MERequestMode current = slotProcessModeSetting.getCurrentSelection();
                    List<String> tooltips = new ArrayList<>();
                    tooltips.add(I18n.format("gui.meiteminputbus.work_mode.desc"));
                    tooltips.add((current == MEItemInputBus.MERequestMode.DEFAULT ? I18n.format("gui.meiteminputbus.current") : "")
                            + I18n.format("gui.meiteminputbus.default.desc"));
                    tooltips.add((current == MEItemInputBus.MERequestMode.THRESHOLD ? I18n.format("gui.meiteminputbus.current") : "")
                            + I18n.format("gui.meiteminputbus.threshold.desc"));
                    return tooltips;
                })
                .setOnClickedListener((btn) -> {
                    MEItemInputBus.MERequestMode current = slotProcessModeSetting.getCurrentSelection();
                    if (current == null) {
                        return;
                    }
                    switch (current) {
                        case DEFAULT -> ModularMachinery.NET_CHANNEL.sendToServer(new PktMEInputBusInvAction(PktMEInputBusInvAction.Action.ENABLE_DEFAULT_MODE));
                        case THRESHOLD -> ModularMachinery.NET_CHANNEL.sendToServer(new PktMEInputBusInvAction(PktMEInputBusInvAction.Action.ENABLE_THRESHOLD_MODE));
                    }
                })
                .setWidthHeight(16, 16);

        // Init Widget Containers...
        Row stackListButtons = new Row();
        stackListButtons.addWidgets(slotProcessModeSetting).setAbsXY(265, 100);

        this.widgetController.addWidget(stackListButtons);

        // Update state...
        updateGUIState();
    }

    @Override
    public void initGui() {
        super.initGui();

        // Initialize threshold text field if not already initialized
        if (!thresholdFieldInitialized) {
            thresholdField = new GuiTextField(0, this.fontRenderer, this.guiLeft + 265, this.guiTop + 135, 70, 12);
            thresholdField.setMaxStringLength(7);
            thresholdField.setText(String.valueOf(thresholdValue));
            thresholdField.setEnableBackgroundDrawing(true);
            thresholdField.setVisible(true);
            thresholdField.setEnabled(true);
            thresholdField.setTextColor(0xFFFFFF);
            thresholdField.setDisabledTextColour(0x7F7F7F);
            thresholdField.setCanLoseFocus(true);
            thresholdFieldInitialized = true;

            // Set the threshold value from the owner if available
            if (owner != null && owner.getThresholdValue() > 0) {
                thresholdValue = owner.getThresholdValue();
                thresholdField.setText(String.valueOf(thresholdValue));
            }
        } else {
            // Just update the position if already initialized
            thresholdField.x = this.guiLeft + 265;
            thresholdField.y = this.guiTop + 135;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);

        // Draw threshold field only when in threshold mode
        if (owner.getMERequestMode() == MEItemInputBus.MERequestMode.THRESHOLD) {
            thresholdField.drawTextBox();

            // Draw threshold label
            this.fontRenderer.drawString(I18n.format("gui.meiteminputbus.threshold.field.title"),
                    this.guiLeft + 265, this.guiTop + 120, 0xbfbfbf);

            // Show tooltip on hover
            if (mouseX >= this.guiLeft + 285 && mouseX <= this.guiLeft + 315 &&
                    mouseY >= this.guiTop + 110 && mouseY <= this.guiTop + 132) {
                List<String> tooltip = new ArrayList<>();
                tooltip.add(TextFormatting.GRAY + I18n.format("gui.meiteminputbus.threshold.field.tooltip"));
                this.drawHoveringText(tooltip, mouseX, mouseY);
            }
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        if (owner.getMERequestMode() == MEItemInputBus.MERequestMode.THRESHOLD) {
            thresholdField.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (thresholdField.isFocused()) {
            if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_ESCAPE) {
                // Submit value on Enter/Escape
                thresholdField.setFocused(false);
                validateAndSendThreshold();
                return;
            }

            // Only allow numerical input
            if (Character.isDigit(typedChar) || keyCode == Keyboard.KEY_BACK || keyCode == Keyboard.KEY_DELETE) {
                thresholdField.textboxKeyTyped(typedChar, keyCode);

                // Only validate when we have text
                if (!thresholdField.getText().isEmpty()) {
                    try {
                        int value = Integer.parseInt(thresholdField.getText());
                        if (value < 1) {
                            thresholdField.setText("1");
                        } else if (value > 1e7) {
                            thresholdField.setText("10000000");
                        }
                    } catch (NumberFormatException e) {
                        // Reset to previous valid value
                        thresholdField.setText(String.valueOf(thresholdValue));
                    }
                }
                return;
            }
        }

        super.keyTyped(typedChar, keyCode);
    }

    private void validateAndSendThreshold() {
        try {
            int newValue = Integer.parseInt(thresholdField.getText());
            if (newValue > 0 && newValue <= 1e7 && newValue != thresholdValue) {
                thresholdValue = newValue;
                // Send to server
                ModularMachinery.NET_CHANNEL.sendToServer(new PktMEInputBusInvAction(
                        PktMEInputBusInvAction.Action.SET_THRESHOLD, thresholdValue));
            } else {
                // Reset to current value
                thresholdField.setText(String.valueOf(thresholdValue));
            }
        } catch (NumberFormatException e) {
            // Reset to current value
            thresholdField.setText(String.valueOf(thresholdValue));
        }
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();

        // Make sure we send the threshold value when closing the GUI
        if (thresholdField != null && thresholdField.isFocused()) {
            validateAndSendThreshold();
        }
    }

    private static int getAddAmount() {
        int addAmount;
        // I prefer powers of two values :3
        // For real is better move it to the client config (Later maybe)
        // SHIFT + CTRL + ALT 1048576
        // ALT + CTRL         65536
        // ALT + SHIFT        8192
        // SHIFT + CTRL       1024
        // CTRL               128
        // SHIFT              16
        if (isShiftDown() && isControlDown() && isAltDown()) {
            addAmount = 1_048_576;
        } else if (isAltDown() && isControlDown()) {
            addAmount = 65536;
        } else if (isAltDown() && isShiftDown()) {
            addAmount = 8192;
        } else if (isShiftDown() && isControlDown()) {
            addAmount = 1024;
        } else if (isControlDown()) {
            addAmount = 128;
        } else if (isShiftDown()) {
            addAmount = 16;
        } else {
            addAmount = 1;
        }
        return addAmount;
    }

    private static List<String> getAddActionInfo() {
        List<String> tooltip = new ArrayList<>();
        tooltip.add(TextFormatting.GRAY + I18n.format("gui.meiteminputbus.inv_action"));
        // Quite a sight, isn't it?
        String addAmount = MiscUtils.formatDecimal(getAddAmount());
        if (isShiftDown() && isControlDown() && isAltDown()) {
            tooltip.add(TextFormatting.GRAY + I18n.format("gui.meiteminputbus.inv_action.increase",
                    "SHIFT + CTRL + ALT", addAmount));
            tooltip.add(TextFormatting.GRAY + I18n.format("gui.meiteminputbus.inv_action.decrease",
                    "SHIFT + CTRL + ALT", addAmount));
        } else if (isAltDown() && isControlDown()) {
            tooltip.add(TextFormatting.GRAY + I18n.format("gui.meiteminputbus.inv_action.increase",
                    "CTRL + ALT", addAmount));
            tooltip.add(TextFormatting.GRAY + I18n.format("gui.meiteminputbus.inv_action.decrease",
                    "CTRL + ALT", addAmount));
        } else if (isAltDown() && isShiftDown()) {
            tooltip.add(TextFormatting.GRAY + I18n.format("gui.meiteminputbus.inv_action.increase",
                    "SHIFT + ALT", addAmount));
            tooltip.add(TextFormatting.GRAY + I18n.format("gui.meiteminputbus.inv_action.decrease",
                    "SHIFT + ALT", addAmount));
        } else if (isShiftDown() && isControlDown()) {
            tooltip.add(TextFormatting.GRAY + I18n.format("gui.meiteminputbus.inv_action.increase",
                    "SHIFT + CTRL", addAmount));
            tooltip.add(TextFormatting.GRAY + I18n.format("gui.meiteminputbus.inv_action.decrease",
                    "SHIFT + CTRL", addAmount));
        } else if (isControlDown()) {
            tooltip.add(TextFormatting.GRAY + I18n.format("gui.meiteminputbus.inv_action.increase",
                    "CTRL", addAmount));
            tooltip.add(TextFormatting.GRAY + I18n.format("gui.meiteminputbus.inv_action.decrease",
                    "CTRL", addAmount));
        } else if (isShiftDown()) {
            tooltip.add(TextFormatting.GRAY + I18n.format("gui.meiteminputbus.inv_action.increase",
                    "SHIFT", addAmount));
            tooltip.add(TextFormatting.GRAY + I18n.format("gui.meiteminputbus.inv_action.decrease",
                    "SHIFT", addAmount));
        } else {
            tooltip.add(TextFormatting.GRAY + I18n.format("gui.meiteminputbus.inv_action.increase.normal"));
            tooltip.add(TextFormatting.GRAY + I18n.format("gui.meiteminputbus.inv_action.decrease.normal"));
        }
        return tooltip;
    }

    private static boolean isAltDown() {
        return Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU);
    }

    private static boolean isControlDown() {
        return Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
    }

    private static boolean isShiftDown() {
        return Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();

        final int i = Mouse.getEventDWheel();
        if (i != 0) {
            final int x = Mouse.getEventX() * this.width / this.mc.displayWidth;
            final int y = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
            this.onMouseWheelEvent(x, y, i / Math.abs(i));
        }
    }

    /**
     * Override AE2EL mouseWheelEvent() to prevent SHIFT = 11.
     */
    protected void mouseWheelEvent(final int x, final int y, final int wheel) {
    }

    protected void onMouseWheelEvent(final int x, final int y, final int wheel) {
        final Slot slot = this.getSlot(x, y);
        if (!(slot instanceof SlotFake)) {
            return;
        }
        final ItemStack stack = slot.getStack();
        if (stack.isEmpty()) {
            return;
        }

        int amount = wheel < 0 ? -getAddAmount() : getAddAmount();
        int stackCount = stack.getCount();

        if (amount > 0) {
            if (stackCount + amount > slot.getSlotStackLimit()) {
                return;
            }
        } else if (stackCount - amount <= 0) {
            return;
        }

        this.invActionAmount += amount;
        ClientProxy.clientScheduler.addRunnable(() -> sendInvActionToServer(slot.slotNumber), 0);
    }

    public void sendInvActionToServer(int slotNumber) {
        if (invActionAmount == 0) {
            return;
        }
        ModularMachinery.NET_CHANNEL.sendToServer(new PktMEInputBusInvAction(
                PktMEInputBusInvAction.Action.ADD_AMOUNT, invActionAmount, slotNumber
        ));
        invActionAmount = 0;
    }

    @Override
    public void drawFG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        this.fontRenderer.drawString(I18n.format("gui.meiteminputbus.title"), 0, -ySize, 0xbfbfbf);
        this.fontRenderer.drawString(GuiText.Config.getLocal(), 8, -10, 0xbfbfbf);
        this.fontRenderer.drawString(GuiText.StoredItems.getLocal(), xSize / 2 + 8, -10, 0xbfbfbf);
        this.fontRenderer.drawString(GuiText.inventory.getLocal(), 10, this.ySize - 98, 0x404040);

        super.drawFG(offsetX, offsetY, mouseX, mouseY);
    }

    @Override
    public void drawBG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(TEXTURES_INPUT_BUS);
        this.drawTexturedModalRect(offsetX, offsetY, 0, 0, this.xSize, this.ySize);

        int i = (this.width - this.xSize) / 2;
        int j = (this.height - this.ySize) / 2;

        // Fake
        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 6; x++) {
                GuiUtils.drawTexturedModalRect(i + 11 + x * 18, j +8 + y * 18, 0, 205, 18, 18, 2);
            }
        }

        int slotOffsetX = 7 * 18;
        // Real
        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 6; x++) {
                GuiUtils.drawTexturedModalRect(i + 11 + x * 18 + slotOffsetX, j +8 + y * 18, 18, 205, 18, 18, 2);

            }
        }

        super.drawBG(offsetX, offsetY, mouseX, mouseY);
    }

    @Override
    protected void renderToolTip(@Nonnull final ItemStack stack, final int x, final int y) {
        final FontRenderer font = stack.getItem().getFontRenderer(stack);
        GuiUtils.preItemToolTip(stack);

        final List<String> tooltip = this.getItemToolTip(stack);
        final Slot slot = getSlot(x, y);
        if (slot instanceof SlotFake) {
            final String formattedAmount = MiscUtils.formatDecimal((stack.getCount()));
            final String formatted = I18n.format("gui.meiteminputbus.items_marked", formattedAmount);
            tooltip.add(TextFormatting.GRAY + formatted);
            tooltip.addAll(getAddActionInfo());

            I18n.format("gui.meiteminputbus.items_marked", formattedAmount);
        } else if (slot instanceof SlotDisabled) {
            final String formattedAmount = NumberFormat.getNumberInstance(Locale.US).format(stack.getCount());
            final String formatted = I18n.format("gui.meitembus.item_cached", formattedAmount);
            tooltip.add(TextFormatting.GRAY + formatted);
        }

        this.drawHoveringText(tooltip, x, y, (font == null ? fontRenderer : font));
        GuiUtils.postItemToolTip();
    }

    public MEItemInputBus getOwner() {
        return owner;
    }

    public void updateGUIState() {
        slotProcessModeSetting.setCurrentSelection(owner.getMERequestMode());

        // Update threshold field state based on current mode
        if (thresholdField != null) {
            boolean isThresholdMode = owner.getMERequestMode() == MEItemInputBus.MERequestMode.THRESHOLD;
            thresholdField.setVisible(isThresholdMode);
            thresholdField.setEnabled(isThresholdMode);

            // Update threshold value from owner if available
            int ownerThreshold = owner.getThresholdValue();
            if (ownerThreshold > 0) {
                thresholdValue = ownerThreshold;
                thresholdField.setText(String.valueOf(thresholdValue));
            }
        }
    }
}