package github.kasuminova.mmce.client.gui.util;

import github.kasuminova.mmce.client.gui.widget.base.DynamicWidget;
import github.kasuminova.mmce.client.gui.widget.base.WidgetGui;
import net.minecraft.client.gui.GuiScreen;

@FunctionalInterface
public interface RenderFunction {

    void doRender(DynamicWidget dynamicWidget, WidgetGui gui, RenderSize renderSize, RenderPos renderPos, MousePos mousePos);

}
