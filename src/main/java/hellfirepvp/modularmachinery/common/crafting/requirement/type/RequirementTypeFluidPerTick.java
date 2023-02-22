package hellfirepvp.modularmachinery.common.crafting.requirement.type;

import com.google.gson.JsonObject;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementFluidPerTick;
import hellfirepvp.modularmachinery.common.integration.ingredient.HybridFluid;
import hellfirepvp.modularmachinery.common.machine.IOType;

public class RequirementTypeFluidPerTick extends RequirementType<HybridFluid, RequirementFluidPerTick> {
    @Override
    public RequirementFluidPerTick createRequirement(IOType type, JsonObject jsonObject) {
        return null;
    }
}
