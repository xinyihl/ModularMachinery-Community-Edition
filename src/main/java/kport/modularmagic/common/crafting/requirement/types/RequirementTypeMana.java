package kport.modularmagic.common.crafting.requirement.types;

import com.google.gson.JsonObject;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.crafting.requirement.type.RequirementType;
import hellfirepvp.modularmachinery.common.machine.IOType;
import kport.modularmagic.common.crafting.requirement.RequirementMana;
import kport.modularmagic.common.integration.jei.ingredient.Mana;
import kport.modularmagic.common.utils.RequirementUtils;

import javax.annotation.Nullable;

public class RequirementTypeMana extends RequirementType<Mana, RequirementMana> {

    @Override
    public ComponentRequirement<Mana, ? extends RequirementType<Mana, RequirementMana>> createRequirement(IOType type, JsonObject json) {
        int amount = RequirementUtils.getRequiredInt(json, "amount", ModularMagicRequirements.KEY_REQUIREMENT_MANA.toString());
        boolean perTick = RequirementUtils.getOptionalBoolean(json, "perTick", false);
        return new RequirementMana(type, amount, perTick);
    }

    @Nullable
    @Override
    public String requiresModid() {
        return "botania";
    }
}
