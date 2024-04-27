package github.kasuminova.mmce.client.gui;

import github.kasuminova.mmce.client.gui.widget.Button4State;
import github.kasuminova.mmce.client.gui.widget.Button5State;
import github.kasuminova.mmce.client.gui.widget.MultiLineLabel;
import github.kasuminova.mmce.client.gui.widget.base.WidgetController;
import github.kasuminova.mmce.client.gui.widget.base.WidgetGui;
import github.kasuminova.mmce.client.gui.widget.container.Row;
import github.kasuminova.mmce.client.gui.widget.impl.patternprovider.PatternProviderIngredientList;
import github.kasuminova.mmce.common.container.ContainerMEPatternProvider;
import github.kasuminova.mmce.common.network.PktMEPatternProviderAction;
import github.kasuminova.mmce.common.tile.MEPatternProvider;
import github.kasuminova.mmce.common.util.InfItemFluidHandler;
import hellfirepvp.modularmachinery.ModularMachinery;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GuiMEPatternProvider extends AEBaseGuiContainerDynamic {

    public static final ResourceLocation GUI_TEXTURE =
            new ResourceLocation(ModularMachinery.MODID, "textures/gui/mepatternprovider.png");

    protected final MEPatternProvider owner;

    protected final PatternProviderIngredientList stackList = new PatternProviderIngredientList();
    protected final Button5State toggleBlockingMode = new Button5State();

    public GuiMEPatternProvider(final MEPatternProvider owner, final EntityPlayer player) {
        super(new ContainerMEPatternProvider(owner, player));
        this.owner = owner;
        this.xSize = 256;
        this.ySize = 196;
        this.guiLeft = (this.width - this.xSize) / 2;
        this.guiTop = (this.height - this.ySize) / 2;

        this.widgetController = new WidgetController(WidgetGui.of(this, this.xSize, this.ySize, guiLeft, guiTop));

        // Titles...
        this.widgetController.addWidget(
                new MultiLineLabel(Collections.singletonList(I18n.format("gui.mepatternprovider.title")))
                        .setAutoWrap(false)
                        .setMargin(0)
                        .setAbsXY(7, 11)
        );
        this.widgetController.addWidget(
                new MultiLineLabel(Collections.singletonList(I18n.format("gui.mepatternprovider.cached")))
                        .setAutoWrap(false)
                        .setMargin(0)
                        .setAbsXY(180, 11)
        );
        this.widgetController.addWidget(
                new MultiLineLabel(Collections.singletonList(I18n.format("gui.mepatternprovider.inventory")))
                        .setAutoWrap(false)
                        .setMargin(0)
                        .setAbsXY(7, 101)
        );

        // Init StackList...
        stackList.setMaxStackPerRow(3);
        stackList.setWidthHeight(69, 162);
        stackList.setAbsXY(180, 27);

        // Init ReturnItems...
        Button4State returnItems = new Button4State();
        returnItems
                .setMouseDownTextureXY(176 + 18 + 18, 214)
                .setHoveredTextureXY(176 + 18, 214)
                .setTextureXY(176, 214)
                .setTextureLocation(GuiMEPatternProvider.GUI_TEXTURE)
                .setTooltipFunction((btn) -> {
                    List<String> tooltips = new ArrayList<>();
                    tooltips.add(I18n.format("gui.mepatternprovider.return_items"));
                    tooltips.add(I18n.format("gui.mepatternprovider.return_items.desc"));
                    return tooltips;
                })
                .setOnClickedListener((btn) -> ModularMachinery.NET_CHANNEL.sendToServer(new PktMEPatternProviderAction(PktMEPatternProviderAction.Action.RETURN_ITEMS)))
                .setWidthHeight(16, 16);

        // Init ToggleBlockingMode...
        toggleBlockingMode
                .setClickedTextureXY(176 + 18 + 18 + 18, 196)
                .setMouseDownTextureXY(176 + 18 + 18, 196)
                .setHoveredTextureXY(176 + 18, 196)
                .setTextureXY(176, 196)
                .setTextureLocation(GuiMEPatternProvider.GUI_TEXTURE)
                .setTooltipFunction((btn) -> {
                    List<String> tooltips = new ArrayList<>();
                    if (toggleBlockingMode.isClicked()) {
                        tooltips.add(I18n.format("gui.mepatternprovider.blocking_mode.disable"));
                        tooltips.add(I18n.format("gui.mepatternprovider.blocking_mode.enabled.desc"));
                    } else {
                        tooltips.add(I18n.format("gui.mepatternprovider.blocking_mode.enable"));
                        tooltips.add(I18n.format("gui.mepatternprovider.blocking_mode.disabled.desc"));
                    }
                    return tooltips;
                })
                .setOnClickedListener((btn) -> {
                    if (toggleBlockingMode.isClicked()) {
                        ModularMachinery.NET_CHANNEL.sendToServer(new PktMEPatternProviderAction(PktMEPatternProviderAction.Action.ENABLE_BLOCKING_MODE));
                    } else {
                        ModularMachinery.NET_CHANNEL.sendToServer(new PktMEPatternProviderAction(PktMEPatternProviderAction.Action.DISABLE_BLOCKING_MODE));
                    }
                })
                .setWidthHeight(16, 16);

        // Init Widget Containers...
        Row buttons = new Row();
        buttons.addWidgets(returnItems.setMarginRight(2), toggleBlockingMode).setAbsXY(215, 7);

        this.widgetController.addWidget(stackList);
        this.widgetController.addWidget(buttons);

        // Update state...
        updateGUIState();
    }

    @Override
    public void drawBG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(GUI_TEXTURE);
        this.drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        
        super.drawBG(offsetX, offsetY, mouseX, mouseY);
    }

    public MEPatternProvider getOwner() {
        return owner;
    }

    public void updateGUIState() {
        InfItemFluidHandler infHandler = owner.getInfHandler();
        stackList.setStackList(infHandler.getItemStackList(), infHandler.getFluidStackList());
        toggleBlockingMode.setClicked(owner.isBlockingMode());
    }

}
