package github.kasuminova.mmce.client.model;

import github.kasuminova.mmce.client.resource.GeoModelExternalLoader;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.builder.Animation;
import software.bernie.geckolib3.geo.render.built.GeoBone;
import software.bernie.geckolib3.geo.render.built.GeoModel;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class MachineControllerModel extends AnimatedGeoModel<TileMultiblockMachineController> {
    protected final ResourceLocation modelLocation;
    protected final ResourceLocation textureLocation;
    protected final ResourceLocation animationFileLocation;

    protected GeoModel currentModel;

    public MachineControllerModel(final ResourceLocation modelLocation, final ResourceLocation textureLocation, final ResourceLocation animationFileLocation) {
        this.modelLocation = modelLocation;
        this.textureLocation = textureLocation;
        this.animationFileLocation = animationFileLocation;
    }

    public GeoModel getModel() {
        return getModel(modelLocation);
    }

    @Override
    public Animation getAnimation(final String name, final IAnimatable animatable) {
        return GeoModelExternalLoader.INSTANCE.getAnimation(animationFileLocation).getAnimation(name);
    }

    @Override
    public GeoModel getModel(final ResourceLocation location) {
        GeoModel model = GeoModelExternalLoader.INSTANCE.getModel(modelLocation);

        if (model != currentModel) {
            this.getAnimationProcessor().clearModelRendererList();
            for (GeoBone bone : model.topLevelBones) {
                registerBone(bone);
            }
            this.currentModel = model;
        }

        return model;
    }

    public ResourceLocation getModelLocation() {
        return modelLocation;
    }

    public ResourceLocation getTextureLocation() {
        return textureLocation;
    }

    public ResourceLocation getAnimationFileLocation() {
        return animationFileLocation;
    }

    @Override
    public ResourceLocation getModelLocation(final TileMultiblockMachineController object) {
        return modelLocation;
    }

    @Override
    public ResourceLocation getTextureLocation(final TileMultiblockMachineController object) {
        return textureLocation;
    }

    @Override
    public ResourceLocation getAnimationFileLocation(final TileMultiblockMachineController animatable) {
        return animationFileLocation;
    }
}
