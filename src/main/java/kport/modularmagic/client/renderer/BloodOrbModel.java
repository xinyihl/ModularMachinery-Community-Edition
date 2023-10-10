package kport.modularmagic.client.renderer;// Made with Blockbench 4.8.3
// Exported for Minecraft version 1.7 - 1.12
// Paste this class into your mod and generate all required imports


import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.ModelRenderer;

public class BloodOrbModel extends ModelBase {
    private final ModelRenderer orb;

    public BloodOrbModel() {
        textureWidth = 32;
        textureHeight = 16;

        orb = new ModelRenderer(this);
        orb.setRotationPoint(0.0F, 24.0F, 0.0F);
        orb.cubeList.add(new ModelBox(orb, 0, 0, -4.0F, -12.0F, -4.0F, 8, 8, 8, 0.0F, false));
    }

    public void render() {
        orb.render(0.0625F);
    }

    public void setRotation(ModelRenderer modelRenderer, float x, float y, float z) {
        modelRenderer.rotateAngleX = x;
        modelRenderer.rotateAngleY = y;
        modelRenderer.rotateAngleZ = z;
    }
}