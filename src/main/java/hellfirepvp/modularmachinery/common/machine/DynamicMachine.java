/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.machine;

import com.google.common.collect.Lists;
import com.google.gson.*;
import crafttweaker.util.IEventHandler;
import hellfirepvp.modularmachinery.common.crafting.ActiveMachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.MachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.RecipeRegistry;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentSelectorTag;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.event.machine.MachineEvent;
import hellfirepvp.modularmachinery.common.modifier.ModifierReplacement;
import hellfirepvp.modularmachinery.common.tiles.TileMachineController;
import hellfirepvp.modularmachinery.common.util.BlockArray;
import hellfirepvp.modularmachinery.common.util.IBlockStateDescriptor;
import hellfirepvp.modularmachinery.common.util.SmartInterfaceType;
import hellfirepvp.modularmachinery.common.util.nbt.NBTJsonDeserializer;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Type;
import java.util.*;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: DynamicMachine
 * Created by HellFirePvP
 * Date: 27.06.2017 / 13:57
 */
public class DynamicMachine extends AbstractMachine {
    private final Map<BlockPos, List<ModifierReplacement>> modifiers = new HashMap<>();
    private final Map<Class<?>, List<IEventHandler<MachineEvent>>> machineEventHandlers = new HashMap<>();
    private final HashMap<String, SmartInterfaceType> smartInterfaces = new HashMap<>();
    private TaggedPositionBlockArray pattern = new TaggedPositionBlockArray();

    public DynamicMachine(String registryName) {
        super(registryName);
    }

    public boolean hasSmartInterfaceType(String type) {
        return smartInterfaces.containsKey(type);
    }

    public SmartInterfaceType getSmartInterfaceType(String type) {
        return smartInterfaces.get(type);
    }

    public void addSmartInterfaceType(SmartInterfaceType type) {
        smartInterfaces.put(type.getType(), type);
    }

    public Optional<SmartInterfaceType> getFirstSmartInterfaceType() {
        return smartInterfaces.values().stream().sorted().findFirst();
    }

    public boolean smartInterfaceTypesIsEmpty() {
        return smartInterfaces.isEmpty();
    }

    public Map<String, SmartInterfaceType> getFilteredType(Collection<String> ignoredTypes) {
        Map<String, SmartInterfaceType> filtered = new HashMap<>();
        smartInterfaces.forEach((type, data) -> {
            if (!ignoredTypes.contains(type)) {
                filtered.put(type, data);
            }
        });

        return filtered;
    }

    @SuppressWarnings("unchecked")
    public <H extends MachineEvent> void addMachineEventHandler(Class<H> hClass, IEventHandler<H> handler) {
        machineEventHandlers.putIfAbsent(hClass, new ArrayList<>());
        machineEventHandlers.get(hClass).add((IEventHandler<MachineEvent>) handler);
    }

    @Nullable
    public List<IEventHandler<MachineEvent>> getMachineEventHandlers(Class<?> handlerClass) {
        return machineEventHandlers.get(handlerClass);
    }

    public Map<Class<?>, List<IEventHandler<MachineEvent>>> getMachineEventHandlers() {
        return machineEventHandlers;
    }

    public TaggedPositionBlockArray getPattern() {
        return pattern;
    }

    public Map<BlockPos, List<ModifierReplacement>> getModifiers() {
        return modifiers;
    }

    @Nonnull
    public ModifierReplacementMap getModifiersAsMatchingReplacements() {
        ModifierReplacementMap infoMap = new ModifierReplacementMap();
        for (BlockPos pos : modifiers.keySet()) {
            List<ModifierReplacement> replacements = modifiers.get(pos);
            List<BlockArray.BlockInformation> informationList = new ArrayList<>();
            for (ModifierReplacement replacement : replacements) {
                informationList.add(replacement.getBlockInformation());
            }

            infoMap.put(pos, informationList);
        }
        return infoMap;
    }

    @Nonnull
    public Iterable<MachineRecipe> getAvailableRecipes() {
        return RecipeRegistry.getRecipesFor(this);
    }

    public RecipeCraftingContext createContext(ActiveMachineRecipe activeRecipe,
                                               TileMachineController controller,
                                               Collection<Tuple<MachineComponent<?>, ComponentSelectorTag>> taggedComponents,
                                               Collection<ModifierReplacement> modifiers) {
        if (!activeRecipe.getRecipe().getOwningMachineIdentifier().equals(registryName)) {
            throw new IllegalArgumentException("Tried to create context for a recipe that doesn't belong to the referenced machine!");
        }
        RecipeCraftingContext context = new RecipeCraftingContext(activeRecipe, controller);
        for (Tuple<MachineComponent<?>, ComponentSelectorTag> tpl : taggedComponents) {
            context.addComponent(tpl.getFirst(), tpl.getSecond());
        }
        for (ModifierReplacement modifier : modifiers) {
            context.addModifier(modifier);
        }
        return context;
    }

    public void mergeFrom(DynamicMachine another) {
        smartInterfaces.putAll(another.smartInterfaces);
        modifiers.putAll(another.modifiers);
        pattern = another.pattern;
        machineEventHandlers.putAll(another.machineEventHandlers);
    }

    public static class ModifierReplacementMap extends HashMap<BlockPos, List<BlockArray.BlockInformation>> {

        public ModifierReplacementMap rotateYCCW() {
            ModifierReplacementMap map = new ModifierReplacementMap();

            for (BlockPos pos : keySet()) {
                List<BlockArray.BlockInformation> infoList = this.get(pos);
                List<BlockArray.BlockInformation> copyRotated = new ArrayList<>(infoList.size());
                for (BlockArray.BlockInformation info : infoList) {
                    copyRotated.add(info.copyRotateYCCW());
                }
                map.put(new BlockPos(pos.getZ(), pos.getY(), -pos.getX()), copyRotated);
            }

            return map;
        }

    }

    public static class MachineDeserializer implements JsonDeserializer<DynamicMachine> {

        private static List<BlockPos> buildPermutations(List<Integer> avX, List<Integer> avY, List<Integer> avZ) {
            List<BlockPos> out = new ArrayList<>(avX.size() * avY.size() * avZ.size());
            for (int x : avX) {
                for (int y : avY) {
                    for (int z : avZ) {
                        out.add(new BlockPos(x, y, z));
                    }
                }
            }
            return out;
        }

        private static void addModifierWithPattern(DynamicMachine machine, ModifierReplacement mod, JsonObject part) throws JsonParseException {
            List<Integer> avX = new ArrayList<>();
            List<Integer> avY = new ArrayList<>();
            List<Integer> avZ = new ArrayList<>();
            addCoordinates("x", part, avX);
            addCoordinates("y", part, avY);
            addCoordinates("z", part, avZ);

            for (BlockPos permutation : buildPermutations(avX, avY, avZ)) {
                if (permutation.getX() == 0 && permutation.getY() == 0 && permutation.getZ() == 0) {
                    continue; //We're not going to overwrite the controller.
                }
                machine.modifiers.putIfAbsent(permutation, Lists.newArrayList());
                machine.modifiers.get(permutation).add(mod);
            }
        }

        private static void addDescriptorWithPattern(TaggedPositionBlockArray pattern, BlockArray.BlockInformation information, JsonObject part) throws JsonParseException {
            List<Integer> avX = new ArrayList<>();
            List<Integer> avY = new ArrayList<>();
            List<Integer> avZ = new ArrayList<>();
            addCoordinates("x", part, avX);
            addCoordinates("y", part, avY);
            addCoordinates("z", part, avZ);

            String tag = null;
            if (part.has("selector-tag")) {
                JsonElement strTag = part.get("selector-tag");
                if (!strTag.isJsonPrimitive()) {
                    throw new JsonParseException("The 'selector-tag' in an element must be a string!");
                }
                tag = strTag.getAsString();
            }
            ComponentSelectorTag selector = tag != null && !tag.isEmpty() ? new ComponentSelectorTag(tag) : null;

            for (BlockPos permutation : buildPermutations(avX, avY, avZ)) {
                if (permutation.getX() == 0 && permutation.getY() == 0 && permutation.getZ() == 0) {
                    continue; //We're not going to overwrite the controller.
                }
                pattern.addBlock(permutation, information);

                if (tag != null && !tag.isEmpty()) {
                    pattern.setTag(permutation, selector);
                }
            }
        }

        private static void addCoordinates(String key, JsonObject part, List<Integer> out) throws JsonParseException {
            if (!part.has(key)) {
                out.add(0);
                return;
            }
            JsonElement coordinateElement = part.get(key);
            if (coordinateElement.isJsonPrimitive() && coordinateElement.getAsJsonPrimitive().isNumber()) {
                out.add(coordinateElement.getAsInt());
            } else if (coordinateElement.isJsonArray() && coordinateElement.getAsJsonArray().size() > 0) {
                for (JsonElement element : coordinateElement.getAsJsonArray()) {
                    if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
                        out.add(element.getAsInt());
                    } else {
                        throw new JsonParseException("Expected only numbers in JsonArray " + coordinateElement + " but found " + element);
                    }
                }
            }
        }

        @Override
        public DynamicMachine deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject root = json.getAsJsonObject();
            String registryName = JsonUtils.getString(root, "registryname", "");
            if (registryName.isEmpty()) {
                registryName = JsonUtils.getString(root, "registryName", "");
                if (registryName.isEmpty()) {
                    throw new JsonParseException("Invalid/Missing 'registryname' !");
                }
            }
            String localized = JsonUtils.getString(root, "localizedname", "");
            if (localized.isEmpty()) {
                throw new JsonParseException("Invalid/Missing 'localizedname' !");
            }
            JsonArray parts = JsonUtils.getJsonArray(root, "parts", new JsonArray());
            if (parts.size() == 0) {
                throw new JsonParseException("Empty/Missing 'parts'!");
            }
            DynamicMachine machine = new DynamicMachine(registryName);
            machine.setLocalizedName(localized);

            //Failure Action
            if (root.has("failure-action")) {
                JsonElement failureAction = root.get("failure-action");
                if (!failureAction.isJsonPrimitive() || !failureAction.getAsJsonPrimitive().isString()) {
                    throw new JsonParseException("'failure-action' has to be 'reset', 'still' or 'decrease'!");
                }
                String action = failureAction.getAsJsonPrimitive().getAsString();
                machine.setFailureAction(RecipeFailureActions.getFailureAction(action));
            }

            //Requires Blueprint
            if (root.has("requires-blueprint")) {
                JsonElement elementBlueprint = root.get("requires-blueprint");
                if (!elementBlueprint.isJsonPrimitive() || !elementBlueprint.getAsJsonPrimitive().isBoolean()) {
                    throw new JsonParseException("'requires-blueprint' has to be either 'true' or 'false'!");
                }
                machine.setRequiresBlueprint(elementBlueprint.getAsJsonPrimitive().getAsBoolean());
            }

            //Color
            if (root.has("color")) {
                JsonElement elementColor = root.get("color");
                if (!elementColor.isJsonPrimitive()) {
                    throw new JsonParseException("The Color defined in 'color' should be a hex integer number! Found " + elementColor + " instead!");
                }
                int hexColor;
                String hexStr = elementColor.getAsJsonPrimitive().getAsString();
                try {
                    hexColor = Integer.parseInt(hexStr, 16);
                } catch (NumberFormatException parseExc) {
                    throw new JsonParseException("The Color defined in 'color' should be a hex integer number! Found " + elementColor + " instead!", parseExc);
                }
                machine.definedColor = hexColor;
            }

            //Parallelizable
            if (root.has("parallelizable")) {
                JsonElement parallelizable = root.get("parallelizable");
                if (!parallelizable.isJsonPrimitive()) {
                    throw new JsonParseException("The 'parallelizable' should be a boolean! Found " + parallelizable + " instead!");
                }
                machine.parallelizable = parallelizable.getAsBoolean();
            }

            //Max Parallelism
            if (root.has("max-parallelism")) {
                JsonElement maxParallelism = root.get("max-parallelism");
                if (!maxParallelism.isJsonPrimitive()) {
                    throw new JsonParseException("The 'max-parallelism' should be a integer! Found " + maxParallelism + " instead!");
                }
                machine.maxParallelism = maxParallelism.getAsInt();
            }

            //Parts
            for (int i = 0; i < parts.size(); i++) {
                JsonElement element = parts.get(i);
                if (!element.isJsonObject()) {
                    throw new JsonParseException("A part of 'parts' is not a compound object!");
                }
                JsonObject part = element.getAsJsonObject();
                NBTTagCompound match = null;
                if (part.has("nbt")) {
                    JsonElement je = part.get("nbt");
                    if (!je.isJsonObject()) {
                        throw new JsonParseException("The ComponentType 'nbt' expects a json compound that defines the NBT tag to match the tileentity's nbt against!");
                    }
                    String jsonStr = je.toString();
                    try {
                        match = NBTJsonDeserializer.deserialize(jsonStr);
                    } catch (NBTException exc) {
                        throw new JsonParseException("Error trying to parse NBTTag! Rethrowing exception...", exc);
                    }
                }

                if (!part.has("elements")) {
                    throw new JsonParseException("Part contained no element!");
                }
                JsonElement partElement = part.get("elements");
                if (partElement.isJsonPrimitive() && partElement.getAsJsonPrimitive().isString()) {
                    String strDesc = partElement.getAsString();
                    BlockArray.BlockInformation descr = MachineLoader.VARIABLE_CONTEXT.get(strDesc);
                    if (descr == null) {
                        descr = new BlockArray.BlockInformation(Lists.newArrayList(BlockArray.BlockInformation.getDescriptor(partElement.getAsString())));
                    } else {
                        descr = descr.copy(); //Avoid NBT-definitions bleed into variable context
                    }
                    if (match != null) {
                        descr.setMatchingTag(match);
                    }
                    addDescriptorWithPattern(machine.getPattern(), descr, part);
                } else if (partElement.isJsonArray()) {
                    JsonArray elementArray = partElement.getAsJsonArray();
                    List<IBlockStateDescriptor> descriptors = Lists.newArrayList();
                    for (int xx = 0; xx < elementArray.size(); xx++) {
                        JsonElement p = elementArray.get(xx);
                        if (!p.isJsonPrimitive() || !p.getAsJsonPrimitive().isString()) {
                            throw new JsonParseException("Part elements of 'elements' have to be blockstate descriptions!");
                        }
                        String prim = p.getAsString();
                        BlockArray.BlockInformation descr = MachineLoader.VARIABLE_CONTEXT.get(prim);
                        if (descr != null) {
                            descriptors.addAll(descr.copy().matchingStates);
                        } else {
                            descriptors.add(BlockArray.BlockInformation.getDescriptor(prim));
                        }
                    }
                    if (descriptors.isEmpty()) {
                        throw new JsonParseException("'elements' array didn't contain any blockstate descriptors!");
                    }
                    BlockArray.BlockInformation bi = new BlockArray.BlockInformation(descriptors);
                    if (match != null) {
                        bi.setMatchingTag(match);
                    }
                    addDescriptorWithPattern(machine.getPattern(), bi, part);
                } else {
                    throw new JsonParseException("'elements' has to either be a blockstate description, variable or array of blockstate descriptions!");
                }
            }

            //Modifiers
            if (root.has("modifiers")) {
                JsonElement partModifiers = root.get("modifiers");
                if (!partModifiers.isJsonArray()) {
                    throw new JsonParseException("'modifiers' has to be an array of modifiers!");
                }
                JsonArray modifiersArray = partModifiers.getAsJsonArray();
                for (int j = 0; j < modifiersArray.size(); j++) {
                    JsonElement modifier = modifiersArray.get(j);
                    if (!modifier.isJsonObject()) {
                        throw new JsonParseException("Elements of 'modifiers' have to be objects!");
                    }
                    addModifierWithPattern(machine, context.deserialize(modifier.getAsJsonObject(), ModifierReplacement.class), modifier.getAsJsonObject());
                }
            }
            return machine;
        }
    }

}
