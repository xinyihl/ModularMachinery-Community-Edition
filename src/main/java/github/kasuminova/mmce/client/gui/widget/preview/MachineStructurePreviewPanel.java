package github.kasuminova.mmce.client.gui.widget.preview;

import com.cleanroommc.client.util.RenderUtils;
import github.kasuminova.mmce.client.gui.widget.Button4State;
import github.kasuminova.mmce.client.gui.widget.Button5State;
import github.kasuminova.mmce.client.gui.widget.container.Row;
import github.kasuminova.mmce.client.gui.widget.slot.SlotVirtual;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.client.ClientScheduler;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.util.BlockArray;
import hellfirepvp.modularmachinery.common.util.ItemUtils;
import ink.ikx.mmce.common.utils.StackUtils;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class MachineStructurePreviewPanel extends Row {
    public static final ResourceLocation WIDGETS_TEX_LOCATION = new ResourceLocation(ModularMachinery.MODID,
            "textures/gui/guiblueprint_new.png");

    public static final int PANEL_WIDTH = 184;
    public static final int PANEL_HEIGHT = 220;

    public static final int WORLD_RENDERER_WIDTH = 172;
    public static final int WORLD_RENDERER_HEIGHT = 150;

    protected final WorldSceneRendererWidget renderer;

    public MachineStructurePreviewPanel(final DynamicMachine machine) {
        renderer = (WorldSceneRendererWidget) new WorldSceneRendererWidget(machine)
                .setWidthHeight(WORLD_RENDERER_WIDTH, WORLD_RENDERER_HEIGHT)
                .setAbsXY(6, 26);

        // ====================
        // Widgets...
        // ====================

        // Buttons, at preview bottom...
        Button5State menuBtn = new Button5State();
        Button5State toggleLayerRender = new Button5State();
        Button4State placeWorldPreview = new Button4State();
        Button4State dynamicPatternPlus = new Button4State();
        Button4State dynamicPatternSubtract = new Button4State();

        // Buttons, at preview top...
        Button5State toggleFormed = new Button5State();
        Button5State showUpgrades = new Button5State();

        // Ingredient list, at panel bottom...
        IngredientList ingredientList = (IngredientList) new IngredientList()
                .setAbsXY(5, 179);

        // Selected block ingredient main stack, at left...
        SlotVirtual selectedBlockIngredientMain = (SlotVirtual) SlotVirtual.ofJEI()
                .setSlotTexX(224).setSlotTexY(105)
                .setSlotTexLocation(WIDGETS_TEX_LOCATION)
                .setAbsXY(8, 28);
        // Selected block ingredient list, at left, under the selectedBlockIngredientMain...
        SelectedBlockIngredientList selectedBlockIngredientList = (SelectedBlockIngredientList) new SelectedBlockIngredientList()
                .setAbsXY(8, 48);

        // 2D layer scrollbar, and 2 buttons, only shown when 2D preview enabled, at right...
        LayerRenderScrollbar layerScrollbar = (LayerRenderScrollbar) new LayerRenderScrollbar()
                .setAbsXY(165, 44)
                .setEnabled(false);

        // ====================
        // Initialize...
        // ====================

        menuBtn.setClickedTextureXY(184 + 15 + 15, 15)
                .setMouseDownTextureXY(184 + 15 + 15, 15)
                .setHoveredTextureXY(184 + 15, 15)
                .setTextureXY(184, 15)
                .setTextureLocation(WIDGETS_TEX_LOCATION)
                .setWidthHeight(13, 13);
        toggleLayerRender.setClickedTextureXY(184 + 15 + 15 + 15, 30)
                .setMouseDownTextureXY(184 + 15 + 15, 30)
                .setHoveredTextureXY(184 + 15, 30)
                .setTextureXY(184,  15 + 15)
                .setTextureLocation(WIDGETS_TEX_LOCATION)
                .setWidthHeight(13, 13);
        placeWorldPreview
                .setMouseDownTextureXY(184 + 15 + 15, 45)
                .setHoveredTextureXY(184 + 15, 45)
                .setTextureXY(184, 45)
                .setTextureLocation(WIDGETS_TEX_LOCATION)
                .setWidthHeight(13, 13);
        dynamicPatternPlus
                .setMouseDownTextureXY(184 + 15 + 15, 60)
                .setHoveredTextureXY(184 + 15, 60)
                .setTextureXY(184, 60)
                .setTextureLocation(WIDGETS_TEX_LOCATION)
                .setWidthHeight(13, 13);
        dynamicPatternSubtract
                .setMouseDownTextureXY(184 + 15 + 15, 75)
                .setHoveredTextureXY(184 + 15, 75)
                .setTextureXY(184, 75)
                .setTextureLocation(WIDGETS_TEX_LOCATION)
                .setWidthHeight(13, 13);

        toggleFormed.setClickedTextureXY(184 + 15 + 15 + 15, 0)
                .setMouseDownTextureXY(184 + 15 + 15, 0)
                .setHoveredTextureXY(184 + 15, 0)
                .setTextureXY(184, 0)
                .setTextureLocation(WIDGETS_TEX_LOCATION)
                .setWidthHeight(13, 13);
        showUpgrades.setClickedTextureXY(184 + 15 + 15 + 15, 90)
                .setMouseDownTextureXY(184 + 15 + 15, 90)
                .setHoveredTextureXY(184 + 15, 90)
                .setTextureXY(184, 90)
                .setTextureLocation(WIDGETS_TEX_LOCATION)
                .setWidthHeight(13, 13);

        // ====================
        // Assembly...
        // ====================

        Row bottomMenu = new Row();
        if (!machine.getDynamicPatterns().isEmpty()) {
            bottomMenu.addWidgets(
                    dynamicPatternPlus.setMarginRight(2).setDisabled(true),
                    dynamicPatternSubtract.setMarginRight(2).setDisabled(true)
            );
        }
        bottomMenu.addWidgets(
                placeWorldPreview.setMarginRight(2).setDisabled(true),
                toggleLayerRender.setMarginRight(2).setDisabled(true),
                menuBtn.setMarginRight(2)
        );
        bottomMenu.setAbsXY(PANEL_WIDTH - (bottomMenu.getWidth() + 6), 161);

        Row topMenu = (Row) new Row().addWidgets(
                showUpgrades.setMarginRight(2),
                toggleFormed.setMarginRight(2)
        );
        topMenu.setAbsXY(PANEL_WIDTH - (topMenu.getWidth() + 6), 28);

        // Add all widgets to panel...
        addWidgets(topMenu, bottomMenu,
                ingredientList,
                selectedBlockIngredientMain.setDisabled(true), selectedBlockIngredientList.setDisabled(true),
                layerScrollbar
        );
        addWidget(renderer);

        // ====================
        // EventHandlers...
        // ====================

        menuBtn.setOnClickedListener(btn -> {
            boolean enable = menuBtn.isClicked();
            bottomMenu.getWidgets().stream()
                    .filter(widget -> widget != menuBtn)
                    .forEach(widget -> widget.setEnabled(enable));
            bottomMenu.setAbsXY(PANEL_WIDTH - (bottomMenu.getWidth() + 6), 161);
        });
        toggleLayerRender.setOnClickedListener(btn -> {
            boolean layerRender = toggleLayerRender.isClicked();
            if (layerRender) {
                renderer.useLayerRender();
                int minY = renderer.getPattern().getMin().getY();
                int maxY = renderer.getPattern().getMax().getY();
                layerScrollbar.setEnabled(true);
                layerScrollbar.getScrollbar().setRange(minY, maxY);
                renderer.setRenderLayer(maxY - minY);
            } else {
                renderer.use3DRender();
                layerScrollbar.setDisabled(true);
            }
        });
        if (!machine.getDynamicPatterns().isEmpty()) {
            dynamicPatternPlus.setOnClickedListener(btn ->
                    renderer.setDynamicPatternSize(renderer.getDynamicPatternSize() + 1));
            dynamicPatternSubtract.setOnClickedListener(btn ->
                    renderer.setDynamicPatternSize(Math.max(renderer.getDynamicPatternSize() - 1, 0)));
        }
        layerScrollbar.setOnScrollChanged(renderLayer -> {
            int range = layerScrollbar.getScrollbar().getRange();
            renderer.setRenderLayer(range - renderLayer);
        });
        renderer.setOnPatternUpdate(r -> {
            List<ItemStack> stackList = renderer.getPattern().getDescriptiveStackList(ClientScheduler.getClientTick());
            ingredientList.setStackList(stackList.stream()
                    .sorted(Comparator.comparingInt(ItemStack::getCount).reversed())
                    .collect(Collectors.toList()));
        });
        renderer.setOnBlockSelected(relativePos -> {
            BlockPos pos = relativePos == null ? null : relativePos.subtract(renderer.getRenderOffset());
            if (pos == null) {
                selectedBlockIngredientMain.setStackInSlot(ItemStack.EMPTY);
                selectedBlockIngredientMain.setDisabled(true);
                selectedBlockIngredientList.setStackList(Collections.emptyList());
                selectedBlockIngredientList.setDisabled(true);
                renderer.getWorldRenderer().setAfterWorldRender(null);
                return;
            }

            IBlockState clickedBlock = renderer.getWorldRenderer().getWorld().getBlockState(pos);
            BlockArray.BlockInformation clicked = renderer.getPattern().getPattern().get(pos);
            ItemStack clickedBlockStack = StackUtils.getStackFromBlockState(clickedBlock);
            List<ItemStack> replaceable = clicked.getIngredientList().stream()
                    .filter(replaceableStack -> !ItemUtils.matchStacks(clickedBlockStack, replaceableStack))
                    .collect(Collectors.toList());
            selectedBlockIngredientMain.setStackInSlot(clickedBlockStack);
            selectedBlockIngredientMain.setEnabled(true);
            selectedBlockIngredientList.setStackList(replaceable);
            selectedBlockIngredientList.setEnabled(!replaceable.isEmpty());

            renderer.getWorldRenderer().setAfterWorldRender(worldRenderer ->
                    RenderUtils.renderBlockOverLay(pos, .6f, 0f, 0f, 1f, 1.01f));
        });
    }
}
