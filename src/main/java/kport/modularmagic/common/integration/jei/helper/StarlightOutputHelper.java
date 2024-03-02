package kport.modularmagic.common.integration.jei.helper;

import hellfirepvp.modularmachinery.ModularMachinery;
import kport.modularmagic.common.integration.jei.ingredient.StarlightOutput;
import mezz.jei.api.ingredients.IIngredientHelper;

import javax.annotation.Nullable;

public class StarlightOutputHelper<T extends StarlightOutput> implements IIngredientHelper<StarlightOutput> {

    @Nullable
    @Override
    public StarlightOutput getMatch(Iterable<StarlightOutput> ingredients, StarlightOutput ingredientToMatch) {
        return ingredients.iterator().next();
    }

    @Override
    public String getDisplayName(StarlightOutput ingredient) {
        return "Starlight";
    }

    @Override
    public String getUniqueId(StarlightOutput ingredient) {
        return "starlight";
    }

    @Override
    public String getWildcardId(StarlightOutput ingredient) {
        return "starlight";
    }

    @Override
    public String getModId(StarlightOutput ingredient) {
        return ModularMachinery.MODID;
    }

    @Override
    public String getResourceId(StarlightOutput ingredient) {
        return "starlight";
    }

    @Override
    public StarlightOutput copyIngredient(StarlightOutput ingredient) {
        return ingredient;
    }

    @Override
    public String getErrorInfo(@Nullable StarlightOutput ingredient) {
        return null;
    }
}
