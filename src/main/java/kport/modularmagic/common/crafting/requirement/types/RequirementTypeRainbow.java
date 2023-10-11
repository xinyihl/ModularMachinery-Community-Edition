package kport.modularmagic.common.crafting.requirement.types;

import com.google.gson.JsonObject;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.crafting.requirement.type.RequirementType;
import hellfirepvp.modularmachinery.common.machine.IOType;
import kport.modularmagic.common.crafting.requirement.RequirementRainbow;
import kport.modularmagic.common.integration.jei.ingredient.Rainbow;

import javax.annotation.Nullable;

public class RequirementTypeRainbow extends RequirementType<Rainbow, RequirementRainbow> {

    @Override
    public ComponentRequirement<Rainbow, ? extends RequirementType<Rainbow, RequirementRainbow>> createRequirement(IOType type, JsonObject jsonObject) {
        return new RequirementRainbow();
    }

    @Nullable
    @Override
    public String requiresModid() {
        return "extrautils2";
    }
}
