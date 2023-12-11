package hellfirepvp.modularmachinery.common.crafting.requirement;

import hellfirepvp.modularmachinery.common.crafting.helper.CraftCheck;
import hellfirepvp.modularmachinery.common.crafting.helper.ProcessingComponent;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.crafting.requirement.jei.JEIComponentCatalyst;
import hellfirepvp.modularmachinery.common.integration.ingredient.IngredientItemStack;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import hellfirepvp.modularmachinery.common.util.ResultChance;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RequirementCatalyst extends RequirementItem {
    protected final List<RecipeModifier> modifierList = new ArrayList<>();
    protected final List<String> toolTipList = new ArrayList<>();
    protected boolean isRequired = false;

    public RequirementCatalyst(ItemStack item) {
        super(IOType.INPUT, item);
    }

    public RequirementCatalyst(String oreDictName, int oreDictAmount) {
        super(IOType.INPUT, oreDictName, oreDictAmount);
    }

    public RequirementCatalyst(int fuelBurntime) {
        super(IOType.INPUT, fuelBurntime);
    }

    public void addModifier(RecipeModifier modifier) {
        modifierList.add(modifier);
    }

    public void addTooltip(String tooltip) {
        toolTipList.add(tooltip);
    }

    public List<String> getToolTipList() {
        return toolTipList;
    }

    @Nonnull
    @Override
    public CraftCheck canStartCrafting(List<ProcessingComponent<?>> components, RecipeCraftingContext context) {
        if (super.canStartCrafting(components, context).isSuccess()) {
            for (RecipeModifier modifier : modifierList) {
                context.addPermanentModifier(modifier);
            }
            isRequired = true;
            return CraftCheck.success();
        }
        return CraftCheck.skipComponent();
    }

    @Override
    public RequirementCatalyst deepCopy() {
        return deepCopyModified(Collections.emptyList());
    }

    @Override
    public RequirementCatalyst deepCopyModified(List<RecipeModifier> modifiers) {
        RequirementCatalyst catalyst;
        switch (this.requirementType) {
            case OREDICT -> {
                int inOreAmt = Math.round(RecipeModifier.applyModifiers(modifiers, this, this.oreDictItemAmount, false));
                catalyst = new RequirementCatalyst(this.oreDictName, inOreAmt);
            }
            case FUEL -> {
                int inFuel = Math.round(RecipeModifier.applyModifiers(modifiers, this, this.fuelBurntime, false));
                catalyst = new RequirementCatalyst(inFuel);
            }
            default -> {
                ItemStack inReq = this.required.copy();
                int amt = Math.round(RecipeModifier.applyModifiers(modifiers, this, inReq.getCount(), false));
                inReq.setCount(amt);
                catalyst = new RequirementCatalyst(inReq);
            }
        }
        catalyst.chance = this.chance;
        if (this.itemChecker != null) {
            catalyst.itemChecker = this.itemChecker;
        } else if (this.tag != null) {
            catalyst.tag = this.tag.copy();
        }
        if (!this.itemModifierList.isEmpty()) {
            catalyst.itemModifierList.addAll(this.itemModifierList);
        }
        if (this.previewDisplayTag != null) {
            catalyst.previewDisplayTag = this.previewDisplayTag.copy();
        }
        catalyst.modifierList.addAll(this.modifierList);
        catalyst.toolTipList.addAll(toolTipList);
        return catalyst;
    }

    @Override
    public void startCrafting(List<ProcessingComponent<?>> components, RecipeCraftingContext context, ResultChance chance) {
        if (isRequired) {
            super.startCrafting(components, context, chance);
        }
    }

    @Override
    public JEIComponent<IngredientItemStack> provideJEIComponent() {
        return new JEIComponentCatalyst(this);
    }
}
