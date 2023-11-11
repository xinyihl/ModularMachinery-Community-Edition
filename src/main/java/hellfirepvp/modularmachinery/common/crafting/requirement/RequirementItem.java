/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.crafting.requirement;

import github.kasuminova.mmce.common.helper.AdvancedItemChecker;
import github.kasuminova.mmce.common.helper.AdvancedItemModifier;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.crafting.helper.CraftCheck;
import hellfirepvp.modularmachinery.common.crafting.helper.ProcessingComponent;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.crafting.requirement.jei.JEIComponentItem;
import hellfirepvp.modularmachinery.common.crafting.requirement.type.RequirementTypeItem;
import hellfirepvp.modularmachinery.common.lib.ComponentTypesMM;
import hellfirepvp.modularmachinery.common.lib.RequirementTypesMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import hellfirepvp.modularmachinery.common.util.Asyncable;
import hellfirepvp.modularmachinery.common.util.ItemUtils;
import hellfirepvp.modularmachinery.common.util.ResultChance;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.items.IItemHandlerModifiable;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: RequirementItem
 * Created by HellFirePvP
 * Date: 24.02.2018 / 12:35
 */
public class RequirementItem extends ComponentRequirement.MultiCompParallelizable<ItemStack, RequirementTypeItem>
        implements ComponentRequirement.ChancedRequirement, ComponentRequirement.Parallelizable, Asyncable {

    public final ItemRequirementType requirementType;

    public final ItemStack required;

    public final String oreDictName;
    public final int oreDictItemAmount;

    public final int fuelBurntime;

    public final List<ItemStack> previewItemStacks = new ArrayList<>();

    public final List<AdvancedItemModifier> itemModifierList = new ArrayList<>();

    public NBTTagCompound tag = null;
    public NBTTagCompound previewDisplayTag = null;

    public AdvancedItemChecker itemChecker = null;
    public float chance = 1F;

    public RequirementItem(IOType ioType, ItemStack item) {
        super(RequirementTypesMM.REQUIREMENT_ITEM, ioType);
        this.requirementType = ItemRequirementType.ITEMSTACKS;
        this.required = item.copy();
        this.oreDictName = null;
        this.oreDictItemAmount = 0;
        this.fuelBurntime = 0;
    }

    public RequirementItem(IOType ioType, String oreDictName, int oreDictAmount) {
        super(RequirementTypesMM.REQUIREMENT_ITEM, ioType);
        this.requirementType = ItemRequirementType.OREDICT;
        this.oreDictName = oreDictName;
        this.oreDictItemAmount = oreDictAmount;
        this.required = ItemStack.EMPTY;
        this.fuelBurntime = 0;
    }

    public RequirementItem(IOType actionType, int fuelBurntime) {
        super(RequirementTypesMM.REQUIREMENT_ITEM, actionType);
        this.requirementType = ItemRequirementType.FUEL;
        this.fuelBurntime = fuelBurntime;
        this.oreDictName = null;
        this.oreDictItemAmount = 0;
        this.required = ItemStack.EMPTY;
    }

    public void setItemChecker(AdvancedItemChecker itemChecker) {
        this.itemChecker = itemChecker;
    }

    public void addItemModifier(AdvancedItemModifier itemModifier) {
        this.itemModifierList.add(itemModifier);
    }

    @Override
    public int getSortingWeight() {
        return PRIORITY_WEIGHT_ITEM;
    }

    @Override
    public RequirementItem deepCopy() {
        return deepCopyModified(Collections.emptyList());
    }

    @Override
    public RequirementItem deepCopyModified(List<RecipeModifier> modifiers) {
        RequirementItem item;
        switch (this.requirementType) {
            case OREDICT -> {
                int inOreAmt = Math.round(RecipeModifier.applyModifiers(modifiers, this, this.oreDictItemAmount, false));
                item = new RequirementItem(this.actionType, this.oreDictName, inOreAmt);
            }
            case FUEL -> {
                int inFuel = Math.round(RecipeModifier.applyModifiers(modifiers, this, this.fuelBurntime, false));
                item = new RequirementItem(this.actionType, inFuel);
            }
            default -> {
                ItemStack inReq = this.required.copy();
                int amt = Math.round(RecipeModifier.applyModifiers(modifiers, this, inReq.getCount(), false));
                inReq.setCount(amt);
                item = new RequirementItem(this.actionType, inReq);
            }
        }
        item.chance = this.chance;
        if (this.itemChecker != null) {
            item.itemChecker = this.itemChecker;
        } else if (this.tag != null) {
            item.tag = this.tag.copy();
        }
        if (!this.itemModifierList.isEmpty()) {
            item.itemModifierList.addAll(this.itemModifierList);
        }
        if (this.previewDisplayTag != null) {
            item.previewDisplayTag = this.previewDisplayTag.copy();
        }
        return item;
    }

    @Override
    public JEIComponent<ItemStack> provideJEIComponent() {
        return new JEIComponentItem(this);
    }

    @Override
    public void setChance(float chance) {
        this.chance = chance;
    }

    @Nonnull
    @Override
    public String getMissingComponentErrorMessage(IOType ioType) {
        ResourceLocation compKey = this.getRequirementType().getRegistryName();
        return String.format("component.missing.%s.%s.%s",
                compKey.getNamespace(), compKey.getPath(), ioType.name().toLowerCase());
    }

    @Override
    public boolean isValidComponent(ProcessingComponent<?> component, RecipeCraftingContext ctx) {
        MachineComponent<?> cmp = component.component();
        return cmp.getComponentType().equals(ComponentTypesMM.COMPONENT_ITEM) &&
               cmp instanceof MachineComponent.ItemBus &&
               cmp.ioType == actionType;
    }

    @Override
    public void startCrafting(List<ProcessingComponent<?>> components, RecipeCraftingContext context, ResultChance chance) {
        if (actionType == IOType.INPUT) {
            doItemIO(components, context, itemModifierList, chance);
        }
    }

    @Override
    public void finishCrafting(final List<ProcessingComponent<?>> components, final RecipeCraftingContext context, final ResultChance chance) {
        if (actionType == IOType.OUTPUT) {
            doItemIO(components, context, itemModifierList, chance);
        }
    }

    @Nonnull
    @Override
    public CraftCheck canStartCrafting(final List<ProcessingComponent<?>> components, final RecipeCraftingContext context) {
        return doItemIO(components, context, Collections.emptyList(), ResultChance.GUARANTEED);
    }

    @Override
    public int getMaxParallelism(List<ProcessingComponent<?>> components, RecipeCraftingContext context, int maxParallelism) {
        if (parallelizeUnaffected || (ignoreOutputCheck && actionType == IOType.OUTPUT)) {
            return maxParallelism;
        }

        return doItemIOInternal(components, context, maxParallelism, Collections.emptyList(), ResultChance.GUARANTEED);
    }

    private CraftCheck doItemIO(List<ProcessingComponent<?>> components, RecipeCraftingContext context, List<AdvancedItemModifier> itemModifiers, ResultChance chance) {
        int mul = doItemIOInternal(components, context, parallelism, itemModifiers, chance);
        if (mul < parallelism) {
            return switch (actionType) {
                case INPUT -> CraftCheck.failure("craftcheck.failure.item.input");
                case OUTPUT -> CraftCheck.failure("craftcheck.failure.item.output.space");
            };
        }
        return CraftCheck.success();
    }

    private int doItemIOInternal(final List<ProcessingComponent<?>> components,
                                 final RecipeCraftingContext context,
                                 final int maxMultiplier,
                                 final List<AdvancedItemModifier> itemModifiers,
                                 final ResultChance chance)
    {
        List<IItemHandlerModifiable> handlers = new ArrayList<>();
        for (ProcessingComponent<?> component : components) {
            IItemHandlerModifiable providedComponent = (IItemHandlerModifiable) component.getProvidedComponent();
            handlers.add(providedComponent);
        }

        return switch (actionType) {
            case INPUT -> consumeAllItems(handlers, context, maxMultiplier, itemModifiers, chance);
            case OUTPUT -> {
                if (ignoreOutputCheck) {
                    insertAllItems(handlers, context, maxMultiplier, itemModifiers, chance);
                    yield maxMultiplier;
                }
                yield insertAllItems(handlers, context, maxMultiplier, itemModifiers, chance);
            }
        };

    }

    @Nonnull
    @Override
    public List<ProcessingComponent<?>> copyComponents(final List<ProcessingComponent<?>> components) {
        return ItemUtils.copyItemHandlerComponents(components);
    }

    public int consumeAllItems(final List<IItemHandlerModifiable> handlers,
                               final RecipeCraftingContext context,
                               final int maxMultiplier,
                               final List<AdvancedItemModifier> itemModifiers,
                               final ResultChance chance)
    {
        int consumed = 0;
        int toConsume = switch (this.requirementType) {
            case ITEMSTACKS -> Math.round(RecipeModifier.applyModifiers(context, this, required.getCount(), false));
            case OREDICT -> Math.round(RecipeModifier.applyModifiers(context, this, oreDictItemAmount, false));
            default -> 0;
        };

        int maxConsume = toConsume * maxMultiplier;

        if (toConsume <= 0) {
            return maxMultiplier;
        }

        ItemStack stack = ItemStack.EMPTY;

        switch (this.requirementType) {
            case ITEMSTACKS -> {
                stack = required.copy();
                if (tag != null) {
                    stack.setTagCompound(tag);
                }

                if (!itemModifiers.isEmpty()) {
                    stack.setCount(toConsume);

                    for (final AdvancedItemModifier modifier : itemModifiers) {
                        stack = modifier.apply(context.getMachineController(), stack);
                    }

                    toConsume = stack.getCount();
                }
            }
            case OREDICT -> {
                if (!itemModifiers.isEmpty()) {
                    stack = ItemUtils.getOredictItem(context, oreDictName, tag);
                    if (stack.isEmpty()) {
                        return maxMultiplier;
                    }

                    stack.setCount(toConsume);

                    if (tag != null) {
                        stack.setTagCompound(tag);
                    }

                    for (final AdvancedItemModifier modifier : itemModifiers) {
                        stack = modifier.apply(context.getMachineController(), stack);
                    }

                    toConsume = stack.getCount();
                }
            }
        }

        if (toConsume <= 0) {
            return maxMultiplier;
        }

        if (!chance.canWork(RecipeModifier.applyModifiers(context, this, this.chance, true))) {
            return maxMultiplier;
        }

        switch (this.requirementType) {
            case ITEMSTACKS -> {
                for (final IItemHandlerModifiable handler : handlers) {
                    stack.setCount(maxConsume - consumed);
                    if (itemChecker != null) {
                        consumed += ItemUtils.consumeAll(handler, stack, itemChecker, context.getMachineController());
                    } else {
                        consumed += ItemUtils.consumeAll(handler, stack, tag);
                    }
                    if (consumed >= maxConsume) {
                        break;
                    }
                }
            }
            case OREDICT -> {
                for (final IItemHandlerModifiable handler : handlers) {
                    if (itemChecker != null) {
                        consumed += ItemUtils.consumeAll(handler, oreDictName, maxConsume - consumed, itemChecker, context.getMachineController());
                    } else {
                        consumed += ItemUtils.consumeAll(handler, oreDictName, maxConsume - consumed, tag);
                    }
                    if (consumed >= maxConsume) {
                        break;
                    }
                }
            }
        }

        return consumed / toConsume;
    }

    public int insertAllItems(final List<IItemHandlerModifiable> handlers,
                              final RecipeCraftingContext context,
                              final int maxMultiplier,
                              final List<AdvancedItemModifier> itemModifiers,
                              final ResultChance chance)
    {
        if (fuelBurntime > 0 && oreDictName == null && required.isEmpty()) {
            throw new IllegalStateException("Invalid item output!");
        }

        int inserted = 0;
        int toInsert = switch (this.requirementType) {
            case ITEMSTACKS -> Math.round(RecipeModifier.applyModifiers(context, this, required.getCount(), false));
            case OREDICT -> Math.round(RecipeModifier.applyModifiers(context, this, oreDictItemAmount, false));
            default -> 0;
        };

        if (toInsert <= 0) {
            return maxMultiplier;
        }

        ItemStack stack;
        switch (this.requirementType) {
            case ITEMSTACKS -> stack = ItemUtils.copyStackWithSize(required, 1);
            case OREDICT -> stack = ItemUtils.getOredictItem(context, oreDictName, tag);
            default -> {
                return 0;
            }
        }
        if (tag != null) {
            stack.setTagCompound(tag);
        }

        if (!itemModifiers.isEmpty()) {
            for (final AdvancedItemModifier modifier : itemModifiers) {
                stack = modifier.apply(context.getMachineController(), stack);
            }
            toInsert *= stack.getCount();
            if (toInsert <= 0) {
                return maxMultiplier;
            }
            stack.setCount(1);
        }

        if (!chance.canWork(RecipeModifier.applyModifiers(context, this, this.chance, true))) {
            return maxMultiplier;
        }

        int maxInsert = toInsert * maxMultiplier;
        for (final IItemHandlerModifiable handler : handlers) {
            synchronized (handler) {
                inserted += ItemUtils.insertAll(stack, handler, maxInsert - inserted);
            }
            if (inserted >= maxInsert) {
                break;
            }
        }

        return inserted / toInsert;
    }

    public enum ItemRequirementType {
        ITEMSTACKS,
        OREDICT,
        FUEL,
        ITEMSTACK_ARRAY

    }

}
