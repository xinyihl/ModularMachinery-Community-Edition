package kport.modularmagic.common.crafting.requirement.types;

import com.google.gson.JsonObject;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.crafting.requirement.type.RequirementType;
import hellfirepvp.modularmachinery.common.machine.IOType;
import kport.modularmagic.common.crafting.requirement.RequirementStarlight;
import kport.modularmagic.common.integration.jei.ingredient.Starlight;
import kport.modularmagic.common.utils.RequirementUtils;

import javax.annotation.Nullable;

public class RequirementTypeStarlight extends RequirementType<Starlight, RequirementStarlight> {

    @Override
    public ComponentRequirement<Starlight, ? extends RequirementType<Starlight, RequirementStarlight>> createRequirement(IOType type, JsonObject json) {
        float amount = RequirementUtils.getRequiredFloat(json, "amount", ModularMagicRequirements.KEY_REQUIREMENT_STARLIGHT.toString());
        return new RequirementStarlight(type, amount);
    }

    @Nullable
    @Override
    public String requiresModid() {
        return "astralsorcery";
    }
}
