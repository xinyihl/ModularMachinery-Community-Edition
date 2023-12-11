package hellfirepvp.modularmachinery.common.crafting.requirement.type;

import com.google.gson.JsonObject;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementCatalyst;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementItem;
import hellfirepvp.modularmachinery.common.integration.ingredient.IngredientItemStack;
import hellfirepvp.modularmachinery.common.machine.IOType;

public class RequirementTypeCatalyst extends RequirementType<IngredientItemStack, RequirementItem> {

    @Override
    public RequirementCatalyst createRequirement(IOType type, JsonObject jsonObject) {
        return null;
    }
}
