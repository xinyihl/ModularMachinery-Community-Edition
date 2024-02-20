package github.kasuminova.mmce.client.gui.widget.preview;

import github.kasuminova.mmce.client.gui.widget.HorizontalLine;
import github.kasuminova.mmce.client.gui.widget.MultiLineLabel;
import github.kasuminova.mmce.client.gui.widget.base.WidgetGui;
import github.kasuminova.mmce.client.gui.widget.container.Column;
import hellfirepvp.modularmachinery.common.util.MiscUtils;
import net.minecraft.client.resources.I18n;

import java.util.Collections;

public class PreviewCompilerProgressbar extends Column {

    protected final WorldSceneRendererWidget renderer;

    protected final MultiLineLabel messageLabel = new MultiLineLabel(Collections.emptyList());
    protected final HorizontalLine progressLine = new HorizontalLine();

    protected int maxWidth = 0;

    protected boolean hasProgress = false;

    public PreviewCompilerProgressbar(WorldSceneRendererWidget renderer) {
        this.renderer = renderer;
    }

    @Override
    public void initWidget(final WidgetGui gui) {
        super.initWidget(gui);
        messageLabel.setAutoRecalculateSize(false)
                .setScale(.72f)
                .setHeight(MultiLineLabel.DEFAULT_FONT_HEIGHT)
                .setMarginLeft(22);
        progressLine.setColor(0xFF87CEFA)
                .setHeight(2);
    }

    @Override
    public void update(final WidgetGui gui) {
        super.update(gui);
        float progress = renderer.getWorldRenderer().getCompileProgress();
        if (progress <= 0) {
            if (hasProgress) {
                getWidgets().clear();
                hasProgress = false;
            }
            return;
        }
        if (!hasProgress) {
            addWidgets(progressLine, messageLabel);
            hasProgress = true;
        }
        messageLabel.setContents(Collections.singletonList(I18n.format(
                "gui.preview.compiling.progress", MiscUtils.formatFloat(progress * 100f, 1)))
        );
        progressLine.setWidth((int) Math.floor(maxWidth * progress));
    }

    public PreviewCompilerProgressbar setMaxWidth(final int maxWidth) {
        this.maxWidth = maxWidth;
        this.messageLabel.setWidth(maxWidth - messageLabel.getMarginLeft() - messageLabel.getMarginRight());
        return this;
    }
}
