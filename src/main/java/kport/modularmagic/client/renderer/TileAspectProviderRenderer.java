package kport.modularmagic.client.renderer;

import kport.modularmagic.common.tile.TileAspectProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Optional;
import org.lwjgl.opengl.GL11;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.client.lib.RenderCubes;
import thaumcraft.client.lib.UtilsFX;
import thaumcraft.common.config.ModConfig;
import thaumcraft.common.tiles.essentia.TileJarFillable;

import javax.annotation.Nonnull;
import java.awt.*;

public class TileAspectProviderRenderer extends TileEntitySpecialRenderer<TileAspectProvider> {

    private static final ResourceLocation TEX_LABEL = new ResourceLocation("thaumcraft", "textures/models/label.png");

    @Override
    @Optional.Method(modid = "thaumcraft")
    public void render(@Nonnull TileAspectProvider tile, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        super.render(tile, x, y, z, partialTicks, destroyStage, alpha);
        GlStateManager.pushMatrix();
        GlStateManager.disableCull();
        GlStateManager.translate((float) x + 0.5F, (float) y + 0.01F, (float) z + 0.5F);
        GlStateManager.rotate(180.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableLighting();

        if (tile.aspectFilter != null) {
            GlStateManager.pushMatrix();
            GlStateManager.blendFunc(770, 771);
            switch (tile.facing) {
                case 3 -> GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
                case 4 -> GlStateManager.rotate(270.0F, 0.0F, 1.0F, 0.0F);
                case 5 -> GlStateManager.rotate(90.0F, 0.0F, 1.0F, 0.0F);
            }

            float rot = (float) ((tile.aspectFilter.getTag().hashCode() + tile.getPos().getX() + tile.facing) % 4 - 2);
            GlStateManager.pushMatrix();
            GlStateManager.translate(0.0F, -0.5F, 0.501F);
            if (ModConfig.CONFIG_GRAPHICS.crooked) {
                GlStateManager.rotate(rot, 0.0F, 0.0F, 1.0F);
            }

            UtilsFX.renderQuadCentered(TEX_LABEL, 0.5F, 1.0F, 1.0F, 1.0F, -99, 771, 1.0F);
            GlStateManager.popMatrix();
            GlStateManager.pushMatrix();
            GlStateManager.translate(0.0F, -0.5F, 0.502F);
            if (ModConfig.CONFIG_GRAPHICS.crooked) {
                GlStateManager.rotate(rot, 0.0F, 0.0F, 1.0F);
            }

            GlStateManager.scale(0.021D, 0.021D, 0.021D);
            GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
            UtilsFX.drawTag(-8, -8, tile.aspectFilter);
            GlStateManager.popMatrix();
            GlStateManager.popMatrix();
        }
        if (tile.amount > 0) {
            renderLiquid(tile, x, y, z, partialTicks);
        }
        GlStateManager.enableLighting();
        GlStateManager.enableCull();
        GlStateManager.popMatrix();
    }

    public void renderLiquid(TileJarFillable te, double x, double y, double z, float f) {
        GL11.glPushMatrix();
        GL11.glRotatef(180.0f, 1.0f, 0.0f, 0.0f);
        World world = te.getWorld();
        RenderCubes renderBlocks = new RenderCubes();
        GL11.glDisable(2896);
        float level = te.amount / 250.0f * 0.35f;
        Tessellator t = Tessellator.getInstance();
        renderBlocks.setRenderBounds(0.25, 0.375, 0.25, 0.75, 0.375 + level, 0.75);
        t.getBuffer().begin(7, DefaultVertexFormats.POSITION_TEX_LMAP_COLOR);
        Color co = new Color(0);
        if (te.aspect != null) {
            co = new Color(te.aspect.getColor());
        }
        TextureAtlasSprite icon = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite("thaumcraft:blocks/animatedglow");
        bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        renderBlocks.renderFaceYNeg(BlocksTC.jarNormal, -0.5, 0.0, -0.5, icon, co.getRed() / 255.0f, co.getGreen() / 255.0f, co.getBlue() / 255.0f, 200);
        renderBlocks.renderFaceYPos(BlocksTC.jarNormal, -0.5, 0.0, -0.5, icon, co.getRed() / 255.0f, co.getGreen() / 255.0f, co.getBlue() / 255.0f, 200);
        renderBlocks.renderFaceZNeg(BlocksTC.jarNormal, -0.5, 0.0, -0.5, icon, co.getRed() / 255.0f, co.getGreen() / 255.0f, co.getBlue() / 255.0f, 200);
        renderBlocks.renderFaceZPos(BlocksTC.jarNormal, -0.5, 0.0, -0.5, icon, co.getRed() / 255.0f, co.getGreen() / 255.0f, co.getBlue() / 255.0f, 200);
        renderBlocks.renderFaceXNeg(BlocksTC.jarNormal, -0.5, 0.0, -0.5, icon, co.getRed() / 255.0f, co.getGreen() / 255.0f, co.getBlue() / 255.0f, 200);
        renderBlocks.renderFaceXPos(BlocksTC.jarNormal, -0.5, 0.0, -0.5, icon, co.getRed() / 255.0f, co.getGreen() / 255.0f, co.getBlue() / 255.0f, 200);
        t.draw();
        GL11.glEnable(2896);
        GL11.glPopMatrix();
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
    }
}
