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
import github.kasuminova.mmce.common.concurrent.Sync;
import github.kasuminova.mmce.common.event.Phase;
import github.kasuminova.mmce.common.event.recipe.ResultChanceCreateEvent;
import hellfirepvp.modularmachinery.common.crafting.ActiveMachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.MachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.command.ControllerCommandSender;
import hellfirepvp.modularmachinery.common.crafting.requirement.type.RequirementType;
import hellfirepvp.modularmachinery.common.lib.RequirementTypesMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import hellfirepvp.modularmachinery.common.modifier.SingleBlockModifierReplacement;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import hellfirepvp.modularmachinery.common.util.Asyncable;
import hellfirepvp.modularmachinery.common.util.PriorityProvider;
import hellfirepvp.modularmachinery.common.util.ResultChance;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
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
    private final TileMultiblockMachineController controller;
    private final ControllerCommandSender commandSender;
    private final List<ProcessingComponent<?>> typeComponents = new LinkedList<>();
    private final Map<RequirementType<?, ?>, List<RecipeModifier>> modifiers = new HashMap<>();
    private final List<RecipeModifier> permanentModifierList = new ArrayList<>();
    private final List<ComponentOutputRestrictor> currentRestrictions = new ArrayList<>();
    private final List<ComponentRequirement<?, ?>> requirements;
    private final Map<ComponentRequirement<?, ?>, Iterable<ProcessingComponent<?>>> requirementComponents = new HashMap<>();

    public RecipeCraftingContext(ActiveMachineRecipe activeRecipe, TileMultiblockMachineController controller) {
        this.activeRecipe = activeRecipe;
        this.controller = controller;
        this.commandSender = new ControllerCommandSender(this.controller);
        this.requirements = new ArrayList<>((int) (getParentRecipe().getCraftingRequirements().size() * 1.5));

        for (ComponentRequirement<?, ?> craftingRequirement : getParentRecipe().getCraftingRequirements()) {
            this.requirements.add(craftingRequirement.deepCopy());
        }
    }

    public TileMultiblockMachineController getMachineController() {
        return controller;
    }

    public MachineRecipe getParentRecipe() {
        return activeRecipe.getRecipe();
    }

    public ActiveMachineRecipe getActiveRecipe() {
        return activeRecipe;
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

        //Input / Output tick
        for (ComponentRequirement<?, ?> requirement : requirements) {
            if (!(requirement instanceof ComponentRequirement.PerTick)) continue;
            ComponentRequirement.PerTick<?, ?> perTickRequirement = (ComponentRequirement.PerTick<?, ?>) requirement;

            perTickRequirement.resetIOTick(this);
            perTickRequirement.startIOTick(this, durMultiplier);

            for (ProcessingComponent<?> component : requirementComponents.getOrDefault(requirement, Collections.emptyList())) {
                AtomicReference<CraftCheck> result = new AtomicReference<>();
                if (perTickRequirement instanceof Asyncable) {
                    result.set(perTickRequirement.doIOTick(component, this));
                } else {
                    Sync.doSyncAction(() -> result.set(perTickRequirement.doIOTick(component, this)));
                }
                if (result.get().isSuccess()) {
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
        ResultChanceCreateEvent event = new ResultChanceCreateEvent(
                controller, this, new ResultChance(seed), Phase.START);
        event.postEvent();
        ResultChance chance = event.getResultChance();

        for (ComponentRequirement<?, ?> requirement : requirements) {
            requirement.startRequirementCheck(chance, this);

            for (ProcessingComponent<?> component : requirementComponents.getOrDefault(requirement, Collections.emptyList())) {
                AtomicBoolean success = new AtomicBoolean(false);
                if (requirement instanceof Asyncable) {
                    success.set(requirement.startCrafting(component, this, chance));
                } else {
                    Sync.doSyncAction(() -> success.set(requirement.startCrafting(component, this, chance)));
                }
                if (success.get()) {
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
        ResultChanceCreateEvent event = new ResultChanceCreateEvent(
                controller, this, new ResultChance(seed), Phase.END);
        event.postEvent();
        ResultChance chance = event.getResultChance();

        for (ComponentRequirement<?, ?> requirement : requirements) {
            requirement.startRequirementCheck(chance, this);

            for (ProcessingComponent<?> component : requirementComponents.getOrDefault(requirement, Collections.emptyList())) {
                AtomicReference<CraftCheck> check = new AtomicReference<>();
                if (requirement instanceof Asyncable) {
                    check.set(requirement.finishCrafting(component, this, chance));
                } else {
                    Sync.doSyncAction(() -> check.set(requirement.finishCrafting(component, this, chance)));
                }
                if (check.get().isSuccess()) {
                    requirement.endRequirementCheck();
                    break;
                }
            }
            requirement.endRequirementCheck();
        }

        this.getParentRecipe().getCommandContainer().runFinishCommands(this.commandSender);
    }

    public int getMaxParallelism() {
        int maxParallelism = this.activeRecipe.getMaxParallelism();
        List<ComponentRequirement<?, ?>> parallelizableList = requirements.stream()
                .filter(ComponentRequirement.Parallelizable.class::isInstance)
                .collect(Collectors.toList());

        int requirementMaxParallelism = maxParallelism;
        for (ComponentRequirement<?, ?> requirement : parallelizableList) {
            Iterable<ProcessingComponent<?>> components = requirementComponents.getOrDefault(requirement, Collections.emptyList());
            ComponentRequirement.Parallelizable parallelizable = (ComponentRequirement.Parallelizable) requirement;
            int componentMaxParallelism = 1;
            for (ProcessingComponent<?> component : components) {
                componentMaxParallelism = Math.max(componentMaxParallelism, parallelizable.maxParallelism(component, this, maxParallelism));
                if (componentMaxParallelism >= requirementMaxParallelism) {
                    break;
                }
            }

            requirementMaxParallelism = Math.min(requirementMaxParallelism, componentMaxParallelism);
        }

        return requirementMaxParallelism;
    }

    public void setParallelism(int parallelism) {
        List<ComponentRequirement<?, ?>> parallelizableList = requirements.stream()
                .filter(ComponentRequirement.Parallelizable.class::isInstance)
                .collect(Collectors.toList());

        for (ComponentRequirement<?, ?> requirement : parallelizableList) {
            ComponentRequirement.Parallelizable parallelizable = (ComponentRequirement.Parallelizable) requirement;
            parallelizable.setParallelism(parallelism);
        }

        activeRecipe.setParallelism(parallelism);
    }

    public CraftingCheckResult canStartCrafting() {
        permanentModifierList.clear();
        if (getParentRecipe().isParallelized() && activeRecipe.getMaxParallelism() > 1) {
            int parallelism = Math.max(1, getMaxParallelism());
            setParallelism(parallelism);
        }
        return this.canStartCrafting(req -> true);
    }

    public CraftingCheckResult canFinishCrafting() {
        return this.canStartCrafting(req -> req.actionType == IOType.OUTPUT);
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

            Iterable<ProcessingComponent<?>> components = requirementComponents.getOrDefault(requirement, Collections.emptyList());
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

    public void updateRequirementComponents() {
        requirementComponents.clear();
        requirements.forEach(req ->
                requirementComponents.put(req, getComponentsFor(req, req.tag)));
    }

    public void addModifier(SingleBlockModifierReplacement modifier) {
        List<RecipeModifier> modifiers = modifier.getModifiers();
        for (RecipeModifier mod : modifiers) {
            RequirementType<?, ?> target = mod.getTarget();
            if (target == null) {
                target = RequirementTypesMM.REQUIREMENT_DURATION;
            }
            this.modifiers.computeIfAbsent(target, t -> new LinkedList<>()).add(mod);
        }
    }

    public void addModifier(RecipeModifier modifier) {
        if (modifier != null) {
            this.modifiers.computeIfAbsent(modifier.getTarget(), t -> new LinkedList<>()).add(modifier);
        }
    }

    public void addModifier(Collection<RecipeModifier> modifiers) {
        for (RecipeModifier modifier : modifiers) {
            this.modifiers.computeIfAbsent(modifier.getTarget(), t -> new LinkedList<>()).add(modifier);
        }
    }

    public void addPermanentModifier(RecipeModifier modifier) {
        if (modifier != null) {
            this.permanentModifierList.add(modifier);
            addModifier(modifier);
        }
    }

    public void overrideModifier(Collection<RecipeModifier> modifiers) {
        this.modifiers.clear();
        for (RecipeModifier modifier : modifiers) {
            addModifier(modifier);
        }
        for (RecipeModifier modifier : permanentModifierList) {
            addModifier(modifier);
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

        public void overrideError(String unlocError) {
            this.unlocErrorMessagesMap.clear();
            addError(unlocError);
        }

        public float getValidity() {
            return validity;
        }

        private void setValidity(float validity) {
            this.validity = validity;
        }

        public List<String> getUnlocalizedErrorMessages() {
            List<Map.Entry<String, Integer>> toSort = new ArrayList<>(this.unlocErrorMessagesMap.entrySet());
            toSort.sort(Map.Entry.comparingByValue());
            List<String> list = new ArrayList<>();
            for (Map.Entry<String, Integer> stringIntegerEntry : toSort) {
                String key = stringIntegerEntry.getKey();
                list.add(key);
            }
            return list;
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
