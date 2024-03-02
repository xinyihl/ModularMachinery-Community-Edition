package kport.gugu_utils.common.requirements.types;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.machine.IOType;
import kport.gugu_utils.common.requirements.RequirementAspect;
import kport.gugu_utils.common.requirements.RequirementAspectOutput;
import kport.gugu_utils.common.requirements.basic.ComponentRequirementAdapter;
import kport.gugu_utils.common.requirements.basic.RequirementUtils;
import thaumcraft.api.aspects.Aspect;

public class RequirementTypeAspect extends RequirementTypeAdapter<Integer> implements RequirementTypeAdapter.PerTick<Integer> {
    @Override
    public ComponentRequirementAdapter.PerTick<Integer> gererateRequirementPerTick(IOType type, JsonObject obj) {
        throw new UnsupportedOperationException("Not support this!");
    }

    @Override
    public ComponentRequirementAdapter<Integer> gererateRequirement(IOType type, JsonObject obj) {
        throw new UnsupportedOperationException("Not support this!");
    }

    @Override
    public ComponentRequirement createRequirement(IOType type, JsonObject obj) {
        String aspectType = RequirementUtils.tryGet(obj, "aspect", true).getAsString();
        int aspectAmount = RequirementUtils.tryGet(obj, "amount", true).getAsInt();
        Aspect aspect = Aspect.getAspect(aspectType);
        if (aspect == null) {
            throw new JsonParseException("Aspcet Invaild");
        }
        if (type == IOType.INPUT) {
            return RequirementAspect.createInput(aspectAmount, aspect);
        }
        return new RequirementAspectOutput(aspectAmount, aspect);
    }

}
