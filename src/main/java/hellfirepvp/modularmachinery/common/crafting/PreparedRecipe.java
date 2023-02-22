/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.crafting;

import crafttweaker.util.IEventHandler;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.event.recipe.RecipeEvent;
import net.minecraft.util.ResourceLocation;

import java.util.List;
import java.util.Map;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: PreparedRecipe
 * Created by HellFirePvP
 * Date: 03.01.2018 / 15:56
 */
public interface PreparedRecipe {

    String getFilePath();

    ResourceLocation getRecipeRegistryName();

    ResourceLocation getAssociatedMachineName();

    int getTotalProcessingTickTime();

    int getPriority();

    default boolean voidPerTickFailure() {
        return false;
    }

    List<ComponentRequirement<?, ?>> getComponents();

    Map<Class<?>, List<IEventHandler<RecipeEvent>>> getRecipeEventHandlers();

    List<String> getTooltipList();
}
