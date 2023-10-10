package kport.modularmagic.client.renderer;

import hellfirepvp.modularmachinery.ModularMachinery;
import kport.modularmagic.common.tile.TileLifeEssenceProvider;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.lwjgl.opengl.GL11;

public class TileLifeEssentiaHatchRenderer extends TileEntitySpecialRenderer<TileLifeEssenceProvider> {

    private static final ResourceLocation TEX_ORB_1 = new ResourceLocation(ModularMachinery.MODID, "textures/blocks/orb1.png");
    private static final ResourceLocation TEX_ORB_2 = new ResourceLocation(ModularMachinery.MODID, "textures/blocks/orb2.png");
    private static final ResourceLocation TEX_ORB_3 = new ResourceLocation(ModularMachinery.MODID, "textures/blocks/orb3.png");
    private static final ResourceLocation TEX_ORB_4 = new ResourceLocation(ModularMachinery.MODID, "textures/blocks/orb4.png");
    private static final ResourceLocation TEX_ORB_5 = new ResourceLocation(ModularMachinery.MODID, "textures/blocks/orb5.png");
    private static final ResourceLocation TEX_ORB_6 = new ResourceLocation(ModularMachinery.MODID, "textures/blocks/orb6.png");
    private static final ResourceLocation TEX_ORB_7 = new ResourceLocation(ModularMachinery.MODID, "textures/blocks/orb7.png");
    private static final ResourceLocation TEX_ORB_8 = new ResourceLocation(ModularMachinery.MODID, "textures/blocks/orb8.png");
    public BloodOrbModel model;

    public TileLifeEssentiaHatchRenderer() {
        model = new BloodOrbModel();
    }

    public void renderTileEntityAt(TileLifeEssenceProvider tile, double x, double y, double z) {
        GL11.glPushMatrix();
        GL11.glTranslatef((float) x + 0.5f, (float) y - 0.5f, (float) z + 0.5f);

        ItemStack itemStack = tile.getInventory().getStackInSlot(0);
        NBTTagCompound stackTag = itemStack.getTagCompound();

        if (stackTag != null && stackTag.getString("orb").contains("bloodmagic:weak")) {
            GL11.glPushMatrix();
            bindTexture(TileLifeEssentiaHatchRenderer.TEX_ORB_1);
            model.render();
            GL11.glPopMatrix();
        } else if (stackTag != null && stackTag.getString("orb").contains("bloodmagic:apprentice")) {
            GL11.glPushMatrix();
            bindTexture(TileLifeEssentiaHatchRenderer.TEX_ORB_2);
            model.render();
            GL11.glPopMatrix();
        } else if (stackTag != null && stackTag.getString("orb").contains("bloodmagic:magician")) {
            GL11.glPushMatrix();
            bindTexture(TileLifeEssentiaHatchRenderer.TEX_ORB_3);
            model.render();
            GL11.glPopMatrix();
        } else if (stackTag != null && stackTag.getString("orb").contains("bloodmagic:master")) {
            GL11.glPushMatrix();
            bindTexture(TileLifeEssentiaHatchRenderer.TEX_ORB_4);
            model.render();
            GL11.glPopMatrix();
        } else if (stackTag != null && stackTag.getString("orb").contains("bloodmagic:archmage")) {
            GL11.glPushMatrix();
            bindTexture(TileLifeEssentiaHatchRenderer.TEX_ORB_5);
            model.render();
            GL11.glPopMatrix();
        } else if (stackTag != null && stackTag.getString("orb").contains("bloodmagic:transcendent")) {
            GL11.glPushMatrix();
            bindTexture(TileLifeEssentiaHatchRenderer.TEX_ORB_6);
            model.render();
            GL11.glPopMatrix();
        } else if (itemStack.getItem() == ForgeRegistries.ITEMS.getValue(new ResourceLocation("forbiddenmagicre", "eldritch_orb"))) {
            GL11.glPushMatrix();
            bindTexture(TileLifeEssentiaHatchRenderer.TEX_ORB_7);
            model.render();
            GL11.glPopMatrix();
        } else if (itemStack.getItem() == ForgeRegistries.ITEMS.getValue(new ResourceLocation("avaritia", "armok_orb"))) {
            GL11.glPushMatrix();
            bindTexture(TileLifeEssentiaHatchRenderer.TEX_ORB_8);
            model.render();
            GL11.glPopMatrix();
        }
        GL11.glPopMatrix();
    }

    @Override
    public void render(TileLifeEssenceProvider te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        super.render(te, x, y, z, partialTicks, destroyStage, alpha);
        renderTileEntityAt(te, x, y, z);
    }
}
