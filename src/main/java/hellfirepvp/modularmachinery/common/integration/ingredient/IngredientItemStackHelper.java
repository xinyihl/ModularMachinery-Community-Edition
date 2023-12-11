package hellfirepvp.modularmachinery.common.integration.ingredient;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import hellfirepvp.modularmachinery.common.integration.ModIntegrationJEI;
import mezz.jei.Internal;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.startup.StackHelper;
import mezz.jei.util.ErrorUtil;
import mezz.jei.util.Log;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraftforge.oredict.OreDictionary;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class IngredientItemStackHelper implements IIngredientHelper<IngredientItemStack> {
    private final StackHelper stackHelper = Internal.getStackHelper();
    private IIngredientHelper<ItemStack> helper = null;

    // Rewritten quite a bit to accommodate special features...

    protected static boolean isAllNulls(Iterable<?> iterable) {
        for (Object element : iterable) {
            if (element != null) {
                return false;
            }
        }
        return true;
    }

    protected static void addSubtypesToList(List<IngredientItemStack> subtypesList, Iterable<?> stacks) {
        for (Object obj : stacks) {
            if (obj instanceof final IngredientItemStack ingredient) {
                ItemStack itemStack = ingredient.stack();
                if (!itemStack.isEmpty()) {
                    if (itemStack.getItemDamage() == OreDictionary.WILDCARD_VALUE) {
                        addSubtypesToList(subtypesList, ingredient);
                    } else {
                        subtypesList.add(ingredient);
                    }
                }
            } else if (obj instanceof Iterable) {
                addSubtypesToList(subtypesList, (Iterable<?>) obj);
            } else if (obj != null) {
                Log.get().error("Unknown object found: {}", obj);
            } else {
                subtypesList.add(null);
            }
        }
    }

    protected static void addSubtypesToList(final List<IngredientItemStack> subtypeList, IngredientItemStack ingredient) {
        ItemStack itemStack = ingredient.stack();
        final Item item = itemStack.getItem();
        for (CreativeTabs itemTab : item.getCreativeTabs()) {
            if (itemTab == null) {
                IngredientItemStack copy = ingredient.copy();
                copy.stack().setItemDamage(0);
                subtypeList.add(copy);
            } else {
                addSubtypesFromCreativeTabToList(subtypeList, ingredient, itemTab);
            }
        }
    }

    protected static void addSubtypesFromCreativeTabToList(List<IngredientItemStack> subtypeList, IngredientItemStack ingredient, CreativeTabs itemTab) {
        NonNullList<ItemStack> subItems = NonNullList.create();
        Item item = ingredient.stack().getItem();
        try {
            item.getSubItems(itemTab, subItems);
        } catch (RuntimeException | LinkageError e) {
            Log.get().warn("Caught a crash while getting sub-items of {}", item, e);
        }

        for (ItemStack subItem : subItems) {
            if (subItem.isEmpty()) {
                Log.get().warn("Found an empty subItem of {}", item);
            } else if (subItem.getMetadata() == OreDictionary.WILDCARD_VALUE) {
                String itemStackInfo = ErrorUtil.getItemStackInfo(subItem);
                Log.get().error("Found an subItem of {} with wildcard metadata: {}", item, itemStackInfo);
            } else {
                subtypeList.add(new IngredientItemStack(subItem, ingredient.min(), ingredient.max(), ingredient.chance()));
            }
        }
    }

    public static List<IngredientItemStack> getAllSubtypes(@Nullable Iterable<?> stacks) {
        if (stacks == null) {
            return Collections.emptyList();
        }

        List<IngredientItemStack> allSubtypes = new ArrayList<>();
        addSubtypesToList(allSubtypes, stacks);

        if (isAllNulls(allSubtypes)) {
            return Collections.emptyList();
        }

        return allSubtypes;
    }

    @Nonnull
    @Override
    public List<IngredientItemStack> expandSubtypes(@Nonnull List<IngredientItemStack> contained) {
        return getAllSubtypes(contained);
    }

    public IIngredientHelper<ItemStack> getIngredientHelper() {
        if (helper == null) {
            helper = ModIntegrationJEI.ingredientRegistry.getIngredientHelper(VanillaTypes.ITEM);
        }
        return helper;
    }

    @Nonnull
    @Override
    public IFocus<?> translateFocus(IFocus<IngredientItemStack> focus, @Nonnull IFocusFactory focusFactory) {
        return getIngredientHelper().translateFocus(focusFactory.createFocus(focus.getMode(), focus.getValue().stack()), focusFactory);
    }

    @Override
    @Nullable
    public IngredientItemStack getMatch(@Nonnull Iterable<IngredientItemStack> ingredients, IngredientItemStack toMatch) {
        ItemStack matched = stackHelper.containsStack(Iterables.transform(ingredients, new Function<>() {
            @Nullable
            @Override
            public ItemStack apply(@Nullable final IngredientItemStack input) {
                return input == null ? null : input.stack();
            }
        }), toMatch.stack());

        if (matched == null) {
            return null;
        }

        return new IngredientItemStack(matched, matched.getCount(), matched.getCount(), 1F);
    }

    @Nonnull
    @Override
    public String getDisplayName(IngredientItemStack ingredient) {
        return ErrorUtil.checkNotNull(ingredient.stack().getDisplayName(), "itemStack.getDisplayName()");
    }

    @Nonnull
    @Override
    public String getUniqueId(IngredientItemStack ingredient) {
        return getIngredientHelper().getUniqueId(ingredient.stack());
    }

    @Nonnull
    @Override
    public String getWildcardId(IngredientItemStack ingredient) {
        return getIngredientHelper().getWildcardId(ingredient.stack());
    }

    @Nonnull
    @Override
    public String getModId(IngredientItemStack ingredient) {
        return getIngredientHelper().getModId(ingredient.stack());
    }

    @Nonnull
    @Override
    public String getDisplayModId(IngredientItemStack ingredient) {
        return getIngredientHelper().getDisplayModId(ingredient.stack());
    }

    @Nonnull
    @Override
    public Iterable<Color> getColors(IngredientItemStack ingredient) {
        return getIngredientHelper().getColors(ingredient.stack());
    }

    @Nonnull
    @Override
    public String getResourceId(IngredientItemStack ingredient) {
        return getIngredientHelper().getResourceId(ingredient.stack());
    }

    @Override
    public int getOrdinal(IngredientItemStack ingredient) {
        return getIngredientHelper().getOrdinal(ingredient.stack());
    }

    @Nonnull
    @Override
    public ItemStack getCheatItemStack(IngredientItemStack ingredient) {
        return getIngredientHelper().getCheatItemStack(ingredient.stack());
    }

    @Nonnull
    @Override
    public IngredientItemStack copyIngredient(IngredientItemStack ingredient) {
        return ingredient.copy();
    }

    @Override
    public boolean isValidIngredient(IngredientItemStack ingredient) {
        return !ingredient.stack().isEmpty();
    }

    @Override
    public boolean isIngredientOnServer(IngredientItemStack ingredient) {
        return getIngredientHelper().isIngredientOnServer(ingredient.stack());
    }

    @Nonnull
    @Override
    public Collection<String> getOreDictNames(IngredientItemStack ingredient) {
        return getIngredientHelper().getOreDictNames(ingredient.stack());
    }

    @Nonnull
    @Override
    public Collection<String> getCreativeTabNames(IngredientItemStack ingredient) {
        return getIngredientHelper().getCreativeTabNames(ingredient.stack());
    }

    @Nonnull
    @Override
    public String getErrorInfo(@Nullable IngredientItemStack ingredient) {
        if (ingredient == null) {
            return "null";
        }
        return getIngredientHelper().getErrorInfo(ingredient.stack());
    }
}
