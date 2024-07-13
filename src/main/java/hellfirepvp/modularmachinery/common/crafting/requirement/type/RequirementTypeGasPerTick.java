package hellfirepvp.modularmachinery.common.crafting.requirement.type;

import com.google.gson.JsonObject;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementGasPerTick;
import hellfirepvp.modularmachinery.common.integration.ingredient.HybridFluid;
import hellfirepvp.modularmachinery.common.machine.IOType;

public class RequirementTypeGasPerTick extends RequirementType<HybridFluid, RequirementGasPerTick> {
    @Override
    public RequirementGasPerTick createRequirement(IOType type, JsonObject jsonObject) {
        return null;
    }
}
