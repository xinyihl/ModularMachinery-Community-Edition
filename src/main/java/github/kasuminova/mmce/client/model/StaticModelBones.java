package github.kasuminova.mmce.client.model;

import com.google.common.collect.ImmutableSet;
import software.bernie.geckolib3.file.AnimationFile;
import software.bernie.geckolib3.geo.render.built.GeoBone;
import software.bernie.geckolib3.geo.render.built.GeoModel;

import java.util.HashSet;
import java.util.Set;

public class StaticModelBones {

    private final GeoModel model;
    private final AnimationFile animationFile;
    private Set<String> staticBones = new HashSet<>();;

    private StaticModelBones(final GeoModel model, final AnimationFile animationFile) {
        this.model = model;
        this.animationFile = animationFile;
        compile();
    }

    public static StaticModelBones compile(final GeoModel model, final AnimationFile animation) {
        return new StaticModelBones(model, animation);
    }

    public void compile() {
        model.topLevelBones.forEach(this::recursiveAdd);

        animationFile.getAllAnimations().stream()
                .flatMap(animation -> animation.boneAnimations.stream())
                .map(boneAnimation -> boneAnimation.boneName)
                .forEach(boneName -> staticBones.remove(boneName));

//        model.topLevelBones.stream()
//                .filter(bone -> isStaticBone(bone.name))
//                .filter(this::childBoneHasNonStatic)
//                .forEach(this::recursiveRemove);

        staticBones = ImmutableSet.copyOf(staticBones);
    }

    private void recursiveAdd(final GeoBone bone) {
        staticBones.add(bone.name);
        bone.childBones.stream()
                .filter(childBone -> !childBone.childBones.isEmpty())
                .forEach(this::recursiveAdd);
    }

    private boolean childBoneHasNonStatic(final GeoBone bone) {
        if (!staticBones.contains(bone.name)) {
            return true;
        }
        return bone.childBones.stream()
                .anyMatch(this::childBoneHasNonStatic);
    }

    private void recursiveRemove(final GeoBone bone) {
        staticBones.remove(bone.name);
        bone.childBones.stream()
                .filter(childBone -> !childBone.childBones.isEmpty())
                .forEach(this::recursiveRemove);
    }

    public boolean isStaticBone(final String boneName) {
        return staticBones.contains(boneName);
    }

}
