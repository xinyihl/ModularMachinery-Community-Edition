package github.kasuminova.mmce.client.gui.widget;

import github.kasuminova.mmce.client.gui.util.MousePos;
import github.kasuminova.mmce.client.gui.util.RenderPos;
import github.kasuminova.mmce.client.gui.util.RenderSize;
import github.kasuminova.mmce.client.gui.widget.base.WidgetGui;

import java.util.Collections;
import java.util.List;

/**
 * Inherits the 4 states of the button with the addition of a persistent press state.
 */
@SuppressWarnings("unused")
public class Button5State extends Button4State {

    protected int clickedTextureX = 0;
    protected int clickedTextureY = 0;

    protected boolean clicked = false;

    @Override
    public void render(final WidgetGui gui, final RenderSize renderSize, final RenderPos renderPos, final MousePos mousePos) {
        if (!isVisible() || textureLocation == null) {
            return;
        }

        int texX;
        int texY;

        if (isUnavailable()) {
            texX = unavailableTextureX;
            texY = unavailableTextureY;
        } else if (clicked) {
            texX = clickedTextureX;
            texY = clickedTextureY;
        } else if (mouseDown) {
            texX = mouseDownTextureX;
            texY = mouseDownTextureY;
        } else if (isMouseOver(mousePos)) {
            texX = hoveredTextureX;
            texY = hoveredTextureY;
        } else {
            texX = textureX;
            texY = textureY;
        }

        gui.getGui().mc.getTextureManager().bindTexture(textureLocation);
        gui.getGui().drawTexturedModalRect(renderPos.posX(), renderPos.posY(), texX, texY, width, height);
    }

    @Override
    public boolean onMouseReleased(final MousePos mousePos, final RenderPos renderPos) {
        if (isVisible() && isMouseOver(mousePos) && mouseDown) {
            mouseDown = false;
            clicked = !clicked;
            if (onClickedListener != null) {
                onClickedListener.accept(this);
            }
            return true;
        }
        mouseDown = false;
        return false;
    }

    @Override
    public List<String> getHoverTooltips(final MousePos mousePos) {
        if (clicked) {
            return Collections.emptyList();
        }
        return super.getHoverTooltips(mousePos);
    }

    public Button5State setClickedTextureXY(final int clickedTextureX, final int clickedTextureY) {
        this.clickedTextureX = clickedTextureX;
        this.clickedTextureY = clickedTextureY;
        return this;
    }

    public int getClickedTextureX() {
        return clickedTextureX;
    }

    public Button5State setClickedTextureX(final int clickedTextureX) {
        this.clickedTextureX = clickedTextureX;
        return this;
    }

    public int getClickedTextureY() {
        return clickedTextureY;
    }

    public Button5State setClickedTextureY(final int clickedTextureY) {
        this.clickedTextureY = clickedTextureY;
        return this;
    }

    public boolean isClicked() {
        return clicked;
    }

    public Button5State setClicked(final boolean clicked) {
        this.clicked = clicked;
        return this;
    }
}
