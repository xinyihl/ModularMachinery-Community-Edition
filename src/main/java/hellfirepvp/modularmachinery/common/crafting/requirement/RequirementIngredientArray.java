package hellfirepvp.modularmachinery.common.crafting.requirement;

import github.kasuminova.mmce.common.itemtype.ChancedIngredientStack;
import github.kasuminova.mmce.common.itemtype.IngredientStack;
import hellfirepvp.modularmachinery.common.crafting.helper.*;
import hellfirepvp.modularmachinery.common.crafting.requirement.jei.JEIComponentIngredientArray;
import hellfirepvp.modularmachinery.common.crafting.requirement.type.RequirementTypeIngredientArray;
import hellfirepvp.modularmachinery.common.lib.ComponentTypesMM;
import hellfirepvp.modularmachinery.common.lib.RequirementTypesMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import hellfirepvp.modularmachinery.common.util.IOInventory;
import hellfirepvp.modularmachinery.common.util.ItemUtils;
import hellfirepvp.modularmachinery.common.util.ResultChance;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class RequirementIngredientArray extends ComponentRequirement<ItemStack, RequirementTypeIngredientArray> implements ComponentRequirement.ChancedRequirement, ComponentRequirement.Parallelizable {
    public final List<ChancedIngredientStack> itemArray;
    public float chance = 1.0F;
    protected int parallelism = 1;
    protected boolean parallelizeUnaffected = false;

    /**
     * <p>物品组输入，仅消耗组内的其中一个</p>
     * <p>**仅限输入**</p>
     */
    public RequirementIngredientArray(List<ChancedIngredientStack> itemArray) {
        super(RequirementTypesMM.REQUIREMENT_INGREDIENT_ARRAY, IOType.INPUT);

        this.itemArray = itemArray;
    }

    @Override
    public boolean isValidComponent(ProcessingComponent<?> component, RecipeCraftingContext ctx) {
        MachineComponent<?> cmp = component.component;
        return cmp.getComponentType().equals(ComponentTypesMM.COMPONENT_ITEM) &&
                cmp instanceof MachineComponent.ItemBus &&
                cmp.ioType == actionType;
    }

    @Override
    public boolean startCrafting(ProcessingComponent<?> component, RecipeCraftingContext context, ResultChance chance) {
        IOInventory handler = (IOInventory) component.providedComponent;

        for (ChancedIngredientStack stack : itemArray) {
            float productionChance = RecipeModifier.applyModifiers(context, this, stack.chance, true) * this.chance;

            switch (stack.ingredientType) {
                case ITEMSTACK: {
                    ItemStack copiedStack = stack.itemStack.copy();
                    int amt = Math.round(RecipeModifier.applyModifiers(context, this, copiedStack.getCount(), false)) * parallelism;
                    copiedStack.setCount(amt);

                    if (ItemUtils.consumeFromInventory(handler, copiedStack, true, stack.tag)) {
                        if (chance.canProduce(productionChance)) {
                            return true;
                        } else {
                            return ItemUtils.consumeFromInventory(handler, copiedStack, false, stack.tag);
                        }
                    }
                    break;
                }
                case ORE_DICT: {
                    int amt = Math.round(RecipeModifier.applyModifiers(context, this, stack.count, false)) * parallelism;

                    if (ItemUtils.consumeFromInventoryOreDict(handler, stack.oreDictName, amt, true, stack.tag)) {
                        if (chance.canProduce(productionChance)) {
                            return true;
                        } else {
                            return ItemUtils.consumeFromInventoryOreDict(handler, stack.oreDictName, amt, false, stack.tag);
                        }
                    }
                    break;
                }
            }
        }

        return false;
    }

    @Nonnull
    @Override
    public CraftCheck finishCrafting(ProcessingComponent<?> component, RecipeCraftingContext context, ResultChance chance) {
        return CraftCheck.skipComponent();
    }

    @Nonnull
    @Override
    public CraftCheck canStartCrafting(ProcessingComponent<?> component, RecipeCraftingContext context, List<ComponentOutputRestrictor> restrictions) {
        IOInventory handler = (IOInventory) component.providedComponent;

        for (IngredientStack stack : itemArray) {
            switch (stack.ingredientType) {
                case ITEMSTACK: {
                    ItemStack copiedStack = stack.itemStack.copy();
                    int amt = Math.round(RecipeModifier.applyModifiers(context, this, copiedStack.getCount(), false)) * parallelism;
                    copiedStack.setCount(amt);
                    if (ItemUtils.consumeFromInventory(handler, copiedStack, true, copiedStack.getTagCompound())) {
                        return CraftCheck.success();
                    }
                    break;
                }
                case ORE_DICT: {
                    int amt = Math.round(RecipeModifier.applyModifiers(context, this, stack.count, false)) * parallelism;
                    if (ItemUtils.consumeFromInventoryOreDict(handler, stack.oreDictName, amt,true, stack.tag)) {
                        return CraftCheck.success();
                    }
                    break;
                }
            }
        }

        return CraftCheck.failure("craftcheck.failure.item.input");
    }

    @Override
    public ComponentRequirement<ItemStack, RequirementTypeIngredientArray> deepCopy() {
        RequirementIngredientArray copied = new RequirementIngredientArray(this.itemArray);
        copied.parallelizeUnaffected = this.parallelizeUnaffected;
        return copied;
    }

    @Override
    public ComponentRequirement<ItemStack, RequirementTypeIngredientArray> deepCopyModified(List<RecipeModifier> modifiers) {
        ArrayList<ChancedIngredientStack> newArray = new ArrayList<>(this.itemArray);
        newArray.forEach(item -> {
            switch (item.ingredientType) {
                case ITEMSTACK: {
                    ItemStack itemStack = item.itemStack;
                    int amt = Math.round(RecipeModifier.applyModifiers(modifiers, this, itemStack.getCount(), false));
                    itemStack.setCount(amt);
                    break;
                }
                case ORE_DICT: {
                    item.count = Math.round(RecipeModifier.applyModifiers(modifiers, this, item.count, false));
                    break;
                }
            }

            item.chance = RecipeModifier.applyModifiers(modifiers, this, item.chance, true);
        });
        RequirementIngredientArray copied = new RequirementIngredientArray(newArray);
        copied.parallelizeUnaffected = this.parallelizeUnaffected;
        return copied;
    }

    @Override
    public void startRequirementCheck(ResultChance contextChance, RecipeCraftingContext context) {
    }

    @Override
    public void endRequirementCheck() {
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
    public int maxParallelism(ProcessingComponent<?> component, RecipeCraftingContext context, int maxParallelism) {
        if (parallelizeUnaffected) {
            return maxParallelism;
        }
        IOInventory handler = (IOInventory) component.providedComponent;
        for (ChancedIngredientStack ingredientStack : this.itemArray) {
            switch (ingredientStack.ingredientType) {
                case ITEMSTACK: {
                    ItemStack stack = ItemUtils.copyStackWithSize(ingredientStack.itemStack, ingredientStack.count);
                    stack.setCount(Math.round(stack.getCount() * parallelism));
                    return ItemUtils.maxInputParallelism(handler, stack, maxParallelism, ingredientStack.tag);
                }
                case ORE_DICT: {
                    int amount = ingredientStack.count;
                    return ItemUtils.maxInputParallelism(handler, ingredientStack.oreDictName, amount, maxParallelism, ingredientStack.tag);
                }
            }
        }
        return maxParallelism;
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
