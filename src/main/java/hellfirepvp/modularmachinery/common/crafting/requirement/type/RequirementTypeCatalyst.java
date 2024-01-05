package hellfirepvp.modularmachinery.common.crafting.requirement.type;

import com.google.gson.JsonObject;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementCatalyst;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementItem;
import hellfirepvp.modularmachinery.common.integration.ingredient.IngredientItemStack;
import hellfirepvp.modularmachinery.common.machine.IOType;
import net.minecraft.item.ItemStack;

public class RequirementTypeCatalyst extends RequirementType<ItemStack, RequirementItem> {

    @Override
    public RequirementCatalyst createRequirement(IOType type, JsonObject jsonObject) {
        return null;
    }
}
