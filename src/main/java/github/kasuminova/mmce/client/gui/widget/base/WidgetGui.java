package github.kasuminova.mmce.client.gui.widget.base;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;

public class WidgetGui {
    private final GuiScreen gui;
    private int xSize;
    private int ySize;

    public WidgetGui(final GuiScreen gui, final int xSize, final int ySize) {
        this.gui = gui;
        this.xSize = xSize;
        this.ySize = ySize;
    }

    public static WidgetGui of(final GuiContainer container) {
        return new WidgetGui(container, container.getXSize(), container.getYSize());
    }

    public static WidgetGui of(final GuiScreen container, final int xSize, final int ySize) {
        return new WidgetGui(container, xSize, ySize);
    }

    public GuiScreen getGui() {
        return gui;
    }

    public int getWidth() {
        return gui.width;
    }

    public int getHeight() {
        return gui.height;
    }

    public int getXSize() {
        return xSize;
    }

    public WidgetGui setXSize(final int xSize) {
        this.xSize = xSize;
        return this;
    }

    public int getYSize() {
        return ySize;
    }

    public WidgetGui setYSize(final int ySize) {
        this.ySize = ySize;
        return this;
    }
}
