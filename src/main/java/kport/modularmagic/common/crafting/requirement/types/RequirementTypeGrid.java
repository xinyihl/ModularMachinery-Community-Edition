package kport.modularmagic.common.crafting.requirement.types;

import com.google.gson.JsonObject;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.crafting.requirement.type.RequirementType;
import hellfirepvp.modularmachinery.common.machine.IOType;
import kport.modularmagic.common.crafting.requirement.RequirementGrid;
import kport.modularmagic.common.integration.jei.ingredient.Grid;
import kport.modularmagic.common.utils.RequirementUtils;

import javax.annotation.Nullable;

public class RequirementTypeGrid extends RequirementType<Grid, RequirementGrid> {

    @Override
    public ComponentRequirement<Grid, ? extends RequirementType<Grid, RequirementGrid>> createRequirement(IOType type, JsonObject json) {
        float power = RequirementUtils.getRequiredPositiveFloat(json, "power", ModularMagicRequirements.KEY_REQUIREMENT_GRID.toString());
        return new RequirementGrid(type, power);
    }

    @Nullable
    @Override
    public String requiresModid() {
        return "extrautils2";
    }
}
