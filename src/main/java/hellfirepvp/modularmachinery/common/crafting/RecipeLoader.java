/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.crafting;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.crafting.adapter.RecipeAdapterAccessor;
import hellfirepvp.modularmachinery.common.crafting.command.RecipeRunnableCommand;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import net.minecraft.util.JsonUtils;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: RecipeLoader
 * Created by HellFirePvP
 * Date: 27.06.2017 / 23:23
 */
public class RecipeLoader {

    public static final List<RecipeAdapterAccessor> recipeAdapters = new LinkedList<>();
    private static final Gson GSON = new GsonBuilder()
            .registerTypeHierarchyAdapter(MachineRecipe.MachineRecipeContainer.class, new MachineRecipe.Deserializer())
            .registerTypeHierarchyAdapter(ComponentRequirement.class, new MachineRecipe.ComponentDeserializer())
            .registerTypeHierarchyAdapter(RecipeAdapterAccessor.class, new RecipeAdapterAccessor.Deserializer())
            .registerTypeHierarchyAdapter(RecipeModifier.class, new RecipeModifier.Deserializer())
            .registerTypeHierarchyAdapter(RecipeRunnableCommand.class, new RecipeRunnableCommand.Deserializer())
            .create();
    public static String currentlyReadingPath = null;
    private static Map<String, Exception> failedAttempts = new HashMap<>();

    public static Map<FileType, List<File>> discoverDirectory(File directory) {
        Map<FileType, List<File>> candidates = new EnumMap<>(FileType.class);
        for (FileType type : FileType.values()) {
            candidates.put(type, Lists.newLinkedList());
        }
        LinkedList<File> directories = Lists.newLinkedList();
        directories.add(directory);
        while (!directories.isEmpty()) {
            File dir = directories.remove(0);
            for (File f : dir.listFiles()) {
                if (f.isDirectory()) {
                    directories.addLast(f);
                } else {
                    if (FileType.ADAPTER.accepts(f.getName())) {
                        candidates.get(FileType.ADAPTER).add(f);
                    } else if (FileType.RECIPE.accepts(f.getName())) {
                        candidates.get(FileType.RECIPE).add(f);
                    }
                }
            }
        }
        return candidates;
    }

    public static List<MachineRecipe> loadRecipes(List<File> recipeCandidates, List<PreparedRecipe> preparedRecipes) {
        recipeAdapters.clear();

        List<MachineRecipe> loadedRecipes = Lists.newArrayList();
        for (File f : recipeCandidates) {
            currentlyReadingPath = f.getPath();
            try (InputStreamReader isr = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                MachineRecipe.MachineRecipeContainer container = JsonUtils.fromJson(GSON, isr, MachineRecipe.MachineRecipeContainer.class);
                loadedRecipes.addAll(container.getRecipes());
            } catch (Exception exc) {
                failedAttempts.put(f.getPath(), exc);
            } finally {
                currentlyReadingPath = null;
            }
        }
        for (PreparedRecipe recipe : preparedRecipes) {
            recipe.loadNeedAfterInitActions();
            loadedRecipes.add(convertPreparedRecipe(recipe));
        }
        return loadedRecipes;
    }

    public static List<MachineRecipe> loadAdapterRecipes(List<File> adapterCandidates) {
        List<MachineRecipe> loadedRecipes = Lists.newArrayList();
        for (File f : adapterCandidates) {
            try (InputStreamReader isr = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                RecipeAdapterAccessor accessor = JsonUtils.fromJson(GSON, isr, RecipeAdapterAccessor.class);
                Collection<MachineRecipe> recipes = accessor.loadRecipesForAdapter();
                if (recipes.isEmpty()) {
                    ModularMachinery.log.warn("Adapter with name " + accessor.getAdapterKey().toString() + " didn't provide have any recipes!");
                } else {
                    loadedRecipes.addAll(recipes);
                }
                recipeAdapters.add(accessor);
            } catch (Exception exc) {
                failedAttempts.put(f.getPath(), exc);
            }
        }
        return loadedRecipes;
    }

    private static MachineRecipe convertPreparedRecipe(PreparedRecipe recipe) {
        MachineRecipe mr = new MachineRecipe(recipe.getFilePath(),
                recipe.getRecipeRegistryName(), recipe.getAssociatedMachineName(),
                recipe.getTotalProcessingTickTime(), recipe.getPriority(), recipe.voidPerTickFailure(),
                recipe.isParallelized(), recipe.getRecipeEventHandlers(), recipe.getTooltipList());
        recipe.getComponents().forEach(mr::addRequirement);
        return mr;
    }

    public static Map<String, Exception> captureFailedAttempts() {
        Map<String, Exception> failed = failedAttempts;
        failedAttempts = new HashMap<>();
        return failed;
    }

    public enum FileType {

        ADAPTER,
        RECIPE;

        public boolean accepts(String fileName) {
            switch (this) {
                case ADAPTER:
                    return fileName.endsWith(".adapter.json");
                case RECIPE:
                default:
                    return fileName.endsWith(".json");
            }
        }

    }

}
