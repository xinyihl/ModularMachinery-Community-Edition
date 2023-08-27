package hellfirepvp.modularmachinery.common.crafting.requirement;

import hellfirepvp.modularmachinery.common.crafting.helper.*;
import hellfirepvp.modularmachinery.common.crafting.requirement.type.RequirementTypeRandomItemArray;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import hellfirepvp.modularmachinery.common.util.ResultChance;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.List;

public class RequirementRandomItem extends ComponentRequirement<ItemStack, RequirementTypeRandomItemArray> implements ComponentRequirement.ChancedRequirement {

    public RequirementRandomItem(RequirementTypeRandomItemArray requirementType, IOType actionType) {
        super(requirementType, actionType);
    }

    @Override
    public boolean isValidComponent(ProcessingComponent<?> component, RecipeCraftingContext ctx) {
        return false;
    }

    @Override
    public boolean startCrafting(ProcessingComponent<?> component, RecipeCraftingContext context, ResultChance chance) {
        return false;
    }

    @Nonnull
    @Override
    public CraftCheck finishCrafting(ProcessingComponent<?> component, RecipeCraftingContext context, ResultChance chance) {
        return null;
    }

    @Nonnull
    @Override
    public CraftCheck canStartCrafting(ProcessingComponent<?> component, RecipeCraftingContext context, List<ComponentOutputRestrictor> restrictions) {
        return null;
    }

    @Override
    public ComponentRequirement<ItemStack, RequirementTypeRandomItemArray> deepCopy() {
        return null;
    }

    @Override
    public ComponentRequirement<ItemStack, RequirementTypeRandomItemArray> deepCopyModified(List<RecipeModifier> modifiers) {
        return null;
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
        return null;
    }

    @Override
    public JEIComponent<ItemStack> provideJEIComponent() {
        return null;
    }

    @Override
    public void setChance(float chance) {

    }
}
