package kport.modularmagic.common.integration.jei.helper;

import hellfirepvp.modularmachinery.ModularMachinery;
import kport.modularmagic.common.integration.jei.ingredient.Impetus;
import mezz.jei.api.ingredients.IIngredientHelper;

import javax.annotation.Nullable;

public class ImpetusHelper<T extends Impetus> implements IIngredientHelper<T> {

    @Nullable
    @Override
    public T getMatch(Iterable<T> ingredients, T ingredientToMatch) {
        return ingredients.iterator().next();
    }

    @Override
    public String getDisplayName(Impetus ingredient) {
        return "Impetus";
    }

    @Override
    public String getUniqueId(Impetus ingredient) {
        return "impetus";
    }

    @Override
    public String getWildcardId(Impetus ingredient) {
        return "impetus";
    }

    @Override
    public String getModId(Impetus ingredient) {
        return ModularMachinery.MODID;
    }

    @Override
    public String getResourceId(Impetus ingredient) {
        return "impetus";
    }

    @Override
    public Impetus copyIngredient(Impetus ingredient) {
        return ingredient;
    }

    @Override
    public String getErrorInfo(@Nullable Impetus ingredient) {
        return null;
    }
}
