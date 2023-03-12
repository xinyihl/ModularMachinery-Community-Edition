package hellfirepvp.modularmachinery.common.crafting.requirement;

import hellfirepvp.modularmachinery.common.crafting.helper.ComponentOutputRestrictor;
import hellfirepvp.modularmachinery.common.crafting.helper.CraftCheck;
import hellfirepvp.modularmachinery.common.crafting.helper.ProcessingComponent;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.crafting.requirement.jei.JEIComponentCatalyst;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import hellfirepvp.modularmachinery.common.util.ResultChance;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class RequirementCatalyst extends RequirementItem {
    private final List<RecipeModifier> modifierList = new ArrayList<>();
    private final List<String> toolTipList = new ArrayList<>();
    private boolean isRequired = false;

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
    public CraftCheck canStartCrafting(ProcessingComponent<?> component, RecipeCraftingContext context, List<ComponentOutputRestrictor> restrictions) {
        CraftCheck craftCheck = super.canStartCrafting(component, context, restrictions);
        if (craftCheck.isSuccess()) {
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
        RequirementCatalyst catalyst;
        switch (this.requirementType) {
            case OREDICT:
                catalyst = new RequirementCatalyst(this.oreDictName, this.oreDictItemAmount);
                break;

            case FUEL:
                catalyst = new RequirementCatalyst(this.fuelBurntime);
                break;

            default:
            case ITEMSTACKS:
                catalyst = new RequirementCatalyst(this.required.copy());
                break;
        }
        catalyst.chance = this.chance;
        if (this.nbtChecker != null) {
            catalyst.nbtChecker = this.nbtChecker;
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
    public RequirementCatalyst deepCopyModified(List<RecipeModifier> modifiers) {
        RequirementCatalyst catalyst;
        switch (this.requirementType) {
            case OREDICT:
                int inOreAmt = Math.round(RecipeModifier.applyModifiers(modifiers, this, this.oreDictItemAmount, false));
                catalyst = new RequirementCatalyst(this.oreDictName, inOreAmt);
                break;
            case FUEL:
                int inFuel = Math.round(RecipeModifier.applyModifiers(modifiers, this, this.fuelBurntime, false));
                catalyst = new RequirementCatalyst(inFuel);
                break;
            default:
            case ITEMSTACKS:
                ItemStack inReq = this.required.copy();
                int amt = Math.round(RecipeModifier.applyModifiers(modifiers, this, inReq.getCount(), false));
                inReq.setCount(amt);
                catalyst = new RequirementCatalyst(inReq);
                break;
        }

        catalyst.chance = RecipeModifier.applyModifiers(modifiers, this, this.chance, true);
        if (this.tag != null) {
            catalyst.tag = this.tag.copy();
        }
        if (this.previewDisplayTag != null) {
            catalyst.previewDisplayTag = this.previewDisplayTag.copy();
        }
        catalyst.modifierList.addAll(this.modifierList);
        catalyst.toolTipList.addAll(toolTipList);
        return catalyst;
    }

    @Override
    public boolean startCrafting(ProcessingComponent<?> component, RecipeCraftingContext context, ResultChance chance) {
        if (isRequired) {
            return super.startCrafting(component, context, chance);
        } else {
            return true;
        }
    }

    @Override
    public JEIComponent<ItemStack> provideJEIComponent() {
        return new JEIComponentCatalyst(this);
    }
}
