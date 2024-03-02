package kport.modularmagic.common.crafting.requirement.types;

import com.google.gson.JsonObject;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.crafting.requirement.type.RequirementType;
import hellfirepvp.modularmachinery.common.machine.IOType;
import kport.modularmagic.common.crafting.requirement.RequirementStarlightOutput;
import kport.modularmagic.common.integration.jei.ingredient.StarlightOutput;
import kport.modularmagic.common.utils.RequirementUtils;

import javax.annotation.Nullable;

public class RequirementTypeStarlightOutput extends RequirementType<StarlightOutput, RequirementStarlightOutput> {

    @Override
    public ComponentRequirement<StarlightOutput, ? extends RequirementType<StarlightOutput, RequirementStarlightOutput>> createRequirement(IOType type, JsonObject json) {
        float amount = RequirementUtils.getRequiredFloat(json, "amount", ModularMagicRequirements.KEY_REQUIREMENT_STARLIGHT_OUTPUT.toString());
        return new RequirementStarlightOutput(IOType.OUTPUT, amount);
    }

    @Nullable
    @Override
    public String requiresModid() {
        return "astralsorcery";
    }
}
