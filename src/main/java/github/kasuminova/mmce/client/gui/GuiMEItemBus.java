package github.kasuminova.mmce.client.gui;

import appeng.client.render.StackSizeRenderer;
import appeng.container.slot.AppEngSlot;
import appeng.util.item.AEItemStack;
import github.kasuminova.mmce.client.gui.slot.Size1Slot;
import github.kasuminova.mmce.client.gui.util.MousePos;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.base.Mods;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import java.util.List;

public abstract class GuiMEItemBus extends AEBaseGuiContainerDynamic {

    protected final StackSizeRenderer stackSizeRenderer = Mods.AE2EL.isPresent() ? null : new StackSizeRenderer();

    public GuiMEItemBus(final Container container) {
        super(container);
    }

    @Override
    public void drawSlot(final Slot s) {
        if (Mods.AE2EL.isPresent() || s.slotNumber < 36 || !(s instanceof AppEngSlot)) {
            super.drawSlot(s);
            return;
        }

        // Only for vanilla ae2.
        try {
            this.zLevel = 100.0F;
            this.itemRender.zLevel = 100.0F;

            if (!this.isPowered()) {
                drawRect(s.xPos, s.yPos, 16 + s.xPos, 16 + s.yPos, 0x66111111);
            }

            this.zLevel = 0.0F;
            this.itemRender.zLevel = 0.0F;

            // Annoying but easier than trying to splice into render item
            super.drawSlot(new Size1Slot((AppEngSlot) s));

            this.stackSizeRenderer.renderStackSize(this.fontRenderer, AEItemStack.fromItemStack(s.getStack()), s.xPos, s.yPos);
        } catch (final Exception err) {
            ModularMachinery.log.warn("Prevented crash while drawing slot.", err);
        }
    }

    @Override
    protected void renderHoveredToolTip(final int mouseX, final int mouseY) {
        updateHoveredSlot(mouseX, mouseY);

        final Slot slot = hoveredSlot;
        final ItemStack stackInSlot = (slot == null || !slot.getHasStack()) ? ItemStack.EMPTY : slot.getStack();

        if (!stackInSlot.isEmpty()) {
            renderToolTip(stackInSlot, mouseX, mouseY);
        } else {
            final List<String> hoverTooltips = widgetController.getHoverTooltips(new MousePos(mouseX, mouseY));
            if (!hoverTooltips.isEmpty()) {
                this.drawHoveringText(hoverTooltips, mouseX, mouseY);
            }
        }
    }

}
