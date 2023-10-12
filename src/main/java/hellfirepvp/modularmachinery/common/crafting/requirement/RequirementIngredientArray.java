package hellfirepvp.modularmachinery.common.crafting.requirement;

import github.kasuminova.mmce.common.helper.AdvancedItemChecker;
import github.kasuminova.mmce.common.itemtype.ChancedIngredientStack;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.crafting.helper.CraftCheck;
import hellfirepvp.modularmachinery.common.crafting.helper.ProcessingComponent;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.crafting.requirement.jei.JEIComponentIngredientArray;
import hellfirepvp.modularmachinery.common.crafting.requirement.type.RequirementTypeIngredientArray;
import hellfirepvp.modularmachinery.common.lib.ComponentTypesMM;
import hellfirepvp.modularmachinery.common.lib.RequirementTypesMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import hellfirepvp.modularmachinery.common.util.IItemHandlerImpl;
import hellfirepvp.modularmachinery.common.util.ItemUtils;
import hellfirepvp.modularmachinery.common.util.ResultChance;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RequirementIngredientArray extends ComponentRequirement.MultiComponentRequirement<ItemStack, RequirementTypeIngredientArray>
        implements ComponentRequirement.ChancedRequirement, ComponentRequirement.Parallelizable {

    public final List<ChancedIngredientStack> ingredients;

    public float chance = 1.0F;

    protected int parallelism = 1;
    protected boolean parallelizeUnaffected = false;

    /**
     * <p>物品组输入，仅消耗组内的其中一个</p>
     * <p>**仅限输入**</p>
     */
    public RequirementIngredientArray(List<ChancedIngredientStack> ingredients) {
        super(RequirementTypesMM.REQUIREMENT_INGREDIENT_ARRAY, IOType.INPUT);

        this.ingredients = ingredients;
    }

    @Override
    public boolean isValidComponent(ProcessingComponent<?> component, RecipeCraftingContext ctx) {
        MachineComponent<?> cmp = component.component();
        return cmp.getComponentType().equals(ComponentTypesMM.COMPONENT_ITEM) &&
                cmp instanceof MachineComponent.ItemBus &&
                cmp.ioType == actionType;
    }


    @Override
    public RequirementIngredientArray deepCopy() {
        return deepCopyModified(Collections.emptyList());
    }

    @Override
    public RequirementIngredientArray deepCopyModified(List<RecipeModifier> modifiers) {
        ArrayList<ChancedIngredientStack> newArray = new ArrayList<>(this.ingredients);
        newArray.forEach(item -> {
            switch (item.ingredientType) {
                case ITEMSTACK -> {
                    ItemStack itemStack = item.itemStack;
                    int amt = Math.round(RecipeModifier.applyModifiers(modifiers, this, itemStack.getCount(), false));
                    itemStack.setCount(amt);
                }
                case ORE_DICT -> item.count = Math.round(RecipeModifier.applyModifiers(modifiers, this, item.count, false));
            }
            item.chance = RecipeModifier.applyModifiers(modifiers, this, item.chance, true);
        });
        return new RequirementIngredientArray(newArray);
    }

    @Nonnull
    @Override
    public String getMissingComponentErrorMessage(IOType ioType) {
        ResourceLocation compKey = RequirementTypesMM.KEY_REQUIREMENT_ITEM;
        return String.format("component.missing.%s.%s.%s",
                compKey.getNamespace(), compKey.getPath(), ioType.name().toLowerCase());
    }

    @Override
    public JEIComponent<ItemStack> provideJEIComponent() {
        return new JEIComponentIngredientArray(this);
    }

    @Override
    public void setChance(float chance) {
        this.chance = chance;
    }

    @Override
    public void startCrafting(List<ProcessingComponent<?>> components, RecipeCraftingContext context, ResultChance chance) {
        doItemIO(components, context, chance);
    }

    @Nonnull
    @Override
    public CraftCheck canStartCrafting(final List<ProcessingComponent<?>> components, final RecipeCraftingContext context) {
        return doItemIO(components, context, ResultChance.GUARANTEED);
    }

    @Override
    public int getMaxParallelism(List<ProcessingComponent<?>> component, RecipeCraftingContext context, int maxParallelism) {
        if (parallelizeUnaffected || (ignoreOutputCheck && actionType == IOType.OUTPUT)) {
            return maxParallelism;
        }

        return doItemIOInternal(component, context, maxParallelism, ResultChance.GUARANTEED);
    }

    @Override
    public int getParallelism() {
        return parallelism;
    }

    @Nonnull
    @Override
    public List<ProcessingComponent<?>> copyComponents(final List<ProcessingComponent<?>> components) {
        return ItemUtils.copyItemHandlerComponents(components);
    }

    private CraftCheck doItemIO(List<ProcessingComponent<?>> components, RecipeCraftingContext context, ResultChance chance) {
        int mul = doItemIOInternal(components, context, parallelism, chance);
        if (mul < parallelism) {
            return CraftCheck.failure("craftcheck.failure.item.input");
        }
        return CraftCheck.success();
    }

    private int doItemIOInternal(List<ProcessingComponent<?>> components, RecipeCraftingContext context, int maxMultiplier, ResultChance chance) {
        List<IItemHandlerImpl> handlers = new ArrayList<>();
        for (ProcessingComponent<?> component : components) {
            IItemHandlerImpl providedComponent = (IItemHandlerImpl) component.getProvidedComponent();
            handlers.add(providedComponent);
        }

        return consumeAllItems(handlers, context, maxMultiplier, chance);
    }

    public int consumeAllItems(final List<IItemHandlerImpl> handlers,
                               final RecipeCraftingContext context,
                               final int maxMultiplier,
                               final ResultChance chance)
    {
        int totalConsumed = 0;

        for (final ChancedIngredientStack ingredient : ingredients) {
            int toConsume = Math.round(RecipeModifier.applyModifiers(context, this, ingredient.count, false));
            int maxConsume = maxMultiplier - totalConsumed;
            int consumed = 0;

            AdvancedItemChecker checker;

            switch (ingredient.ingredientType) {
                case ITEMSTACK -> {
                    checker = ingredient.itemChecker;
                    ItemStack stack = ItemUtils.copyStackWithSize(ingredient.itemStack, toConsume);
                    if (!chance.canWork(RecipeModifier.applyModifiers(context, this, this.chance, true))) {
                        return maxMultiplier;
                    }
                    for (final IItemHandlerImpl handler : handlers) {
                        if (checker != null) {
                            consumed += ItemUtils.consumeAll(
                                    handler, stack, maxConsume - (consumed / toConsume), checker, context.getMachineController()) / toConsume;
                        } else {
                            consumed += ItemUtils.consumeAll(
                                    handler, stack, maxConsume - (consumed / toConsume), ingredient.tag);
                        }
                        if (consumed >= maxConsume) {
                            break;
                        }
                    }
                }
                case ORE_DICT -> {
                    checker = ingredient.itemChecker;
                    if (!chance.canWork(RecipeModifier.applyModifiers(context, this, this.chance, true))) {
                        return maxMultiplier;
                    }
                    for (final IItemHandlerImpl handler : handlers) {
                        if (checker != null) {
                            consumed += ItemUtils.consumeAll(
                                    handler, ingredient.oreDictName, toConsume, maxConsume - (consumed / toConsume), checker, context.getMachineController());
                        } else {
                            consumed += ItemUtils.consumeAll(
                                    handler, ingredient.oreDictName, toConsume, maxConsume - (consumed / toConsume), ingredient.tag);
                        }
                        if (consumed >= maxConsume) {
                            break;
                        }
                    }
                }
            }

            totalConsumed += (consumed / toConsume);
        }

        return totalConsumed;
    }

    @Override
    public void setParallelism(int parallelism) {
        if (!parallelizeUnaffected) {
            this.parallelism = parallelism;
        }
    }

    @Override
    public void setParallelizeUnaffected(boolean unaffected) {
        this.parallelizeUnaffected = unaffected;
        if (parallelizeUnaffected) {
            this.parallelism = 1;
        }
    }
}
