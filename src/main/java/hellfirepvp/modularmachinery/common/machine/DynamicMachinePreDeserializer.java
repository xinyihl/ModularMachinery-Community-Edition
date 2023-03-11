package hellfirepvp.modularmachinery.common.machine;

import com.google.gson.*;
import net.minecraft.util.JsonUtils;

import java.lang.reflect.Type;

public class DynamicMachinePreDeserializer implements JsonDeserializer<DynamicMachine> {
    @Override
    public DynamicMachine deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject root = json.getAsJsonObject();

        String registryName = getRegistryName(root);
        String localized = getLocalizedName(root);

        DynamicMachine machine = new DynamicMachine(registryName);
        machine.setLocalizedName(localized);

        //Failure Action
        if (root.has("failure-action")) {
            machine.setFailureAction(getFailureActions(root));
        }

        //Requires Blueprint
        if (root.has("requires-blueprint")) {
            machine.setRequiresBlueprint(getRequireBlueprint(root));
        }

        //Color
        if (root.has("color")) {
            machine.setDefinedColor(getColor(root));
        }

        return machine;
    }

    private static String getRegistryName(JsonObject root) throws JsonParseException {
        String registryName = JsonUtils.getString(root, "registryname", "");
        if (registryName.isEmpty()) {
            registryName = JsonUtils.getString(root, "registryName", "");
            if (registryName.isEmpty()) {
                throw new JsonParseException("Invalid/Missing 'registryname' !");
            }
        }
        return registryName;
    }

    private static String getLocalizedName(JsonObject root) throws JsonParseException {
        String localized = JsonUtils.getString(root, "localizedname", "");

        if (localized.isEmpty()) {
            throw new JsonParseException("Invalid/Missing 'localizedname' !");
        }

        return localized;
    }

    private static RecipeFailureActions getFailureActions(JsonObject root) throws JsonParseException {
        JsonElement failureAction = root.get("failure-action");
        if (!failureAction.isJsonPrimitive() || !failureAction.getAsJsonPrimitive().isString()) {
            throw new JsonParseException("'failure-action' has to be 'reset', 'still' or 'decrease'!");
        }
        String action = failureAction.getAsJsonPrimitive().getAsString();
        return RecipeFailureActions.getFailureAction(action);
    }

    private static boolean getRequireBlueprint(JsonObject root) throws JsonParseException {
        JsonElement elementBlueprint = root.get("requires-blueprint");
        if (!elementBlueprint.isJsonPrimitive() || !elementBlueprint.getAsJsonPrimitive().isBoolean()) {
            throw new JsonParseException("'requires-blueprint' has to be either 'true' or 'false'!");
        }
        return elementBlueprint.getAsJsonPrimitive().getAsBoolean();
    }

    private static int getColor(JsonObject root) throws JsonParseException {
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
        return hexColor;
    }
}
