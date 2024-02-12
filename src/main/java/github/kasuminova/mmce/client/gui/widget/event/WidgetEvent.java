package github.kasuminova.mmce.client.gui.widget.event;

import github.kasuminova.mmce.client.gui.widget.base.DynamicWidget;
import github.kasuminova.mmce.client.gui.widget.base.WidgetGui;
import net.minecraft.client.gui.GuiScreen;

public abstract class WidgetEvent extends GuiEvent {
    protected final DynamicWidget sender;

    public WidgetEvent(final WidgetGui gui, final DynamicWidget sender) {
        super(gui);
        this.sender = sender;
    }

    public DynamicWidget getSender() {
        return sender;
    }
}