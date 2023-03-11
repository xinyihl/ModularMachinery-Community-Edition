/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.integration.recipe;

import com.google.common.collect.Lists;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.crafting.MachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.RecipeRegistry;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.crafting.tooltip.RequirementTip;
import hellfirepvp.modularmachinery.common.integration.ModIntegrationJEI;
import hellfirepvp.modularmachinery.common.lib.RegistriesMM;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.machine.IOType;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IGuiIngredientGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: CategoryDynamicRecipe
 * Created by HellFirePvP
 * Date: 11.07.2017 / 16:58
 */
public class CategoryDynamicRecipe implements IRecipeCategory<DynamicRecipeWrapper> {

    final int realHeight;
    final LinkedList<RecipeLayoutPart<?>> inputComponents = Lists.newLinkedList();
    final LinkedList<RecipeLayoutPart<?>> outputComponents = Lists.newLinkedList();
    private final DynamicMachine machine;
    private final String category;
    private final String title;
    private final IDrawable sizeEmptyDrawable;
    Rectangle rectangleProcessArrow;
    private Point offsetProcessArrow;

    public CategoryDynamicRecipe(DynamicMachine machine) {
        this.machine = machine;
        this.category = ModIntegrationJEI.getCategoryStringFor(machine);
        this.title = machine.getLocalizedName();

        Point maxPoint = buildRecipeComponents();
        this.realHeight = maxPoint.y;
        this.sizeEmptyDrawable = ModIntegrationJEI.jeiHelpers.getGuiHelper().createBlankDrawable(maxPoint.x, this.realHeight);
    }

    private Point buildRecipeComponents() {
        Iterable<MachineRecipe> recipes = RecipeRegistry.getRecipesFor(this.machine);
        Map<IOType, Map<Class<?>, Integer>> componentCounts = new EnumMap<>(IOType.class);
        Map<Class<?>, ComponentRequirement.JEIComponent<?>> componentsFound = new HashMap<>();
        FontRenderer fr = Minecraft.getMinecraft().fontRenderer;
        int offsetX = 8;
        int offsetY = 0;
        int highestY = 0;
        int longestTooltip = 0;
        int widestTooltip = 0;

        for (MachineRecipe recipe : recipes) {
            Map<IOType, Map<Class<?>, Integer>> tempComp = new EnumMap<>(IOType.class);
            for (ComponentRequirement<?, ?> req : recipe.getCraftingRequirements()) {
                ComponentRequirement.JEIComponent<?> jeiComp = req.provideJEIComponent();
                if (jeiComp == null) {
                    continue;
                }
                int amt = tempComp.computeIfAbsent(req.getActionType(), ioType -> new HashMap<>())
                        .computeIfAbsent(jeiComp.getJEIRequirementClass(), clazz -> 0);
                amt++;
                tempComp.get(req.getActionType()).put(jeiComp.getJEIRequirementClass(), amt);


                if (!componentsFound.containsKey(jeiComp.getJEIRequirementClass())) {
                    componentsFound.put(jeiComp.getJEIRequirementClass(), jeiComp);
                }
            }
            for (Map.Entry<IOType, Map<Class<?>, Integer>> cmpEntry : tempComp.entrySet()) {
                for (Map.Entry<Class<?>, Integer> cntEntry : cmpEntry.getValue().entrySet()) {
                    int current = componentCounts.computeIfAbsent(cmpEntry.getKey(), ioType -> new HashMap<>())
                            .computeIfAbsent(cntEntry.getKey(), clazz -> 0);
                    if (cntEntry.getValue() > current) {
                        componentCounts.get(cmpEntry.getKey()).put(cntEntry.getKey(), cntEntry.getValue());
                    }
                }
            }

            int tipLength = 0;
            //Custom TooltipList
            tipLength += RequirementTip.LINE_HEIGHT * recipe.getTooltipList().size();
            for (RequirementTip tip : RegistriesMM.REQUIREMENT_TIPS_REGISTRY) {
                Collection<ComponentRequirement<?, ?>> requirements = tip.filterRequirements(recipe, recipe.getCraftingRequirements());
                if (!requirements.isEmpty()) {
                    List<String> tooltip = tip.buildTooltip(recipe, requirements);
                    if (!tooltip.isEmpty()) {
                        for (String tipString : tooltip) {
                            int length = fr.getStringWidth(tipString);
                            if (length > widestTooltip) {
                                widestTooltip = length;
                            }
                        }
                        tipLength += RequirementTip.LINE_HEIGHT * tooltip.size();
                        tipLength += RequirementTip.SPLIT_HEIGHT;
                    }
                }
            }
            if (tipLength > longestTooltip) {
                longestTooltip = tipLength;
            }
        }

        List<Class<?>> classes = new LinkedList<>(componentsFound.keySet());
        classes.sort((o1, o2) -> {
            RecipeLayoutPart<?> part1 = componentsFound.get(o1).getTemplateLayout();
            RecipeLayoutPart<?> part2 = componentsFound.get(o2).getTemplateLayout();
            return part2.getComponentHorizontalSortingOrder() - part1.getComponentHorizontalSortingOrder();
        });

        for (Class<?> clazz : classes) {
            Map<Class<?>, Integer> compMap = componentCounts.get(IOType.INPUT);
            if (compMap != null && compMap.containsKey(clazz)) {
                ComponentRequirement.JEIComponent<?> component = componentsFound.get(clazz);
                RecipeLayoutPart<?> layoutHelper = component.getTemplateLayout();
                int amt = compMap.get(clazz);

                int partOffsetX = offsetX;
                int originalOffsetX = offsetX;
                int partOffsetY = offsetY;
                for (int i = 0; i < amt; i++) {
                    if (i > 0 && i % layoutHelper.getMaxHorizontalCount() == 0) {
                        partOffsetY += layoutHelper.getComponentHeight() + layoutHelper.getComponentVerticalGap();
                        partOffsetX = originalOffsetX;
                    }
                    inputComponents.add(component.getLayoutPart(new Point(partOffsetX, partOffsetY)));
                    partOffsetX += layoutHelper.getComponentWidth() + layoutHelper.getComponentHorizontalGap();
                    if (partOffsetX > offsetX) {
                        offsetX = partOffsetX;
                    }
                    if (partOffsetY + layoutHelper.getComponentHeight() > highestY) {
                        highestY = partOffsetY + layoutHelper.getComponentHeight();
                    }
                }
            }
        }

        offsetX += 4;
        int tempArrowOffsetX = offsetX;
        offsetX += RecipeLayoutHelper.PART_PROCESS_ARROW.xSize;
        offsetX += 4;

        classes = new LinkedList<>(componentsFound.keySet());
        classes.sort((o1, o2) -> {
            RecipeLayoutPart<?> part1 = componentsFound.get(o1).getTemplateLayout();
            RecipeLayoutPart<?> part2 = componentsFound.get(o2).getTemplateLayout();
            return part1.getComponentHorizontalSortingOrder() - part2.getComponentHorizontalSortingOrder();
        });

        for (Class<?> clazz : classes) {
            Map<Class<?>, Integer> compMap = componentCounts.get(IOType.OUTPUT);
            if (compMap != null && compMap.containsKey(clazz)) {
                ComponentRequirement.JEIComponent<?> component = componentsFound.get(clazz);
                RecipeLayoutPart<?> layoutHelper = component.getTemplateLayout();
                int amt = compMap.get(clazz);

                int partOffsetX = offsetX;
                int originalOffsetX = offsetX;
                int partOffsetY = offsetY;
                for (int i = 0; i < amt; i++) {
                    if (i > 0 && i % layoutHelper.getMaxHorizontalCount() == 0) {
                        partOffsetY += layoutHelper.getComponentHeight() + layoutHelper.getComponentVerticalGap();
                        partOffsetX = originalOffsetX;
                    }
                    outputComponents.add(component.getLayoutPart(new Point(partOffsetX, partOffsetY)));
                    partOffsetX += layoutHelper.getComponentWidth() + layoutHelper.getComponentHorizontalGap();
                    if (partOffsetX > offsetX) {
                        offsetX = partOffsetX;
                    }
                    if (partOffsetY + layoutHelper.getComponentHeight() > highestY) {
                        highestY = partOffsetY + layoutHelper.getComponentHeight();
                    }
                }
            }
        }


        int halfY = highestY / 2;
        offsetProcessArrow = new Point(tempArrowOffsetX, halfY / 2);
        rectangleProcessArrow = new Rectangle(offsetProcessArrow.x, offsetProcessArrow.y,
                RecipeLayoutHelper.PART_PROCESS_ARROW.xSize, RecipeLayoutHelper.PART_PROCESS_ARROW.zSize);

        //Texts for input consumed/produced
        highestY += longestTooltip;

        widestTooltip += 8; //Initial offset
        if (widestTooltip > offsetX) {
            offsetX = widestTooltip;
        }

        return new Point(offsetX, highestY);
    }

    @Nonnull
    @Override
    public String getUid() {
        return this.category;
    }

    @Nonnull
    @Override
    public String getTitle() {
        return this.title;
    }

    @Nonnull
    @Override
    public String getModName() {
        return ModularMachinery.NAME;
    }

    @Nonnull
    @Override
    public IDrawable getBackground() {
        return this.sizeEmptyDrawable;
    }

    @Override
    public void drawExtras(@Nonnull Minecraft minecraft) {
        RecipeLayoutHelper.PART_PROCESS_ARROW.drawable.draw(minecraft, offsetProcessArrow.x, offsetProcessArrow.y);

        inputComponents.forEach(slot -> slot.drawBackground(minecraft));
        outputComponents.forEach(slot -> slot.drawBackground(minecraft));
    }

    @Override
    public void setRecipe(@Nonnull IRecipeLayout recipeLayout, @Nonnull DynamicRecipeWrapper recipeWrapper, @Nonnull IIngredients ingredients) {
        List<Class<?>> foundClasses = new LinkedList<>();

        for (IOType type : IOType.values()) {
            for (Class<?> clazz : recipeWrapper.finalOrderedComponents.get(type).keySet()) {
                if (clazz.equals(Long.class)) { //Nope nope nope, fck you, Energy-component.
                    continue;
                }
                if (!foundClasses.contains(clazz)) {
                    foundClasses.add(clazz);
                }
            }
        }

        foundClasses.forEach(clazz -> {
            int amtCompInputs = 0;
            IGuiIngredientGroup<?> clazzGroup = recipeLayout.getIngredientsGroup(clazz);
            int compSlotIndex = 0;

            for (RecipeLayoutPart slot : this.inputComponents
                    .stream()
                    .filter(c -> clazz.isAssignableFrom(c.getLayoutTypeClass()))
                    .collect(Collectors.toList())) {
                clazzGroup.init(
                        compSlotIndex,
                        true,
                        slot.provideIngredientRenderer(),
                        slot.getOffset().x,
                        slot.getOffset().y,
                        slot.getComponentWidth(),
                        slot.getComponentHeight(),
                        slot.getRendererPaddingX(),
                        slot.getRendererPaddingY());
                compSlotIndex++;
                amtCompInputs++;
            }

            for (RecipeLayoutPart slot : this.outputComponents
                    .stream()
                    .filter(c -> clazz.isAssignableFrom(c.getLayoutTypeClass()))
                    .collect(Collectors.toList())) {
                clazzGroup.init(
                        compSlotIndex,
                        false,
                        slot.provideIngredientRenderer(),
                        slot.getOffset().x,
                        slot.getOffset().y,
                        slot.getComponentWidth(),
                        slot.getComponentHeight(),
                        slot.getRendererPaddingX(),
                        slot.getRendererPaddingY());
                compSlotIndex++;
            }

            clazzGroup.set(ingredients);
            int finalAmtInputs = amtCompInputs;

            clazzGroup.addTooltipCallback((slotIndex, input, ingredient, tooltip) -> {
                Map<Class<?>, List<ComponentRequirement<?, ?>>> components = recipeWrapper.finalOrderedComponents
                        .get(input ? IOType.INPUT : IOType.OUTPUT);
                if (components != null) {
                    List<ComponentRequirement<?, ?>> compList = components.get(clazz);

                    int index = input ? slotIndex : slotIndex - finalAmtInputs;
                    if (index < 0 || index >= compList.size()) {
                        return;
                    }
                    ComponentRequirement.JEIComponent jeiComp = compList.get(index).provideJEIComponent();
                    if (jeiComp == null) {
                        return;
                    }
                    jeiComp.onJEIHoverTooltip(index, input, ingredient, tooltip);
                }
            });
        });
    }
}
