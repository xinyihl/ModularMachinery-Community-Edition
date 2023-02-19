/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.crafting.helper;

import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import hellfirepvp.modularmachinery.common.crafting.ActiveMachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.MachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.command.ControllerCommandSender;
import hellfirepvp.modularmachinery.common.crafting.requirement.type.RequirementType;
import hellfirepvp.modularmachinery.common.lib.RequirementTypesMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.modifier.ModifierReplacement;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import hellfirepvp.modularmachinery.common.tiles.TileMachineController;
import hellfirepvp.modularmachinery.common.util.PriorityProvider;
import hellfirepvp.modularmachinery.common.util.ResultChance;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: RecipeCraftingContext
 * Created by HellFirePvP
 * Date: 28.06.2017 / 12:23
 */
public class RecipeCraftingContext {
    private static final Random RAND = new Random();

    private final ActiveMachineRecipe activeRecipe;
    private final TileMachineController machineController;
    private final ControllerCommandSender commandSender;
    private final List<ProcessingComponent<?>> typeComponents = new LinkedList<>();
    private final Map<RequirementType<?, ?>, List<RecipeModifier>> modifiers = new HashMap<>();
    private final List<ComponentOutputRestrictor> currentRestrictions = new ArrayList<>();
    private final List<ComponentRequirement<?, ?>> requirements;
    private int currentCraftingTick = 0;

    public RecipeCraftingContext(ActiveMachineRecipe activeRecipe, TileMachineController controller) {
        this.activeRecipe = activeRecipe;
        this.machineController = controller;
        this.commandSender = new ControllerCommandSender(machineController);
        this.requirements = new ArrayList<>((int) (getParentRecipe().getCraftingRequirements().size() * 1.5));

        for (ComponentRequirement<?, ?> craftingRequirement : getParentRecipe().getCraftingRequirements()) {
            this.requirements.add(craftingRequirement.deepCopy());
        }
    }

    public TileMachineController getMachineController() {
        return machineController;
    }

    public MachineRecipe getParentRecipe() {
        return activeRecipe.getRecipe();
    }

    public ActiveMachineRecipe getActiveRecipe() {
        return activeRecipe;
    }

    public int getCurrentCraftingTick() {
        return currentCraftingTick;
    }

    public void setCurrentCraftingTick(int currentCraftingTick) {
        this.currentCraftingTick = currentCraftingTick;
    }

    @Nonnull
    public List<RecipeModifier> getModifiers(RequirementType<?, ?> target) {
        return modifiers.computeIfAbsent(target, t -> new LinkedList<>());
    }

    public float getDurationMultiplier() {
        float dur = this.getParentRecipe().getRecipeTotalTickTime();
        float result = RecipeModifier.applyModifiers(getModifiers(RequirementTypesMM.REQUIREMENT_DURATION), RequirementTypesMM.REQUIREMENT_DURATION, null, dur, false);
        return dur / result;
    }

    public void addRestriction(ComponentOutputRestrictor restrictor) {
        this.currentRestrictions.add(restrictor);
    }

    public Iterable<ProcessingComponent<?>> getComponentsFor(ComponentRequirement<?, ?> requirement, @Nullable ComponentSelectorTag tag) {
        List<ProcessingComponent<?>> validComponents = new ArrayList<>();
        for (ProcessingComponent<?> typeComponent : this.typeComponents) {
            if (requirement.isValidComponent(typeComponent, this)) {
                validComponents.add(typeComponent);
            }
        }
        if (tag == null) {
            return Collections.unmodifiableList(validComponents);
        } else {
            return new PriorityProvider<>(validComponents, compList -> Iterators.tryFind(compList.iterator(), comp -> tag.equals(comp.tag)).orNull());
        }
    }

    public CraftingCheckResult ioTick(int currentTick) {
        float durMultiplier = this.getDurationMultiplier();

        //Input tick
        for (ComponentRequirement<?, ?> requirement : requirements) {
            if (!(requirement instanceof ComponentRequirement.PerTick) ||
                    requirement.actionType == IOType.OUTPUT) continue;
            ComponentRequirement.PerTick<?, ?> perTickRequirement = (ComponentRequirement.PerTick<?, ?>) requirement;

            perTickRequirement.resetIOTick(this);
            perTickRequirement.startIOTick(this, durMultiplier);

            for (ProcessingComponent<?> component : getComponentsFor(requirement, requirement.tag)) {
                CraftCheck result = perTickRequirement.doIOTick(component, this);
                if (result.isSuccess()) {
                    break;
                }
            }

            CraftCheck result = perTickRequirement.resetIOTick(this);
            if (!result.isSuccess()) {
                CraftingCheckResult res = new CraftingCheckResult();
                res.addError(result.getUnlocalizedMessage());
                return res;
            }
        }

        //Output tick
        for (ComponentRequirement<?, ?> requirement : requirements) {
            if (!(requirement instanceof ComponentRequirement.PerTick) ||
                    requirement.actionType == IOType.INPUT) continue;
            ComponentRequirement.PerTick<?, ?> perTickRequirement = (ComponentRequirement.PerTick<?, ?>) requirement;

            perTickRequirement.resetIOTick(this);
            perTickRequirement.startIOTick(this, durMultiplier);

            for (ProcessingComponent<?> component : getComponentsFor(requirement, requirement.tag)) {
                CraftCheck result = perTickRequirement.doIOTick(component, this);
                if (result.isSuccess()) {
                    break;
                }
            }

            CraftCheck result = perTickRequirement.resetIOTick(this);
            if (!result.isSuccess()) {
                CraftingCheckResult res = new CraftingCheckResult();
                res.addError(result.getUnlocalizedMessage());
                return res;
            }
        }

        this.getParentRecipe().getCommandContainer().runTickCommands(this.commandSender, currentTick);

        return CraftingCheckResult.SUCCESS;
    }

    public void startCrafting() {
        startCrafting(RAND.nextLong());
    }

    public void startCrafting(long seed) {
        ResultChance chance = new ResultChance(seed);
        for (ComponentRequirement<?, ?> requirement : requirements) {
            requirement.startRequirementCheck(chance, this);

            for (ProcessingComponent<?> component : getComponentsFor(requirement, requirement.tag)) {
                if (requirement.startCrafting(component, this, chance)) {
                    requirement.endRequirementCheck();
                    break;
                }
            }
            requirement.endRequirementCheck();
        }

        this.getParentRecipe().getCommandContainer().runStartCommands(this.commandSender);
    }

    public void finishCrafting() {
        finishCrafting(RAND.nextLong());
    }

    public void finishCrafting(long seed) {
        ResultChance chance = new ResultChance(seed);
        for (ComponentRequirement<?, ?> requirement : requirements) {
            requirement.startRequirementCheck(chance, this);

            for (ProcessingComponent<?> component : getComponentsFor(requirement, requirement.tag)) {
                CraftCheck check = requirement.finishCrafting(component, this, chance);
                if (check.isSuccess()) {
                    requirement.endRequirementCheck();
                    break;
                }
            }
            requirement.endRequirementCheck();
        }

        this.getParentRecipe().getCommandContainer().runFinishCommands(this.commandSender);
    }

    public CraftingCheckResult canStartCrafting() {
        return this.canStartCrafting(req -> true);
    }

    public CraftingCheckResult canStartCrafting(Predicate<ComponentRequirement<?, ?>> requirementFilter) {
        currentRestrictions.clear();
        CraftingCheckResult result = new CraftingCheckResult();
        float successfulRequirements = 0;
        List<ComponentRequirement<?, ?>> requirements = this.requirements.stream()
                .filter(requirementFilter)
                .collect(Collectors.toList());

        lblRequirements:
        for (ComponentRequirement<?, ?> requirement : requirements) {
            requirement.startRequirementCheck(ResultChance.GUARANTEED, this);

            Iterable<ProcessingComponent<?>> components = getComponentsFor(requirement, requirement.tag);
            if (!Iterables.isEmpty(components)) {

                List<String> errorMessages = new ArrayList<>();
                for (ProcessingComponent<?> component : components) {
                    CraftCheck check = requirement.canStartCrafting(component, this, this.currentRestrictions);

                    if (check.isSuccess()) {
                        requirement.endRequirementCheck();
                        successfulRequirements += 1;
                        continue lblRequirements;
                    }

                    if (!check.isInvalid() && !check.getUnlocalizedMessage().isEmpty()) {
                        errorMessages.add(check.getUnlocalizedMessage());
                    }
                }
                errorMessages.forEach(result::addError);
            } else {
                // No component found that would apply for the given requirement
                result.addError(requirement.getMissingComponentErrorMessage(requirement.actionType));
            }

            requirement.endRequirementCheck();
        }
        result.setValidity(successfulRequirements / requirements.size());

        currentRestrictions.clear();
        return result;
    }

    public <T> void addComponent(MachineComponent<T> component, @Nullable ComponentSelectorTag tag) {
        this.typeComponents.add(new ProcessingComponent<>(component, component.getContainerProvider(), tag));
    }

    public void addModifier(ModifierReplacement modifier) {
        List<RecipeModifier> modifiers = modifier.getModifiers();
        for (RecipeModifier mod : modifiers) {
            RequirementType<?, ?> target = mod.getTarget();
            if (target == null) {
                target = RequirementTypesMM.REQUIREMENT_DURATION;
            }
            this.modifiers.computeIfAbsent(target, t -> new LinkedList<>()).add(mod);
        }
    }

    public static class CraftingCheckResult {
        private static final CraftingCheckResult SUCCESS = new CraftingCheckResult();

        private final Map<String, Integer> unlocErrorMessagesMap = new HashMap<>();
        public float validity = 0F;

        public CraftingCheckResult() {
        }

        public void addError(String unlocError) {
            if (!unlocError.isEmpty()) {
                int count = this.unlocErrorMessagesMap.getOrDefault(unlocError, 0);
                count++;
                this.unlocErrorMessagesMap.put(unlocError, count);
            }
        }

        public float getValidity() {
            return validity;
        }

        private void setValidity(float validity) {
            this.validity = validity;
        }

        public List<String> getUnlocalizedErrorMessages() {
            return this.unlocErrorMessagesMap.entrySet()
                    .stream()
                    .sorted(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
        }

        public String getFirstErrorMessage(String defaultMessage) {
            List<String> unlocalizedErrorMessages = getUnlocalizedErrorMessages();
            return unlocalizedErrorMessages.isEmpty() ? defaultMessage : unlocalizedErrorMessages.get(0);
        }

        public boolean isFailure() {
            return !this.unlocErrorMessagesMap.isEmpty();
        }

        public boolean isSuccess() {
            return this.unlocErrorMessagesMap.isEmpty();
        }

    }

}
