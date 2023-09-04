/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.crafting.helper;

import hellfirepvp.modularmachinery.common.crafting.requirement.type.RequirementType;
import hellfirepvp.modularmachinery.common.integration.recipe.RecipeLayoutPart;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import hellfirepvp.modularmachinery.common.util.ResultChance;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.Collections;
import java.util.List;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: ComponentRequirement
 * Created by HellFirePvP
 * Date: 28.06.2017 / 10:34
 */
public abstract class ComponentRequirement<T, V extends RequirementType<T, ? extends ComponentRequirement<T, V>>> {

    public static final int PRIORITY_WEIGHT_ENERGY = 50_000_000;
    public static final int PRIORITY_WEIGHT_FLUID = 100;
    public static final int PRIORITY_WEIGHT_ITEM = 50_000;

    public final V requirementType;
    protected final IOType actionType;
    protected ComponentSelectorTag tag = null;

    protected boolean triggered = false;
    protected boolean triggerRepeatable = false;
    protected int triggerTime = 0;

    protected boolean ignoreOutputCheck = false;

    public ComponentRequirement(V requirementType, IOType actionType) {
        this.requirementType = requirementType;
        this.actionType = actionType;
    }

    public final V getRequirementType() {
        return requirementType;
    }

    public final IOType getActionType() {
        return actionType;
    }

    public final ComponentSelectorTag getTag() {
        return tag;
    }

    public final void setTag(ComponentSelectorTag tag) {
        this.tag = tag;
    }

    public void setTriggerTime(int tickTime) {
        triggerTime = tickTime;
    }

    public int getTriggerTime() {
        return triggerTime;
    }

    public void setTriggerRepeatable(boolean triggerRepeatable) {
        this.triggerRepeatable = triggerRepeatable;
    }

    public boolean isTriggerRepeatable() {
        return triggerRepeatable;
    }

    public boolean isTriggered() {
        return triggered;
    }

    public void setTriggered(final boolean triggered) {
        this.triggered = triggered;
    }

    public int getSortingWeight() {
        return 0;
    }

    /**
     * Return true here to indicate the passed {@link ProcessingComponent} is valid for the methods:
     * - {@link #startCrafting(ProcessingComponent, RecipeCraftingContext, ResultChance)}
     * - {@link #finishCrafting(ProcessingComponent, RecipeCraftingContext, ResultChance)}
     * - {@link #canStartCrafting(ProcessingComponent, RecipeCraftingContext, List)}
     * <p>
     * and for {@link PerTick} instances:
     * - {@link PerTick#doIOTick(ProcessingComponent, RecipeCraftingContext)}
     *
     * @param component The component to test
     * @param ctx       The context to test in
     * @return true, if the component is valid for further processing by the specified methods, false otherwise
     */
    public abstract boolean isValidComponent(ProcessingComponent<?> component, RecipeCraftingContext ctx);

    //True, if the requirement could be fulfilled by the given component
    public abstract boolean startCrafting(ProcessingComponent<?> component, RecipeCraftingContext context, ResultChance chance);

    public void startCrafting(List<ProcessingComponent<?>> components, RecipeCraftingContext context, ResultChance chance) {

    }

    @Nonnull
    public abstract CraftCheck finishCrafting(ProcessingComponent<?> component, RecipeCraftingContext context, ResultChance chance);

    public CraftCheck finishCrafting(List<ProcessingComponent<?>> components, RecipeCraftingContext context, ResultChance chance) {
        return CraftCheck.skipComponent();
    }

    @Nonnull
    public abstract CraftCheck canStartCrafting(ProcessingComponent<?> component, RecipeCraftingContext context, List<ComponentOutputRestrictor> restrictions);

    public CraftCheck canStartCrafting(List<ProcessingComponent<?>> components, RecipeCraftingContext context) {
        return CraftCheck.skipComponent();
    }

    //Creates an exact copy of the current requirement
    public abstract ComponentRequirement<T, V> deepCopy();

    //Creates a copy of the current requirement and applies all modifiers to the requirement.
    //Supplying an empty list should behave identical to deepCopy
    public abstract ComponentRequirement<T, V> deepCopyModified(List<RecipeModifier> modifiers);

    public abstract void startRequirementCheck(ResultChance contextChance, RecipeCraftingContext context);

    public abstract void endRequirementCheck();

    //Previously in ComponentType.getMissingComponentErrorMessage
    //Should return an unlocalized error message to display if no component for the given io-type was found
    //i.e. a recipe has an item output, but there's no item output bus on the machine at all.
    //Overwrite this if necessary at all
    @Nonnull
    public abstract String getMissingComponentErrorMessage(IOType ioType);

    //Be sure, that if you specify a new object here as type that you register that along with a helper and renderer
    //in the JEI Integration! Otherwise JEI will complain about not having proper handling for this
    //Also, be sure that this generic T is the *only one* with that type otherwise internally stuff might break...
    public abstract JEIComponent<T> provideJEIComponent();

    public List<ProcessingComponent<?>> copyComponents(final List<ProcessingComponent<?>> components) {
        return Collections.emptyList();
    }

    public boolean isIgnoreOutputCheck() {
        return ignoreOutputCheck;
    }

    public void setIgnoreOutputCheck(final boolean ignoreOutputCheck) {
        this.ignoreOutputCheck = ignoreOutputCheck;
    }

    public interface ChancedRequirement {

        void setChance(float chance);

    }

    /**
     * <p>Parallelizable（可并行）</p>
     * <p>这个接口由 {@link ComponentRequirement} 及其子类实现，用于实现内置的并行功能。</p>
     */
    public interface Parallelizable {
        /**
         * 获取当前需求的最大可并行数量。
         *
         * @param context Context
         * @return 最大可并行数量
         */
        int getMaxParallelism(final List<ProcessingComponent<?>> components,
                              final RecipeCraftingContext context,
                              final int maxParallelism);

        /**
         * 设置需求并行数。
         *
         * @param parallelism 并行数
         */
        void setParallelism(int parallelism);

        /**
         * 设置该并行类型是否不受并行数影响（即并行数始终为 1）。
         * @param unaffected 是否不受影响
         */
        void setParallelizeUnaffected(boolean unaffected);
    }

    /**
     * <p>实现此接口的 ComponentRequirement 应当重写以下方法：</p>
     *
     * <p>{@link #startCrafting(List, RecipeCraftingContext, ResultChance)}</p>
     * <p>{@link #finishCrafting(List, RecipeCraftingContext, ResultChance)}</p>
     * <p>{@link #canStartCrafting(List, RecipeCraftingContext)}</p>
     * <p>{@link #copyComponents(List)}</p>
     */
    public interface MultiComponent {

    }

    public abstract static class JEIComponent<T> {

        public abstract Class<T> getJEIRequirementClass();

        public abstract List<T> getJEIIORequirements();

        @SideOnly(Side.CLIENT)
        public abstract RecipeLayoutPart<T> getLayoutPart(Point offset);

        @SideOnly(Side.CLIENT)
        public RecipeLayoutPart<T> getTemplateLayout() {
            return this.getLayoutPart(new Point(0, 0));
        }

        @SideOnly(Side.CLIENT)
        public abstract void onJEIHoverTooltip(int slotIndex, boolean input, T ingredient, List<String> tooltip);

    }

    public abstract static class PerTick<T, V extends RequirementType<T, ? extends PerTick<T, V>>> extends ComponentRequirement<T, V> {

        public PerTick(V requirementType, IOType actionType) {
            super(requirementType, actionType);
        }

        //Multiplier is passed into this to adjust 'production' or 'consumption' accordingly if the recipe has a longer or shorter duration
        public abstract void startIOTick(RecipeCraftingContext context, float durationMultiplier);

        // Returns the actual result of the IOTick-check after a sufficient amount of components have been checked for the requirement
        // Supply a failure message if invalid!
        @Nonnull
        public abstract CraftCheck resetIOTick(RecipeCraftingContext context);

        // Returns either success, partial success or skip component
        // Return value indicates whether the IO tick requirement was already successful
        // or if more components need to be checked.
        @Nonnull
        public abstract CraftCheck doIOTick(ProcessingComponent<?> component, RecipeCraftingContext context);

        public CraftCheck doIOTick(List<ProcessingComponent<?>> components, RecipeCraftingContext context, float durationMultiplier) {
            return null;
        }
    }
}
