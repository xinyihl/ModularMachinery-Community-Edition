package github.kasuminova.mmce.common.event.client;

import com.github.bsideup.jabel.Desugar;
import crafttweaker.annotations.ZenRegister;
import github.kasuminova.mmce.common.event.machine.MachineEvent;
import github.kasuminova.mmce.common.helper.IMachineController;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.helper.IFunction;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import software.bernie.geckolib3.core.builder.Animation;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenGetter;
import stanhebben.zenscript.annotations.ZenMethod;
import stanhebben.zenscript.annotations.ZenSetter;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

@ZenRegister
@ZenClass("mods.modularmachinery.ControllerAnimationEvent")
public class ControllerAnimationEvent extends MachineEvent {
    private final List<AnimationCT> animations = new ArrayList<>();
    private final AnimationEvent<TileMultiblockMachineController> event;

    private int playState = 0;

    public ControllerAnimationEvent(final TileMultiblockMachineController controller, final AnimationEvent<TileMultiblockMachineController> event) {
        super(controller);
        this.event = event;
    }

    @ZenMethod
    public void addAnimation(String animationName) {
        animations.add(new AnimationCT(animationName, param -> true));
    }

    @ZenMethod
    public void addAnimation(String animationName, boolean loop) {
        animations.add(new AnimationCT(animationName, param -> loop));
    }

    @ZenMethod
    public void addAnimation(String animationName, IFunction<IMachineController, Boolean> loopFunction) {
        animations.add(new AnimationCT(animationName, loopFunction));
    }

    @ZenMethod
    public void setAnimation(String animationName) {
        animations.clear();
        addAnimation(animationName);
    }

    @ZenMethod
    public void setAnimation(String animationName, boolean loop) {
        animations.clear();
        addAnimation(animationName, loop);
    }

    @ZenMethod
    public void setAnimation(String animationName, IFunction<IMachineController, Boolean> loopFunction) {
        animations.clear();
        addAnimation(animationName, loopFunction);
    }

    @ZenGetter("transitionLengthTicks")
    public double getTransitionLengthTicks() {
        return event.getController().transitionLengthTicks;
    }

    @ZenSetter("transitionLengthTicks")
    public void setTransitionLengthTicks(double transitionLengthTicks) {
        event.getController().transitionLengthTicks = transitionLengthTicks;
    }

    @ZenGetter("playState")
    public int getPlayState() {
        return playState;
    }

    @ZenSetter("playState")
    public void setPlayState(final int playState) {
        this.playState = playState;
    }

    @Nullable
    @ZenGetter("currentAnimationName")
    public String getCurrentAnimationName() {
        Animation current = event.getController().getCurrentAnimation();
        return current == null ? null : current.animationName;
    }

    @ZenGetter("animationSpeed")
    public double getAnimationSpeed() {
        return event.getController().getAnimationSpeed();
    }

    @ZenGetter("animationTick")
    public double getAnimationTick() {
        return event.getAnimationTick();
    }

    @ZenGetter("animationState")
    public int getAnimationState() {
        return switch (event.getController().getAnimationState()) {
            case Running -> 0;
            case Transitioning -> 1;
            case Stopped -> 2;
        };
    }

    @ZenGetter("limbSwing")
    public float getLimbSwing() {
        return event.getLimbSwing();
    }

    @ZenGetter("limbSwingAmount")
    public float getLimbSwingAmount() {
        return event.getLimbSwingAmount();
    }

    @ZenGetter("partialTick")
    public float getPartialTick() {
        return event.getPartialTick();
    }

    @ZenGetter("moving")
    public boolean isMoving() {
        return event.isMoving();
    }

    public List<AnimationCT> getAnimations() {
        return animations;
    }

    @Desugar
    public record AnimationCT(String animationName, IFunction<IMachineController, Boolean> loopFunction) {
    }
}