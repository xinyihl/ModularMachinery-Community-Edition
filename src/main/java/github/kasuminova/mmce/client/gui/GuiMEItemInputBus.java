package github.kasuminova.mmce.client.gui;

import appeng.container.slot.SlotDisabled;
import appeng.container.slot.SlotFake;
import appeng.core.localization.GuiText;
import github.kasuminova.mmce.client.gui.util.MousePos;
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

import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GuiMEItemInputBus extends GuiMEItemBus {
    private static final ResourceLocation TEXTURES_INPUT_BUS = new ResourceLocation(ModularMachinery.MODID, "textures/gui/meiteminputbus.png");
    private static final int MAX_THRESHOLD_VALUE = 10_000_000;
    private static final int DEFAULT_THRESHOLD_VALUE = 256;

    private int invActionAmount = 0;
    private GuiTextField thresholdField;
    private int thresholdValue = DEFAULT_THRESHOLD_VALUE;

    protected final ButtonElements<MEItemInputBus.MERequestMode> modeButtonElements = new ButtonElements<>();
    protected final MEItemInputBus owner;

    // Local state
    private MEItemInputBus.MERequestMode localRequestMode;

    public GuiMEItemInputBus(final MEItemInputBus owner, final EntityPlayer player) {
        super(new ContainerMEItemInputBus(owner, player));

        this.owner = owner;
        this.ySize = 205;
        this.xSize = 256;

        initWidgetController();
        initModeButtonElements();

        // Update state...
        updateGUIState();
    }

    private void initWidgetController() {
        this.guiLeft = (this.width - this.ySize) / 2;
        this.guiTop = (this.height - this.ySize) / 2;

        this.widgetController = new WidgetController(WidgetGui.of(this, this.xSize, this.ySize, guiLeft, guiTop));
    }

    private void initModeButtonElements() {
        modeButtonElements
                .addElement(MEItemInputBus.MERequestMode.DEFAULT, TextureProperties.of(0, 239, 16, 16))
                .addElement(MEItemInputBus.MERequestMode.THRESHOLD, TextureProperties.of(16, 239, 16, 16))
                .setMouseDownTexture(32, 223)
                .setHoveredTexture(16, 223)
                .setTexture(0, 223)
                .setTextureLocation(TEXTURES_INPUT_BUS)
                .setTooltipFunction(this::createModeTooltips)
                .setOnClickedListener(this::handleModeButtonClick)
                .setWidthHeight(16, 16);

        // Init Widget Containers...
        Row stackListButtons = new Row();
        stackListButtons.addWidgets(modeButtonElements).setAbsXY(265, 100);

        this.widgetController.addWidget(stackListButtons);
    }

    private List<String> createModeTooltips(Object btn) {
        MEItemInputBus.MERequestMode current = modeButtonElements.getCurrentSelection();
        List<String> tooltips = new ArrayList<>();
        tooltips.add(I18n.format("gui.meiteminputbus.work_mode.desc"));
        tooltips.add((current == MEItemInputBus.MERequestMode.DEFAULT ? I18n.format("gui.meiteminputbus.current") : "")
                + I18n.format("gui.meiteminputbus.default.desc"));
        tooltips.add((current == MEItemInputBus.MERequestMode.THRESHOLD ? I18n.format("gui.meiteminputbus.current") : "")
                + I18n.format("gui.meiteminputbus.threshold.desc"));
        return tooltips;
    }

    private void handleModeButtonClick(Object btn) {
        MEItemInputBus.MERequestMode current = modeButtonElements.getCurrentSelection();
        if (current == null) {
            return;
        }

        // Update local state
        localRequestMode = current;

        // Update server state
        PktMEInputBusInvAction.Action action = current == MEItemInputBus.MERequestMode.DEFAULT ?
                PktMEInputBusInvAction.Action.ENABLE_DEFAULT_MODE :
                PktMEInputBusInvAction.Action.ENABLE_THRESHOLD_MODE;
        ModularMachinery.NET_CHANNEL.sendToServer(new PktMEInputBusInvAction(action));

        // Update threshold field visibility
        toggleThresholdField(current != MEItemInputBus.MERequestMode.DEFAULT);
    }

    void toggleThresholdField(boolean visible) {
        if (thresholdField != null) {
            thresholdField.setVisible(visible);
            thresholdField.setEnabled(visible);
        }
    }

    @Override
    public void initGui() {
        super.initGui();

        thresholdField = new GuiTextField(0, this.fontRenderer, this.guiLeft + 265, this.guiTop + 135, 70, 12);
        thresholdField.setMaxStringLength(7);
        thresholdField.setText(String.valueOf(thresholdValue));
        thresholdField.setEnableBackgroundDrawing(true);
        thresholdField.setVisible(true);
        thresholdField.setEnabled(true);
        thresholdField.setTextColor(0xFFFFFF);
        thresholdField.setDisabledTextColour(0x7F7F7F);
        thresholdField.setCanLoseFocus(true);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);

        // Draw threshold field only when in threshold mode
        if (localRequestMode == MEItemInputBus.MERequestMode.THRESHOLD) {
            thresholdField.drawTextBox();

            // Draw threshold label
            this.fontRenderer.drawString(I18n.format("gui.meiteminputbus.threshold.field.title"),
                    this.guiLeft + 265, this.guiTop + 120, 0xbfbfbf);

            // Show tooltip on hover
            if (isMouseOverThresholdLabel(mouseX, mouseY)) {
                List<String> tooltip = new ArrayList<>();
                tooltip.add(TextFormatting.GRAY + I18n.format("gui.meiteminputbus.threshold.field.tooltip"));
                this.drawHoveringText(tooltip, mouseX, mouseY);
            }
        }
    }

    private boolean isMouseOverThresholdLabel(int mouseX, int mouseY) {
        return mouseX >= this.guiLeft + 285 && mouseX <= this.guiLeft + 315 &&
                mouseY >= this.guiTop + 110 && mouseY <= this.guiTop + 132;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        if (localRequestMode == MEItemInputBus.MERequestMode.THRESHOLD) {
            thresholdField.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (!thresholdField.isFocused()) {
            super.keyTyped(typedChar, keyCode);
            return;
        }

        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_ESCAPE) {
            // Submit value on Enter/Escape
            thresholdField.setFocused(false);
            validateAndSendThreshold();
            return;
        }

        // Only allow numerical input
        if (Character.isDigit(typedChar) || keyCode == Keyboard.KEY_BACK || keyCode == Keyboard.KEY_DELETE) {
            thresholdField.textboxKeyTyped(typedChar, keyCode);
            validateThresholdInput();
        }
    }

    private void validateThresholdInput() {
        // Only validate when we have text
        if (!thresholdField.getText().isEmpty()) {
            try {
                int value = Integer.parseInt(thresholdField.getText());
                if (value < 1) {
                    thresholdField.setText("1");
                } else if (value > MAX_THRESHOLD_VALUE) {
                    thresholdField.setText(String.valueOf(MAX_THRESHOLD_VALUE));
                }
            } catch (NumberFormatException e) {
                // Reset to previous valid value
                thresholdField.setText(String.valueOf(thresholdValue));
            }
        }
    }

    private void validateAndSendThreshold() {
        try {
            int newValue = Integer.parseInt(thresholdField.getText());

            if (newValue > 0 && newValue <= MAX_THRESHOLD_VALUE && newValue != thresholdValue) {
                thresholdValue = newValue;
                // Send to server
                ModularMachinery.NET_CHANNEL.sendToServer(new PktMEInputBusInvAction(
                        PktMEInputBusInvAction.Action.SET_THRESHOLD, thresholdValue));
            } else {
                thresholdField.setText(String.valueOf(thresholdValue));
            }
        } catch (NumberFormatException e) {
            thresholdField.setText(String.valueOf(thresholdValue));
        }
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        validateAndSendThreshold();
    }

    private static int getAddAmount() {
        // Modified to use a more structured approach with descriptive method
        if (isShiftDown() && isControlDown() && isAltDown()) {
            return 1_048_576; // SHIFT + CTRL + ALT
        } else if (isAltDown() && isControlDown()) {
            return 65536;     // ALT + CTRL
        } else if (isAltDown() && isShiftDown()) {
            return 8192;      // ALT + SHIFT
        } else if (isShiftDown() && isControlDown()) {
            return 1024;      // SHIFT + CTRL
        } else if (isControlDown()) {
            return 128;       // CTRL
        } else if (isShiftDown()) {
            return 16;        // SHIFT
        } else {
            return 1;         // No modifier
        }
    }

    private static List<String> getAddActionInfo() {
        List<String> tooltip = new ArrayList<>();
        tooltip.add(TextFormatting.GRAY + I18n.format("gui.meiteminputbus.inv_action"));

        String addAmount = MiscUtils.formatDecimal(getAddAmount());
        String format = TextFormatting.GRAY + I18n.format("gui.meiteminputbus.inv_action.%s", "%s", addAmount);

        if (isShiftDown() && isControlDown() && isAltDown()) {
            addTooltipForModifier(tooltip, "SHIFT + CTRL + ALT", format);
        } else if (isAltDown() && isControlDown()) {
            addTooltipForModifier(tooltip, "CTRL + ALT", format);
        } else if (isAltDown() && isShiftDown()) {
            addTooltipForModifier(tooltip, "SHIFT + ALT", format);
        } else if (isShiftDown() && isControlDown()) {
            addTooltipForModifier(tooltip, "SHIFT + CTRL", format);
        } else if (isControlDown()) {
            addTooltipForModifier(tooltip, "CTRL", format);
        } else if (isShiftDown()) {
            addTooltipForModifier(tooltip, "SHIFT", format);
        } else {
            tooltip.add(TextFormatting.GRAY + I18n.format("gui.meiteminputbus.inv_action.increase.normal"));
            tooltip.add(TextFormatting.GRAY + I18n.format("gui.meiteminputbus.inv_action.decrease.normal"));
        }
        return tooltip;
    }

    private static void addTooltipForModifier(List<String> tooltip, String modifier, String format) {
        tooltip.add(String.format(format, "increase", modifier));
        tooltip.add(String.format(format, "decrease", modifier));
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

        final int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            final int x = Mouse.getEventX() * this.width / this.mc.displayWidth;
            final int y = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
            this.onMouseWheelEvent(x, y, wheel / Math.abs(wheel));
        }
    }

    /**
     * Override AE2EL mouseWheelEvent() to prevent SHIFT = 11.
     */
    protected void mouseWheelEvent(final int x, final int y, final int wheel) {
        // Intentionally empty to override parent behavior
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

        // Check if the operation would exceed limits
        if ((amount > 0 && stackCount + amount > slot.getSlotStackLimit()) ||
                (amount < 0 && stackCount + amount <= 0)) {
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
        this.fontRenderer.drawStringWithShadow(I18n.format("gui.meiteminputbus.title"), 0, -25, 0xffffff);
        this.fontRenderer.drawStringWithShadow(GuiText.Config.getLocal(), 8, -10, 0xbfbfbf);
        this.fontRenderer.drawStringWithShadow(GuiText.StoredItems.getLocal(), (float) xSize / 2 + 8, -10, 0xbfbfbf);
        this.fontRenderer.drawString(GuiText.inventory.getLocal(), 55, this.ySize - 98, 0x404040);

        super.drawFG(offsetX, offsetY, mouseX, mouseY);
    }

    @Override
    public void drawBG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(TEXTURES_INPUT_BUS);
        this.drawTexturedModalRect(offsetX, offsetY, 0, 0, this.xSize, this.ySize);

        int i = (this.width - this.xSize) / 2;
        int j = (this.height - this.ySize) / 2;

        drawSlots(i, j);

        super.drawBG(offsetX, offsetY, mouseX, mouseY);
    }

    private void drawSlots(int i, int j) {
        // Fake slots
        drawSlotGrid(i, j, 11, 8, 6, 5, 0, 205);

        // Real slots
        int slotOffsetX = 7 * 18;
        drawSlotGrid(i, j, 11 + slotOffsetX, 8, 6, 5, 18, 205);
    }

    @SuppressWarnings("SameParameterValue")
    private void drawSlotGrid(int baseX, int baseY, int startX, int startY, int cols, int rows, int u, int v) {
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                GuiUtils.drawTexturedModalRect(
                        baseX + startX + x * 18,
                        baseY + startY + y * 18,
                        u, v, 18, 18, 2
                );
            }
        }
    }

    @Override
    protected void renderHoveredToolTip(final int mouseX, final int mouseY) {
        updateHoveredSlot(mouseX, mouseY);

        ItemStack stackInSlot = hoveredSlot == null ? ItemStack.EMPTY : hoveredSlot.getStack();
        List<String> hoverTooltips = widgetController.getHoverTooltips(new MousePos(mouseX, mouseY));
        if (stackInSlot.isEmpty() && hoverTooltips.isEmpty()) {
            return;
        }

        List<String> itemTooltip = stackInSlot.isEmpty() ? new ArrayList<>() : this.getItemToolTip(stackInSlot);

        if (hoveredSlot instanceof SlotFake) {
            final String formattedAmount = String.valueOf(stackInSlot.getCount());
            itemTooltip.add(TextFormatting.GRAY + I18n.format("gui.meiteminputbus.items_marked", formattedAmount));
            itemTooltip.addAll(getAddActionInfo());
        } else if (hoveredSlot instanceof SlotDisabled) {
            final String formattedAmount = NumberFormat.getNumberInstance(Locale.US).format(stackInSlot.getCount());
            itemTooltip.add(TextFormatting.GRAY + I18n.format("gui.meitembus.item_cached", formattedAmount));
        }

        itemTooltip.addAll(hoverTooltips);

        FontRenderer font = stackInSlot.getItem().getFontRenderer(stackInSlot);
        GuiUtils.preItemToolTip(stackInSlot);
        this.drawHoveringText(itemTooltip, mouseX, mouseY, (font == null ? fontRenderer : font));
        GuiUtils.postItemToolTip();
    }

    public MEItemInputBus getOwner() {
        return owner;
    }

    public void updateGUIState() {
        // Update local state from owner
        localRequestMode = owner.getMERequestMode();
        thresholdValue = owner.getThresholdValue() > 0 ? owner.getThresholdValue() : DEFAULT_THRESHOLD_VALUE;
        modeButtonElements.setCurrentSelection(localRequestMode);

        if (thresholdField != null) {
            boolean isThresholdMode = localRequestMode == MEItemInputBus.MERequestMode.THRESHOLD;
            toggleThresholdField(isThresholdMode);

            if (isThresholdMode) {
                thresholdField.setText(String.valueOf(thresholdValue));
            }
        }
    }
}