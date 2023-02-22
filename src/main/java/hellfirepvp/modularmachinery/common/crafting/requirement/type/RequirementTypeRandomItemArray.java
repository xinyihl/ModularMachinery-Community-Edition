package hellfirepvp.modularmachinery.common.crafting.requirement.type;

import com.google.gson.JsonObject;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementRandomItemArray;
import hellfirepvp.modularmachinery.common.machine.IOType;
import net.minecraft.item.ItemStack;

public class RequirementTypeRandomItemArray extends RequirementType<ItemStack, RequirementRandomItemArray> {
    @Override
    public RequirementRandomItemArray createRequirement(IOType type, JsonObject jsonObject) {
        return null;
    }
}
