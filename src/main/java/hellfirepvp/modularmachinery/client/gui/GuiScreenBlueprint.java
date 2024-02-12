/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.client.gui;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import github.kasuminova.mmce.client.gui.GuiScreenDynamic;
import github.kasuminova.mmce.client.gui.widget.base.DynamicWidget;
import github.kasuminova.mmce.client.gui.widget.base.WidgetController;
import github.kasuminova.mmce.client.gui.widget.base.WidgetGui;
import github.kasuminova.mmce.client.gui.widget.container.Column;
import github.kasuminova.mmce.client.gui.widget.preview.MachineStructurePreviewWidget;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.client.ClientProxy;
import hellfirepvp.modularmachinery.client.gui.widget.GuiScrollbar;
import hellfirepvp.modularmachinery.client.util.DynamicMachineRenderContext;
import hellfirepvp.modularmachinery.client.util.RenderingUtils;
import hellfirepvp.modularmachinery.common.base.Mods;
import hellfirepvp.modularmachinery.common.block.BlockController;
import hellfirepvp.modularmachinery.common.integration.ModIntegrationJEI;
import hellfirepvp.modularmachinery.common.lib.BlocksMM;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.modifier.MultiBlockModifierReplacement;
import hellfirepvp.modularmachinery.common.modifier.SingleBlockModifierReplacement;
import hellfirepvp.modularmachinery.common.util.BlockArray;
import hellfirepvp.modularmachinery.common.util.BlockCompatHelper;
import hellfirepvp.modularmachinery.common.util.IBlockStateDescriptor;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.config.KeyBindings;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec2f;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.IFluidBlock;
import net.minecraftforge.fml.client.config.GuiUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: GuiScreenBlueprint
 * Created by HellFirePvP
 * Date: 09.07.2017 / 21:08
 */
public class GuiScreenBlueprint extends GuiScreenDynamic {
    public static final ResourceLocation TEXTURE_BACKGROUND =
            new ResourceLocation(ModularMachinery.MODID, "textures/gui/guiblueprint_background.png");

    public static final ResourceLocation TEXTURE_OVERLAY =
            new ResourceLocation(ModularMachinery.MODID, "textures/gui/guiblueprint_new.png");
    public static final int X_SIZE = 184;
    public static final int Y_SIZE = 221;

    private static final ResourceLocation ic2TileBlock = new ResourceLocation("ic2", "te");

    private final DynamicMachine machine;

    private DynamicMachineRenderContext renderContext;
    private GuiScrollbar ingredientListScrollbar;
    private int frameCount = 0;

    public GuiScreenBlueprint(DynamicMachine machine) {
        this.machine = machine;
        this.renderContext = DynamicMachineRenderContext.createContext(this.machine);
    }

    public static void renderIngredientList(final GuiScreen g, final Minecraft mc, final RenderItem ri,
                                            final GuiScrollbar scrollbar, final DynamicMachineRenderContext dynamicContext,
                                            final int mouseX, final int mouseY, final int screenX, final int screenY) {
        List<ItemStack> ingredientList = dynamicContext.getPattern().getDescriptiveStackList(dynamicContext.getShiftSnap());
        scrollbar.setRange(0, Math.max(0, (ingredientList.size() - 8) / 8), 1);
        scrollbar.draw(g, mc);

        int indexOffset = scrollbar.getCurrentScroll() * 8;
        int x = screenX;
        int y = screenY;

        GlStateManager.color(1F, 1F, 1F, 1F);
        GlStateManager.pushMatrix();
        RenderHelper.enableGUIStandardItemLighting();

        ItemStack tooltipStack = null;

        for (int i = 0; i + indexOffset < Math.min(16 + indexOffset, ingredientList.size()); i++) {
            ItemStack stack = ingredientList.get(i + indexOffset);
            if (tooltipStack == null) {
                tooltipStack = renderItemStackToGUI(g, mc, ri, x, y, mouseX, mouseY, stack);
            } else {
                renderItemStackToGUI(g, mc, ri, x, y, mouseX, mouseY, stack);
            }
            x += 18;

            if (i == 7) {
                x = screenX;
                y += 18;
            }
        }

        if (tooltipStack != null) {
            GuiUtils.preItemToolTip(tooltipStack);
            g.drawHoveringText(g.getItemToolTip(tooltipStack), mouseX, mouseY);
            GuiUtils.postItemToolTip();
        }

        GlStateManager.disableLighting();
        GlStateManager.popMatrix();
    }

    public static ItemStack renderItemStackToGUI(final GuiScreen g, final Minecraft mc, final RenderItem ri,
                                                 final int x, final int y, final int mouseX, final int mouseY,
                                                 final ItemStack stack) {
        ri.renderItemAndEffectIntoGUI(stack, x, y);
        ri.renderItemOverlays(mc.fontRenderer, stack, x, y);

        if ((mouseX >= x && mouseX <= x + 16) && (mouseY >= y && mouseY <= y + 16)) {
            renderHoveredForeground(x, y);
            if (Mods.JEI.isPresent()) {
                handleJEIInput(stack);
            }
            return stack;
        }
        return null;
    }

    public static void handleJEIInput(final ItemStack stack) {
        int showRecipeKeyCode = KeyBindings.showRecipe.getKeyCode();
        int showUsesKeyCode = KeyBindings.showUses.getKeyCode();
        int bookmarkKeyCode = KeyBindings.bookmark.getKeyCode();

        if ((showRecipeKeyCode > 0 && showRecipeKeyCode <= 255 && Keyboard.isKeyDown(showRecipeKeyCode)) || Mouse.isButtonDown(0)) {
            ClientProxy.clientScheduler.addRunnable(() -> {
                IFocus<ItemStack> focus = ModIntegrationJEI.recipeRegistry.createFocus(IFocus.Mode.OUTPUT, stack);
                ModIntegrationJEI.jeiRuntime.getRecipesGui().show(focus);
            }, 0);
            return;
        }
        if ((showUsesKeyCode > 0 && showUsesKeyCode <= 255 && Keyboard.isKeyDown(showUsesKeyCode)) || Mouse.isButtonDown(1)) {
            ClientProxy.clientScheduler.addRunnable(() -> {
                IFocus<ItemStack> focus = ModIntegrationJEI.recipeRegistry.createFocus(IFocus.Mode.INPUT, stack);
                ModIntegrationJEI.jeiRuntime.getRecipesGui().show(focus);
            }, 0);
            return;
        }

        if (bookmarkKeyCode > 0 && bookmarkKeyCode <= 255 && Keyboard.isKeyDown(bookmarkKeyCode)) {
            ModIntegrationJEI.addItemStackToBookmarkList(stack);
        }
    }

    private static void renderHoveredForeground(final int x, final int y) {
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.colorMask(true, true, true, false);
        GuiScreen.drawRect(x, y, x + 16, y + 16, new Color(255, 255, 255, 150).getRGB());
        GlStateManager.colorMask(true, true, true, true);
        GlStateManager.enableLighting();
        GlStateManager.enableDepth();
    }

    @Override
    public void initGui() {
        this.guiLeft = (this.width - X_SIZE) / 2;
        this.guiTop = (this.height - Y_SIZE) / 2;
        this.ingredientListScrollbar = new GuiScrollbar().setLeft(guiLeft + 156).setTop(guiTop + 142).setHeight(34);

        this.widgetController = new WidgetController(new WidgetGui(this, X_SIZE, Y_SIZE));
        this.widgetController.init();
        Column column = (Column) new Column().setAbsX(6).setAbsY(27).setWidth(172).setHeight(150);
        this.widgetController.addWidgetContainer(column.addWidget(
                new MachineStructurePreviewWidget(machine).setWidth(172).setHeight(150)
        ));
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        int x = (this.width - X_SIZE) / 2;
        int z = (this.height - Y_SIZE) / 2;
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        this.mc.getTextureManager().bindTexture(TEXTURE_BACKGROUND);
        this.drawTexturedModalRect(x, z, 0, 0, X_SIZE, Y_SIZE);

        super.drawScreen(mouseX, mouseY, partialTicks);

        this.mc.getTextureManager().bindTexture(TEXTURE_OVERLAY);
        this.drawTexturedModalRect(x, z, 0, 0, X_SIZE, Y_SIZE);
//
//        if (renderContext.doesRenderIn3D()) {
//            if (Mouse.isButtonDown(0) && frameCount > 20) {
//                renderContext.rotateRender(0.25 * Mouse.getDY(), 0.25 * Mouse.getDX(), 0);
//            }
//        } else {
//            if (Mouse.isButtonDown(0) && frameCount > 20) {
//                renderContext.moveRender(0.25 * Mouse.getDX(), 0, -0.25 * Mouse.getDY());
//            }
//        }
//
//        handleDWheel(mouseX, mouseY);
//
//        if (GameSettings.isKeyDown(Minecraft.getMinecraft().gameSettings.keyBindSneak)) {
//            if (renderContext.getShiftSnap() == -1) {
//                renderContext.snapSamples();
//            }
//        } else {
//            renderContext.releaseSamples();
//        }
//
////        if (renderContext.getPattern().getPattern().size() >= 3500 && renderContext.doesRenderIn3D()) {
////            ScaledResolution res = new ScaledResolution(Minecraft.getMinecraft());
////            FontRenderer fr = fontRenderer;
////            final int[] y = {guiTop + 12};
////            Stream.of(
////                            I18n.format("gui.preview.error.too_large.tip.0"),
////                            I18n.format("gui.preview.error.too_large.tip.1"),
////                            I18n.format("gui.preview.error.too_large.tip.2"))
////                    .flatMap(str -> fr.listFormattedStringToWidth(str, 160 * res.getScaleFactor()).stream())
////                    .forEach(str -> {
////                        fr.drawStringWithShadow(str,  guiLeft + 10, y[0], 0xFFFFFF);
////                        y[0] += 12;
////                    });
////        } else {
//        ScaledResolution res = new ScaledResolution(mc);
//        Rectangle scissorFrame = new Rectangle((guiLeft + 8) * res.getScaleFactor(), (guiTop + 82) * res.getScaleFactor(),
//                160 * res.getScaleFactor(), 94 * res.getScaleFactor());
//        GL11.glEnable(GL11.GL_SCISSOR_TEST);
//        GL11.glScissor(scissorFrame.x, scissorFrame.y, scissorFrame.width, scissorFrame.height);
//        x = 88;
//        z = 62;
//        renderContext.renderAt(this.guiLeft + x, this.guiTop + z, partialTicks);
//        GL11.glDisable(GL11.GL_SCISSOR_TEST);
////        }
//
//        renderIngredientList(this, mc, mc.getRenderItem(), ingredientListScrollbar, renderContext, mouseX, mouseY, guiLeft + 8, guiTop + 142);
//        drawButtons(mouseX, mouseY);
//        renderUpgradeInfo(mouseX, mouseY);
//
//        fontRenderer.drawStringWithShadow(machine.getLocalizedName(), this.guiLeft + 10, this.guiTop + 11, 0xFFFFFFFF);
//        if (machine.isRequiresBlueprint()) {
//            String reqBlueprint = I18n.format("tooltip.machinery.blueprint.required");
//            fontRenderer.drawStringWithShadow(reqBlueprint, this.guiLeft + 10, this.guiTop + 106, 0xFFFFFF);
//        }
//
//        scissorFrame = new Rectangle(MathHelper.floor(this.guiLeft + 8), MathHelper.floor(this.guiTop + 8), 160, 94);
//        if (!renderContext.doesRenderIn3D() && scissorFrame.contains(mouseX, mouseY)) {
//            render2DHover(mouseX, mouseY, x, z);
//        }
    }

    private void handleDWheel(final int x, final int y) {
        int dWheel = Mouse.getDWheel();

        if (ingredientListScrollbar.isMouseOver(x, y)) {
            ingredientListScrollbar.wheel(dWheel);
            return;
        }

        if (dWheel < 0) {
            renderContext.zoomOut();
        } else if (dWheel > 0) {
            renderContext.zoomIn();
        }
    }

    private void renderUpgradeInfo(int mouseX, int mouseY) {
        if (!machine.getModifiers().isEmpty()) {
            this.mc.getTextureManager().bindTexture(TEXTURE_BACKGROUND);
            this.drawTexturedModalRect(guiLeft + 7, guiTop + 124, 0, 185, 80, 14);

            String reqBlueprint = I18n.format("tooltip.machinery.blueprint.upgrades");
            fontRenderer.drawStringWithShadow(reqBlueprint, this.guiLeft + 10, this.guiTop + 127, 0xFFFFFF);

            if (mouseX >= guiLeft + 7 && mouseX <= guiLeft + 87 &&
                    mouseY >= guiTop + 124 && mouseY <= guiTop + 138) {
                renderUpgradeTip(mouseX, mouseY);
            }
        }
    }

    private void renderUpgradeTip(int mouseX, int mouseY) {
        List<Tuple<ItemStack, String>> descriptionList = new LinkedList<>();
        boolean first = true;
        for (List<SingleBlockModifierReplacement> modifiers : machine.getModifiers().values()) {
            for (SingleBlockModifierReplacement mod : modifiers) {
                List<String> description = mod.getDescriptionLines();
                if (description.isEmpty()) {
                    continue;
                }
                if (!first) {
                    descriptionList.add(new Tuple<>(ItemStack.EMPTY, ""));
                }
                first = false;
                ItemStack stack = mod.getBlockInformation().getDescriptiveStack(renderContext.getShiftSnap());
                List<String> tooltip = stack.getTooltip(Minecraft.getMinecraft().player, Minecraft.getMinecraft().gameSettings.advancedItemTooltips ?
                        ITooltipFlag.TooltipFlags.ADVANCED : ITooltipFlag.TooltipFlags.NORMAL);
                descriptionList.add(new Tuple<>(
                        stack,
                        Iterables.getFirst(tooltip, "")));
                for (String str : description) {
                    descriptionList.add(new Tuple<>(ItemStack.EMPTY, str));
                }
            }
        }

        for (MultiBlockModifierReplacement multiBlockModifier : machine.getMultiBlockModifiers()) {
            BlockArray blockArray = multiBlockModifier.getBlockArray();
            Optional<BlockArray.BlockInformation> opt = blockArray.getPattern().values().stream().findFirst();
            if (opt.isPresent()) {
                ItemStack stack = opt.get().getDescriptiveStack(renderContext.getShiftSnap());
                List<String> tooltip = stack.getTooltip(Minecraft.getMinecraft().player, Minecraft.getMinecraft().gameSettings.advancedItemTooltips ?
                        ITooltipFlag.TooltipFlags.ADVANCED : ITooltipFlag.TooltipFlags.NORMAL);
                descriptionList.add(new Tuple<>(
                        stack,
                        Iterables.getFirst(tooltip, "")));
            }

            List<String> description = multiBlockModifier.getDescriptionLines();
            for (String str : description) {
                descriptionList.add(new Tuple<>(ItemStack.EMPTY, str));
            }
        }

        RenderingUtils.renderBlueStackTooltip(mouseX, mouseY, descriptionList, fontRenderer, Minecraft.getMinecraft().getRenderItem());
    }

    private void render2DHover(int mouseX, int mouseY, int x, int z) {
        double scale = renderContext.getScale();
        Vec2f offset = renderContext.getCurrentRenderOffset(guiLeft + x, guiTop + z);
        int jumpWidth = 14;
        double scaleJump = jumpWidth * scale;
        // TODO: Dynamic Pattern Slice.
        Map<BlockPos, BlockArray.BlockInformation> slice = machine.getPattern().getPatternSlice(renderContext.getRenderSlice());
        BlockController ctrl = BlockController.getControllerWithMachine(machine);
        if (ctrl == null) ctrl = BlocksMM.blockController;
        if (renderContext.getRenderSlice() == 0) {
            slice.put(BlockPos.ORIGIN, new BlockArray.BlockInformation(Lists.newArrayList(new IBlockStateDescriptor(ctrl.getDefaultState()))));
        }
        for (BlockPos pos : slice.keySet()) {
            int xMod = pos.getX() + 1 + this.renderContext.getMoveOffset().getX();
            int zMod = pos.getZ() + 1 + this.renderContext.getMoveOffset().getZ();
            Rectangle.Double rct = new Rectangle2D.Double(offset.x - xMod * scaleJump, offset.y - zMod * scaleJump, scaleJump, scaleJump);
            if (rct.contains(mouseX, mouseY)) {
                BlockArray.BlockInformation info = slice.get(pos);
                IBlockState state = info.getSampleState(renderContext.getShiftSnap());
                Tuple<IBlockState, TileEntity> recovered = BlockCompatHelper.transformState(state, info.previewTag == null ? info.matchingTag : info.previewTag,
                        new BlockArray.TileInstantiateContext(Minecraft.getMinecraft().world, pos));
                state = recovered.getFirst();
                Block type = state.getBlock();
                int meta = type.getMetaFromState(state);

                ItemStack stack = ItemStack.EMPTY;

                try {
                    if (ic2TileBlock.equals(type.getRegistryName())) {
                        stack = BlockCompatHelper.tryGetIC2MachineStack(state, recovered.getSecond());
                    } else {
                        stack = state.getBlock().getPickBlock(state, null, null, pos, null);
                    }
                } catch (Exception exc) {
                }

                if (stack.isEmpty()) {
                    if (type instanceof IFluidBlock) {
                        stack = FluidUtil.getFilledBucket(new FluidStack(((IFluidBlock) type).getFluid(), 1000));
                    } else if (type instanceof BlockLiquid) {
                        Material material = state.getMaterial();
                        if (material == Material.WATER) {
                            stack = new ItemStack(Items.WATER_BUCKET);
                        } else if (material == Material.LAVA) {
                            stack = new ItemStack(Items.LAVA_BUCKET);
                        } else {
                            stack = ItemStack.EMPTY;
                        }
                    } else {
                        Item item = Item.getItemFromBlock(type);
                        if (item == Items.AIR) continue;
                        if (item.getHasSubtypes()) {
                            stack = new ItemStack(item, 1, meta);
                        } else {
                            stack = new ItemStack(item);
                        }
                    }
                }

                renderToolTip(stack, mouseX, mouseY);
                break;
            }
        }
    }

    private void drawButtons(int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(TEXTURE_BACKGROUND);

        boolean drawPopoutInfo = false, drawContents = false, drawSmallerPatternTip = false, drawLargerPatternTip = false;

        //3D view
        int add = 0;
        if (!renderContext.doesRenderIn3D()) {
            if (mouseX >= this.guiLeft + 132 && mouseX <= this.guiLeft + 132 + 16 &&
                    mouseY >= this.guiTop + 106 && mouseY < this.guiTop + 106 + 16) {
                add = 16;
            }
        } else {
            add = 32;
        }
        this.drawTexturedModalRect(guiLeft + 132, guiTop + 106, 176 + add, 16, 16, 16);

        //Pop out
        add = 0;
        if (mouseX >= this.guiLeft + 116 && mouseX <= this.guiLeft + 116 + 16 &&
                mouseY >= this.guiTop + 106 && mouseY < this.guiTop + 106 + 16) {
            if (GameSettings.isKeyDown(Minecraft.getMinecraft().gameSettings.keyBindSneak)) {
                add = 16;
            }
            drawPopoutInfo = true;
        }
        this.drawTexturedModalRect(guiLeft + 116, guiTop + 106, 176 + add, 48, 16, 16);

        //2D view
        add = 0;
        if (renderContext.doesRenderIn3D()) {
            if (mouseX >= this.guiLeft + 132 && mouseX <= this.guiLeft + 132 + 16 &&
                    mouseY >= this.guiTop + 122 && mouseY <= this.guiTop + 122 + 16) {
                add = 16;
            }
        } else {
            add = 32;
        }
        this.drawTexturedModalRect(guiLeft + 132, guiTop + 122, 176 + add, 32, 16, 16);

        //Show amount
        add = 0;
        if (mouseX >= this.guiLeft + 116 && mouseX <= this.guiLeft + 116 + 16 &&
                mouseY >= this.guiTop + 122 && mouseY <= this.guiTop + 122 + 16) {
            add = 16;
            drawContents = true;
        }
        this.drawTexturedModalRect(guiLeft + 116, guiTop + 122, 176 + add, 64, 16, 16);

        if (!machine.getDynamicPatterns().isEmpty()) {
            // DynamicPattern Size Smaller
            add = 0;
            if (mouseX >= this.guiLeft + 100 && mouseX <= this.guiLeft + 100 + 16 &&
                    mouseY >= this.guiTop + 106 && mouseY <= this.guiTop + 106 + 16) {
                add = 16;
                drawSmallerPatternTip = true;
            }
            this.drawTexturedModalRect(guiLeft + 100, guiTop + 106, 176 + add, 80, 16, 16);

            // DynamicPattern Size Larger
            add = 0;
            if (mouseX >= this.guiLeft + 100 && mouseX <= this.guiLeft + 100 + 16 &&
                    mouseY >= this.guiTop + 122 && mouseY <= this.guiTop + 122 + 16) {
                add = 16;
                drawLargerPatternTip = true;
            }
            this.drawTexturedModalRect(guiLeft + 100, guiTop + 122, 176 + add, 96, 16, 16);
        }

        if (renderContext.doesRenderIn3D()) {
            GlStateManager.color(0.3F, 0.3F, 0.3F, 1.0F);
        } else {
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }

        if (renderContext.hasSliceUp()) {
            if (!renderContext.doesRenderIn3D() && mouseX >= this.guiLeft + 150 && mouseX <= this.guiLeft + 150 + 16 &&
                    mouseY >= this.guiTop + 102 && mouseY <= this.guiTop + 102 + 16) {
                GlStateManager.color(0.7F, 0.7F, 1.0F, 1.0F);
            }
            this.drawTexturedModalRect(guiLeft + 150, guiTop + 102, 192, 0, 16, 16);
            GlStateManager.color(1F, 1F, 1F, 1F);
        }
        if (renderContext.hasSliceDown()) {
            if (!renderContext.doesRenderIn3D() && mouseX >= this.guiLeft + 150 && mouseX <= this.guiLeft + 150 + 16 &&
                    mouseY >= this.guiTop + 124 && mouseY <= this.guiTop + 124 + 16) {
                GlStateManager.color(0.7F, 0.7F, 1.0F, 1.0F);
            }
            this.drawTexturedModalRect(guiLeft + 150, guiTop + 124, 176, 0, 16, 16);
            GlStateManager.color(1F, 1F, 1F, 1F);
        }
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        int width = fontRenderer.getStringWidth(String.valueOf(renderContext.getRenderSlice()));
        fontRenderer.drawStringWithShadow(String.valueOf(renderContext.getRenderSlice()), guiLeft + 159 - (width / 2), guiTop + 118, 0x222222);
        if (drawPopoutInfo) {
            drawHoveringText(I18n.format("gui.blueprint.popout.info"), mouseX, mouseY);
        }
        if (drawSmallerPatternTip) {
            drawHoveringText(I18n.format(
                    "gui.blueprint.smaller.info", renderContext.getDynamicPatternSize()), mouseX, mouseY);
        }
        if (drawLargerPatternTip) {
            drawHoveringText(I18n.format(
                    "gui.blueprint.larger.info", renderContext.getDynamicPatternSize()), mouseX, mouseY);
        }
        if (drawContents) {
            List<ItemStack> contents = this.renderContext.getDescriptiveStacks();
            List<Tuple<ItemStack, String>> contentMap = Lists.newArrayList();
            for (ItemStack stack : contents) {
                contentMap.add(new Tuple<>(stack, stack.getCount() + "x " + Iterables.getFirst(stack.getTooltip(Minecraft.getMinecraft().player,
                        Minecraft.getMinecraft().gameSettings.advancedItemTooltips ? ITooltipFlag.TooltipFlags.ADVANCED : ITooltipFlag.TooltipFlags.NORMAL), "")));
            }
            RenderingUtils.renderBlueStackTooltip(mouseX, mouseY,
                    contentMap,
                    fontRenderer, Minecraft.getMinecraft().getRenderItem());
        }
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableLighting();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

//        if (mouseButton == 0) {
//            if (!renderContext.doesRenderIn3D()) {
//                if (mouseX >= this.guiLeft + 132 && mouseX <= this.guiLeft + 132 + 16 &&
//                        mouseY >= this.guiTop + 106 && mouseY <= this.guiTop + 106 + 16) {
//                    renderContext.setTo3D();
//                }
//                if (renderContext.hasSliceUp() && mouseX >= this.guiLeft + 150 && mouseX <= this.guiLeft + 150 + 16 &&
//                        mouseY >= this.guiTop + 102 && mouseY <= this.guiTop + 102 + 16) {
//                    renderContext.sliceUp();
//                }
//                if (renderContext.hasSliceDown() && mouseX >= this.guiLeft + 150 && mouseX <= this.guiLeft + 150 + 16 &&
//                        mouseY >= this.guiTop + 124 && mouseY <= this.guiTop + 124 + 16) {
//                    renderContext.sliceDown();
//                }
//            } else {
//                if (mouseX >= this.guiLeft + 132 && mouseX <= this.guiLeft + 132 + 16 &&
//                        mouseY >= this.guiTop + 122 && mouseY <= this.guiTop + 122 + 16) {
//                    renderContext.setTo2D();
//                }
//
//                // DynamicPattern Size Smaller
//                if (mouseX >= this.guiLeft + 100 && mouseX <= this.guiLeft + 100 + 16 &&
//                        mouseY >= this.guiTop + 106 && mouseY <= this.guiTop + 106 + 16) {
//                    renderContext = DynamicMachineRenderContext.createContext(
//                            renderContext.getDisplayedMachine(), renderContext.getDynamicPatternSize() - 1);
//                }
//
//                // DynamicPattern Size Larger
//                if (mouseX >= this.guiLeft + 100 && mouseX <= this.guiLeft + 100 + 16 &&
//                        mouseY >= this.guiTop + 122 && mouseY <= this.guiTop + 122 + 16) {
//                    renderContext = DynamicMachineRenderContext.createContext(
//                            renderContext.getDisplayedMachine(), renderContext.getDynamicPatternSize() + 1);
//                }
//            }
//            if (GameSettings.isKeyDown(Minecraft.getMinecraft().gameSettings.keyBindSneak) &&
//                    mouseX >= this.guiLeft + 116 && mouseX <= this.guiLeft + 116 + 16 &&
//                    mouseY >= this.guiTop + 106 && mouseY < this.guiTop + 106 + 16) {
//                if (ClientProxy.renderHelper.startPreview(this.renderContext)) {
//                    Minecraft.getMinecraft().displayGuiScreen(null);
//                }
//            }
//        } else if (mouseButton == 1) {
//            Minecraft.getMinecraft().displayGuiScreen(null);
//        }
    }

}
