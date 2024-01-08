package github.kasuminova.mmce.client.renderer;

import github.kasuminova.mmce.client.model.MachineControllerModel;
import github.kasuminova.mmce.client.util.MatrixStack;
import hellfirepvp.modularmachinery.common.base.Mods;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.common.Optional;
import org.lwjgl.opengl.GL11;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.IAnimatableModel;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.util.Color;
import software.bernie.geckolib3.geo.render.built.*;

import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;

@SuppressWarnings({"unchecked", "rawtypes"})
@Optional.Interface(iface = "software.bernie.geckolib3.renderers.geo.IGeoRenderer", modid = "geckolib3")
public class MachineControllerRenderer extends TileEntitySpecialRenderer<TileMultiblockMachineController> {

    public static final MachineControllerRenderer INSTANCE = new MachineControllerRenderer();

    private static final MatrixStack MATRIX_STACK = new MatrixStack();

    static {
        if (Mods.GECKOLIB.isPresent()) {
            AnimationController.addModelFetcher((IAnimatable object) -> {
                if (object instanceof TileMultiblockMachineController ctrl) {
                    MachineControllerModel currentModel = ctrl.getCurrentModel();
                    if (currentModel != null) {
                        return (IAnimatableModel) currentModel;
                    }
                }
                return null;
            });
        }
    }

    private MachineControllerRenderer() {
    }

    protected static void rotateBlock(EnumFacing facing) {
        switch (facing) {
            case SOUTH -> GlStateManager.rotate(180, 0, 1, 0);
            case WEST -> GlStateManager.rotate(90, 0, 1, 0);
            /* There is no need to rotate by 0 */
            case NORTH -> {
            }
            case EAST -> GlStateManager.rotate(270, 0, 1, 0);
            case UP -> GlStateManager.rotate(90, 1, 0, 0);
            case DOWN -> GlStateManager.rotate(90, -1, 0, 0);
        }
    }

    private static EnumFacing getFacing(TileMultiblockMachineController tile) {
        return tile.getControllerRotation();
    }

    public static Color getRenderColor(TileMultiblockMachineController animatable, float partialTicks) {
        return Color.ofRGBA(255, 255, 255, 255);
    }

    public static int getUniqueID(TileMultiblockMachineController animatable) {
        return animatable.hashCode();
    }

    @Override
    public void render(TileMultiblockMachineController te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        this.render(te, x, y, z, partialTicks, destroyStage);
    }

    @Optional.Method(modid = "geckolib3")
    public void render(TileMultiblockMachineController tile, double x, double y, double z, float partialTicks, int destroyStage) {
        MachineControllerModel modelProvider = tile.getCurrentModel();
        if (modelProvider == null) {
            return;
        }

        GeoModel model = modelProvider.getModel();
        modelProvider.setLivingAnimations(tile, MachineControllerRenderer.getUniqueID(tile));

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

    @Optional.Method(modid = "geckolib3")
    public void render(GeoModel model, TileMultiblockMachineController animatable, float partialTicks, float red, float green, float blue,
                       float alpha) {
        GlStateManager.disableCull();
        GlStateManager.enableRescaleNormal();

        BufferBuilder builder = Tessellator.getInstance().getBuffer();

        builder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR_NORMAL);

        // Render all top level bones
        for (GeoBone group : model.topLevelBones) {
            renderRecursively(builder, group, red, green, blue, alpha);
        }

        Tessellator.getInstance().draw();

        GlStateManager.disableRescaleNormal();
        GlStateManager.enableCull();
    }

    @Optional.Method(modid = "geckolib3")
    public void renderRecursively(BufferBuilder builder, GeoBone bone, float red, float green, float blue, float alpha) {
        boolean emissive = bone.name.equals("emissive");
        float lastBrightnessX = 0;
        float lastBrightnessY = 0;
        if (emissive) {
            Tessellator.getInstance().draw();
            lastBrightnessX = OpenGlHelper.lastBrightnessX;
            lastBrightnessY = OpenGlHelper.lastBrightnessY;
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);
            builder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR_NORMAL);
        }

        MATRIX_STACK.push();

        MATRIX_STACK.translate(bone);
        MATRIX_STACK.moveToPivot(bone);
        MATRIX_STACK.rotate(bone);
        MATRIX_STACK.scale(bone);
        MATRIX_STACK.moveBackFromPivot(bone);

        if (!bone.isHidden()) {
            for (GeoCube cube : bone.childCubes) {
                MATRIX_STACK.push();
                renderCube(builder, cube, red, green, blue, alpha);
                MATRIX_STACK.pop();
            }
        }
        if (!bone.childBonesAreHiddenToo()) {
            for (GeoBone childBone : bone.childBones) {
                renderRecursively(builder, childBone, red, green, blue, alpha);
            }
        }

        MATRIX_STACK.pop();

        if (emissive) {
            Tessellator.getInstance().draw();
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lastBrightnessX, lastBrightnessY);
            builder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR_NORMAL);
        }
    }

    @Optional.Method(modid = "geckolib3")
    public void renderCube(final BufferBuilder builder, final GeoCube cube, final float red, final float green, final float blue, final float alpha) {
        MATRIX_STACK.moveToPivot(cube);
        MATRIX_STACK.rotate(cube);
        MATRIX_STACK.moveBackFromPivot(cube);

        for (GeoQuad quad : cube.quads) {
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
}
