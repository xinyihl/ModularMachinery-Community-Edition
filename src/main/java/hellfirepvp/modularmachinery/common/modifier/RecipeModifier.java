/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.modifier;

import com.google.gson.*;
import crafttweaker.annotations.ZenRegister;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.crafting.IntegrationTypeHelper;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.crafting.requirement.type.RequirementType;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.RecipeModifierBuilder;
import hellfirepvp.modularmachinery.common.lib.RegistriesMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import stanhebben.zenscript.annotations.ZenClass;

import javax.annotation.Nullable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: RecipeModifier
 * Created by HellFirePvP
 * Date: 30.03.2018 / 10:48
 */
@ZenRegister
@ZenClass("mods.modularmachinery.RecipeModifier")
public class RecipeModifier {

    public static final String IO_INPUT = "input";
    public static final String IO_OUTPUT = "output";

    public static final int OPERATION_ADD = 0;
    public static final int OPERATION_MULTIPLY = 1;

    @Nullable
    protected final RequirementType<?, ?> target;
    protected final IOType ioTarget;
    protected final float modifier;
    protected final int operation;
    protected final boolean chance;

    public RecipeModifier(@Nullable RequirementType<?, ?> target, IOType ioTarget, float modifier, int operation, boolean affectsChance) {
        this.target = target;
        this.ioTarget = ioTarget;
        this.modifier = modifier;
        this.operation = operation;
        this.chance = affectsChance;
    }

    public NBTTagCompound serialize() {
        NBTTagCompound compound = new NBTTagCompound();
        compound.setString("target", (target == null || target.getRegistryName() == null) ? "" : target.getRegistryName().toString());
        compound.setInteger("ioTarget", ioTarget == IOType.INPUT ? 0 : 1);
        compound.setInteger("operation", operation);
        compound.setFloat("value", modifier);
        compound.setBoolean("chance", chance);

        return compound;
    }

    public static RecipeModifier deserialize(NBTTagCompound compound) {
        if (compound.hasKey("target") && compound.hasKey("ioTarget") && compound.hasKey("operation") && compound.hasKey("value") && compound.hasKey("chance")) {
            return RecipeModifierBuilder.newBuilder()
                    .setRequirementType(compound.getString("target"))
                    .setIOType(compound.getInteger("ioTarget") == 0 ? IO_INPUT : IO_OUTPUT)
                    .setOperation(compound.getInteger("operation"))
                    .setValue(compound.getFloat("value"))
                    .isAffectChance(compound.getBoolean("chance"))
                    .build();
        }
        return null;
    }

    public static float applyModifiers(RecipeCraftingContext context, ComponentRequirement<?, ?> in, float value, boolean isChance) {
        return applyModifiers(context, in.requirementType, in.getActionType(), value, isChance);
    }

    public static float applyModifiers(RecipeCraftingContext context, RequirementType<?, ?> target, IOType ioType, float value, boolean isChance) {
        return context.getModifierApplier(target, isChance).apply(value, ioType);
    }

    public static float applyModifiers(Collection<RecipeModifier> modifiers, ComponentRequirement<?, ?> in, float value, boolean isChance) {
        return applyModifiers(modifiers, in.requirementType, in.getActionType(), value, isChance);
    }

    public static float applyModifiers(Collection<RecipeModifier> modifiers, RequirementType<?, ?> target, IOType ioType, float value, boolean isChance) {
        List<RecipeModifier> applicable = new ArrayList<>();
        for (RecipeModifier recipeModifier : modifiers) {
            if (recipeModifier.target != null && recipeModifier.target.equals(target)) {
                if (ioType == null || recipeModifier.ioTarget == ioType) {
                    if (recipeModifier.affectsChance() == isChance) {
                        applicable.add(recipeModifier);
                    }
                }
            }
        }
        float add = 0F;
        float mul = 1F;
        for (RecipeModifier mod : applicable) {
            if (mod.operation == OPERATION_ADD) {
                add += mod.modifier;
            } else if (mod.operation == OPERATION_MULTIPLY) {
                mul *= mod.modifier;
            } else {
                throw new IllegalArgumentException("Unknown modifier operation: " + mod.operation);
            }
        }
        return (value + add) * mul;
    }

    @Nullable
    public RequirementType<?, ?> getTarget() {
        return target;
    }

    public IOType getIOTarget() {
        return ioTarget;
    }

    public float getModifier() {
        return modifier;
    }

    public boolean affectsChance() {
        return chance;
    }

    public int getOperation() {
        return operation;
    }

    public static class ModifierApplier {
        public static final ModifierApplier DEFAULT_APPLIER = new ModifierApplier();

        public float inputAdd = 0;
        public float inputMul = 1;
        public float outputAdd = 0;
        public float outputMul = 1;

        public float apply(final float value, final IOType ioType) {
            return ioType == null || ioType == IOType.INPUT
                    ? (value + inputAdd) * inputMul
                    : (value + outputAdd) * outputMul;
        }

        public boolean isDefault() {
            return inputAdd == 0 && inputMul == 0 && outputAdd == 0 && outputMul == 0;
        }
    }

    public static class Deserializer implements JsonDeserializer<RecipeModifier> {

        @Override
        public RecipeModifier deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject part = json.getAsJsonObject();
            if (!part.has("io") || !part.get("io").isJsonPrimitive() || !part.getAsJsonPrimitive("io").isString()) {
                throw new JsonParseException("'io' string-tag not found when deserializing recipemodifier!");
            }
            String ioTarget = part.getAsJsonPrimitive("io").getAsString();
            IOType ioType = IOType.getByString(ioTarget);
            if (ioType == null) {
                throw new JsonParseException("Unknown machine iotype: " + ioTarget);
            }
            if (!part.has("target") || !part.get("target").isJsonPrimitive() || !part.getAsJsonPrimitive("target").isString()) {
                throw new JsonParseException("'target' string-tag not found when deserializing recipemodifier!");
            }
            String targetStr = part.getAsJsonPrimitive("target").getAsString();
            RequirementType<?, ?> target = RegistriesMM.REQUIREMENT_TYPE_REGISTRY.getValue(new ResourceLocation(targetStr));
            if (target == null) {
                target = IntegrationTypeHelper.searchRequirementType(targetStr);
                if (target != null) {
                    ModularMachinery.log.info("[Modular Machinery]: Deprecated requirement name '"
                            + targetStr + "'! Consider using " + target.getRegistryName().toString());
                }
            }
            if (!part.has("multiplier") || !part.get("multiplier").isJsonPrimitive() || !part.getAsJsonPrimitive("multiplier").isNumber()) {
                throw new JsonParseException("'multiplier' float-tag not found when deserializing recipemodifier!");
            }
            float multiplier = part.getAsJsonPrimitive("multiplier").getAsFloat();
            if (!part.has("operation") || !part.get("operation").isJsonPrimitive() || !part.getAsJsonPrimitive("operation").isNumber()) {
                throw new JsonParseException("'operation' int-tag not found when deserializing recipemodifier!");
            }
            int operation = part.getAsJsonPrimitive("operation").getAsInt();
            if (operation < 0 || operation > 1) {
                throw new JsonParseException("There are currently only operation 0 and 1 available (add and multiply)! Found: " + operation);
            }
            boolean affectsChance = false;
            if (part.has("affectChance")) {
                if (!part.get("affectChance").isJsonPrimitive() || !part.getAsJsonPrimitive("affectChance").isBoolean()) {
                    throw new JsonParseException("'affectChance', if defined, needs to be either true or false!");
                }
                affectsChance = part.getAsJsonPrimitive("affectChance").getAsBoolean();
            }
            return new RecipeModifier(target, ioType, multiplier, operation, affectsChance);
        }
    }

}
