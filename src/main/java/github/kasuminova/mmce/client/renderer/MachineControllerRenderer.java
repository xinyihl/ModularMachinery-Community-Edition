package github.kasuminova.mmce.client.renderer;

import github.kasuminova.mmce.client.model.DynamicMachineModelRegistry;
import github.kasuminova.mmce.client.model.MachineControllerModel;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import software.bernie.geckolib3.core.util.Color;
import software.bernie.geckolib3.geo.render.built.GeoCube;
import software.bernie.geckolib3.geo.render.built.GeoModel;
import software.bernie.geckolib3.geo.render.built.GeoQuad;
import software.bernie.geckolib3.geo.render.built.GeoVertex;
import software.bernie.geckolib3.model.AnimatedGeoModel;
import software.bernie.geckolib3.renderers.geo.IGeoRenderer;

import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;

public class MachineControllerRenderer extends TileEntitySpecialRenderer<TileMultiblockMachineController> implements IGeoRenderer<TileMultiblockMachineController> {

    public static final MachineControllerRenderer INSTANCE = new MachineControllerRenderer();

    private MachineControllerRenderer() {
    }

    @Override
    public void render(TileMultiblockMachineController te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        this.render(te, x, y, z, partialTicks, destroyStage);
    }

    public void render(TileMultiblockMachineController tile, double x, double y, double z, float partialTicks, int destroyStage) {
        MachineControllerModel modelProvider = DynamicMachineModelRegistry.INSTANCE.getModel(tile.getFoundMachine());
        if (modelProvider == null) {
            return;
        }

        GeoModel model = modelProvider.getModel();
        modelProvider.setLivingAnimations(tile, this.getUniqueID(tile));

        int light = tile.getWorld().getCombinedLight(tile.getPos(), 0);
        int lx = light % 65536;
        int ly = light / 65536;

        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        OpenGlHelper.setLightmapTextureCoords(GL11.GL_TEXTURE_2D, lx, ly);
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.translate(0, 0.01f, 0);
        GlStateManager.translate(0.5, 0, 0.5);

        rotateBlock(getFacing(tile));

        Minecraft.getMinecraft().renderEngine.bindTexture(modelProvider.getTextureLocation());
        Color renderColor = getRenderColor(tile, partialTicks);
        render(model, tile, partialTicks, (float) renderColor.getRed() / 255f, (float) renderColor.getGreen() / 255f,
                (float) renderColor.getBlue() / 255f, (float) renderColor.getAlpha() / 255);
        GlStateManager.popMatrix();
    }

    @Override
    public void renderCube(final BufferBuilder builder, final GeoCube cube, final float red, final float green, final float blue, final float alpha) {
        MATRIX_STACK.moveToPivot(cube);
        MATRIX_STACK.rotate(cube);
        MATRIX_STACK.moveBackFromPivot(cube);

        for (GeoQuad quad : cube.quads) {
            // 你知道我为什么 NPE，来找我吧~
            if (quad == null) {
                continue;
            }

            Vector3f normal = new Vector3f(quad.normal.getX(), quad.normal.getY(), quad.normal.getZ());

            MATRIX_STACK.getNormalMatrix().transform(normal);

            if ((cube.size.y == 0 || cube.size.z == 0) && normal.getX() < 0) {
                normal.x *= -1;
            }
            if ((cube.size.x == 0 || cube.size.z == 0) && normal.getY() < 0) {
                normal.y *= -1;
            }
            if ((cube.size.x == 0 || cube.size.y == 0) && normal.getZ() < 0) {
                normal.z *= -1;
            }

            for (GeoVertex vertex : quad.vertices) {
                Vector4f vector4f = new Vector4f(vertex.position.getX(), vertex.position.getY(), vertex.position.getZ(),
                        1.0F);

                MATRIX_STACK.getModelMatrix().transform(vector4f);

                builder.pos(vector4f.getX(), vector4f.getY(), vector4f.getZ()).tex(vertex.textureU, vertex.textureV)
                        .color(red, green, blue, alpha).normal(normal.getX(), normal.getY(), normal.getZ()).endVertex();
            }
        }
    }

    @Override
    public AnimatedGeoModel<TileMultiblockMachineController> getGeoModelProvider() {
        return null;
    }

    protected void rotateBlock(EnumFacing facing) {
        switch (facing) {
            case SOUTH:
                GlStateManager.rotate(180, 0, 1, 0);
                break;
            case WEST:
                GlStateManager.rotate(90, 0, 1, 0);
                break;
            case NORTH:
                /* There is no need to rotate by 0 */
                break;
            case EAST:
                GlStateManager.rotate(270, 0, 1, 0);
                break;
            case UP:
                GlStateManager.rotate(90, 1, 0, 0);
                break;
            case DOWN:
                GlStateManager.rotate(90, -1, 0, 0);
                break;
        }
    }

    private EnumFacing getFacing(TileMultiblockMachineController tile) {
        return tile.getControllerRotation();
    }

    @Override
    public ResourceLocation getTextureLocation(TileMultiblockMachineController instance) {
        return null;
    }
}
