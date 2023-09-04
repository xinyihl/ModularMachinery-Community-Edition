/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.crafting.helper;

import github.kasuminova.mmce.common.concurrent.Sync;
import github.kasuminova.mmce.common.event.Phase;
import github.kasuminova.mmce.common.event.recipe.ResultChanceCreateEvent;
import hellfirepvp.modularmachinery.common.crafting.ActiveMachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.MachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.command.ControllerCommandSender;
import hellfirepvp.modularmachinery.common.crafting.requirement.type.RequirementType;
import hellfirepvp.modularmachinery.common.lib.RequirementTypesMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import hellfirepvp.modularmachinery.common.modifier.SingleBlockModifierReplacement;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import hellfirepvp.modularmachinery.common.util.Asyncable;
import hellfirepvp.modularmachinery.common.util.ResultChance;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;

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

    private final int reloadCounter;

    private final Map<RequirementType<?, ?>, List<RecipeModifier>> modifiers = new HashMap<>();
    private final Map<RequirementType<?, ?>, RecipeModifier.ModifierApplier> modifierAppliers = new HashMap<>();
    private final Map<RequirementType<?, ?>, RecipeModifier.ModifierApplier> chanceModifierAppliers = new HashMap<>();

    private final List<RecipeModifier> permanentModifierList = new ArrayList<>();

    private final List<ComponentOutputRestrictor> currentRestrictions = new ArrayList<>();

    private final List<ComponentRequirement<?, ?>> requirements = new ArrayList<>();
    private final List<RequirementComponents> requirementComponents = new ArrayList<>();

    private ActiveMachineRecipe activeRecipe;
    private TileMultiblockMachineController controller = null;
    private ControllerCommandSender commandSender = null;

    private List<ProcessingComponent<?>> typeComponents = new ArrayList<>();

    private int currentIOTickIndex = 0;

    public RecipeCraftingContext(final int reloadCounter,
                                 final ActiveMachineRecipe activeRecipe,
                                 final TileMultiblockMachineController controller)
    {
        this.reloadCounter = reloadCounter;
        this.activeRecipe = activeRecipe;
        for (ComponentRequirement<?, ?> craftingRequirement : getParentRecipe().getCraftingRequirements()) {
            this.requirements.add(craftingRequirement.deepCopy());
        }

        init(activeRecipe, controller);
    }

    public RecipeCraftingContext reset() {
        this.modifiers.clear();
        this.modifierAppliers.clear();
        this.chanceModifierAppliers.clear();
        this.permanentModifierList.clear();
        this.currentRestrictions.clear();

        this.currentIOTickIndex = 0;
        return this;
    }

    public RecipeCraftingContext resetAll() {
        setParallelism(1);
        this.activeRecipe = null;
        this.controller = null;
        this.commandSender = null;
        this.typeComponents = null;
        this.requirementComponents.clear();

        return reset();
    }

    public void destroy() {
        resetAll();
        this.requirements.clear();
    }

    public RecipeCraftingContext init(final ActiveMachineRecipe activeRecipe,
                                      final TileMultiblockMachineController ctrl)
    {
        this.controller = ctrl;
        this.activeRecipe = activeRecipe;
        this.commandSender = new ControllerCommandSender(this.controller);

        reset();
        updateComponents(ctrl.getFoundComponents());
        return this;
    }

    public int getReloadCounter() {
        return reloadCounter;
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

    @Nonnull
    public RecipeModifier.ModifierApplier getModifierApplier(RequirementType<?, ?> target, boolean isChance) {
        return isChance
                ? chanceModifierAppliers.getOrDefault(target, RecipeModifier.ModifierApplier.DEFAULT_APPLIER)
                : modifierAppliers.getOrDefault(target, RecipeModifier.ModifierApplier.DEFAULT_APPLIER);
    }

    public float getDurationMultiplier() {
        float dur = this.getParentRecipe().getRecipeTotalTickTime();
        float result = RecipeModifier.applyModifiers(this, RequirementTypesMM.REQUIREMENT_DURATION, null, dur, false);
        return dur / result;
    }

    public void addRestriction(ComponentOutputRestrictor restrictor) {
        this.currentRestrictions.add(restrictor);
    }

    public List<ProcessingComponent<?>> getComponentsFor(ComponentRequirement<?, ?> requirement, @Nullable ComponentSelectorTag tag) {
        List<ProcessingComponent<?>> validComponents = new ArrayList<>();
        for (ProcessingComponent<?> typeComponent : this.typeComponents) {
            if (!requirement.isValidComponent(typeComponent, this)) {
                continue;
            }

            if (tag != null) {
                if (tag.equals(typeComponent.tag)) {
                    validComponents.add(typeComponent);
                }
            } else {
                validComponents.add(typeComponent);
            }
        }

        return validComponents;
    }

    public CraftingCheckResult ioTick(int currentTick) {
        ResultChance chance = new ResultChance(RAND.nextLong());
        CraftingCheckResult checkResult = new CraftingCheckResult();
        float durMultiplier = this.getDurationMultiplier();

        //Input / Output tick
        for (int i = currentIOTickIndex; i < requirementComponents.size(); i++) {
            final RequirementComponents reqComponent = requirementComponents.get(i);

            ComponentRequirement<?, ?> requirement = reqComponent.getRequirement();
            if (!(requirement instanceof ComponentRequirement.PerTick)) {
                if (requirement.getTriggerTime() >= 1) {
                    checkAndTriggerRequirement(checkResult, currentTick, chance, reqComponent);
                    if (checkResult.isFailure()) {
                        currentIOTickIndex = i;
                        return checkResult;
                    }
                    continue;
                }
                continue;
            }

            ComponentRequirement.PerTick<?, ?> perTickRequirement = (ComponentRequirement.PerTick<?, ?>) requirement;

            if (perTickRequirement instanceof ComponentRequirement.MultiComponent) {
                CraftCheck result = perTickRequirement.doIOTick(reqComponent.getComponents(), this, durMultiplier);
                if (!result.isSuccess()) {
                    currentIOTickIndex = i;
                    checkResult.addError(result.getUnlocalizedMessage());
                    return checkResult;
                }

                continue;
            }

            perTickRequirement.resetIOTick(this);
            perTickRequirement.startIOTick(this, durMultiplier);

            for (ProcessingComponent<?> component : reqComponent.getComponents()) {
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
                currentIOTickIndex = i;
                checkResult.addError(result.getUnlocalizedMessage());
                return checkResult;
            }
        }
        currentIOTickIndex = 0;

        this.getParentRecipe().getCommandContainer().runTickCommands(this.commandSender, currentTick);

        return CraftingCheckResult.SUCCESS;
    }

    public List<ComponentRequirement<?, ?>> getRequirementBy(RequirementType<?, ?> type) {
        return requirements.stream()
                .filter(req -> req.getRequirementType().equals(type))
                .collect(Collectors.toList());
    }

    public List<ComponentRequirement<?, ?>> getRequirementBy(RequirementType<?, ?> type, IOType ioType) {
        return requirements.stream()
                .filter(req -> req.getRequirementType().equals(type) && req.getActionType() == ioType)
                .collect(Collectors.toList());
    }

    private void checkAndTriggerRequirement(final CraftingCheckResult res,
                                            final int currentTick,
                                            final ResultChance chance,
                                            final RequirementComponents reqComponent)
    {
        ComponentRequirement<?, ?> req = reqComponent.getRequirement();
        int triggerTime = req.getTriggerTime() * Math.round(RecipeModifier.applyModifiers(
                this, RequirementTypesMM.REQUIREMENT_DURATION, null, 1, false));
        if (triggerTime <= 0 || triggerTime != currentTick || (req.isTriggered() && !req.isTriggerRepeatable())) {
            return;
        }

        if (canStartCrafting(res, reqComponent, new EnumMap<>(IOType.class))) {
            startCrafting(chance, reqComponent);
            req.setTriggered(true);
        }
    }

    public void startCrafting() {
        startCrafting(RAND.nextLong());
    }

    public void startCrafting(long seed) {
        ResultChance chance = new ResultChance(seed);

        for (RequirementComponents reqComponents : requirementComponents) {
            if (reqComponents.getRequirement().getTriggerTime() <= 0) {
                startCrafting(chance, reqComponents);
            }
        }

        this.getParentRecipe().getCommandContainer().runStartCommands(this.commandSender);
    }

    private void startCrafting(final ResultChance chance, final RequirementComponents reqComponents) {
        ComponentRequirement<?, ?> requirement = reqComponents.getRequirement();

        if (requirement instanceof ComponentRequirement.MultiComponent) {
            requirement.startCrafting(reqComponents.getComponents(), this, chance);
            return;
        }

        requirement.startRequirementCheck(chance, this);
        for (ProcessingComponent<?> component : reqComponents.getComponents()) {
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

    public void finishCrafting() {
        finishCrafting(RAND.nextLong());
    }

    public void finishCrafting(long seed) {
        ResultChanceCreateEvent event = new ResultChanceCreateEvent(
                controller, this, new ResultChance(seed), Phase.END);
        event.postEvent();
        ResultChance chance = event.getResultChance();

        for (RequirementComponents reqComponents : requirementComponents) {
            ComponentRequirement<?, ?> requirement = reqComponents.getRequirement();
            List<ProcessingComponent<?>> components = reqComponents.getComponents();

            if (requirement instanceof ComponentRequirement.MultiComponent) {
                requirement.finishCrafting(components, this, chance);
                continue;
            }

            requirement.startRequirementCheck(chance, this);
            for (ProcessingComponent<?> component : components) {
                AtomicReference<CraftCheck> check = new AtomicReference<>();
                if (requirement instanceof Asyncable) {
                    check.set(requirement.finishCrafting(component, this, chance));
                } else {
                    Sync.doSyncAction(() -> check.set(requirement.finishCrafting(component, this, chance)));
                }
                if (check.get().isSuccess()) {
                    break;
                }
            }
            requirement.endRequirementCheck();
        }

        this.getParentRecipe().getCommandContainer().runFinishCommands(this.commandSender);
    }

    public int getMaxParallelism() {
        int maxParallelism = this.activeRecipe.getMaxParallelism();
        List<RequirementComponents> parallelizableList = new ArrayList<>();
        for (RequirementComponents e : requirementComponents) {
            if (e.getRequirement() instanceof ComponentRequirement.Parallelizable) {
                parallelizableList.add(e);
            }
        }

        Map<IOType, Map<RequirementType<?, ?>, List<ProcessingComponent<?>>>> typeCopiedComp = new EnumMap<>(IOType.class);

        int reqMaxParallelism = maxParallelism;
        for (RequirementComponents reqComponent : parallelizableList) {
            ComponentRequirement<?, ?> req = reqComponent.getRequirement();
            ComponentRequirement.Parallelizable requirement = (ComponentRequirement.Parallelizable) req;
            List<ProcessingComponent<?>> copiedCompList = typeCopiedComp.computeIfAbsent(
                    req.actionType, v -> new Object2ObjectArrayMap<>()).computeIfAbsent(
                            req.requirementType, v -> req.copyComponents(reqComponent.getComponents()));

            reqMaxParallelism = Math.min(reqMaxParallelism, requirement.getMaxParallelism(copiedCompList, this, maxParallelism));
        }

        return reqMaxParallelism;
    }

    public void setParallelism(int parallelism) {
        for (RequirementComponents obj : requirementComponents) {
            if (obj.getRequirement() instanceof ComponentRequirement.Parallelizable) {
                ((ComponentRequirement.Parallelizable) obj.getRequirement()).setParallelism(parallelism);
            }
        }
        activeRecipe.setParallelism(parallelism);
    }

    public CraftingCheckResult canStartCrafting() {
        permanentModifierList.clear();
        if (getParentRecipe().isParallelized() && activeRecipe.getMaxParallelism() > 1) {
            setParallelism(Math.max(1, getMaxParallelism()));
        }
        return canStartCrafting(req -> true);
    }

    public CraftingCheckResult canRestartCrafting() {
        permanentModifierList.clear();
        int currentParallelism = activeRecipe.getParallelism();

        if (currentParallelism >= activeRecipe.getMaxParallelism()) {
            setParallelism(currentParallelism);
            CraftingCheckResult result = canStartCrafting(req -> true);
            if (!result.isSuccess()) {
                setParallelism(1);
            } else {
                return result;
            }
        }

        return canStartCrafting();
    }

    public CraftingCheckResult canFinishCrafting() {
        return this.canStartCrafting(req -> req.getRequirement().actionType == IOType.OUTPUT);
    }

    public CraftingCheckResult canStartCrafting(Predicate<RequirementComponents> filter) {
        currentRestrictions.clear();
        List<RequirementComponents> requirements = new ArrayList<>();
        for (RequirementComponents requirementComponent : this.requirementComponents) {
            if (filter.test(requirementComponent)) {
                requirements.add(requirementComponent);
            }
        }
        CraftingCheckResult result = new CraftingCheckResult();
        float successfulRequirements = 0;

        Map<IOType, Map<RequirementType<?, ?>, List<ProcessingComponent<?>>>> typeCopiedComp = new EnumMap<>(IOType.class);
        for (RequirementComponents reqEntry : requirements) {
            if (canStartCrafting(result, reqEntry, typeCopiedComp)) {
                successfulRequirements++;
            }
        }
        result.setValidity(successfulRequirements / requirements.size());

        currentRestrictions.clear();
        return result;
    }

    private boolean canStartCrafting(final CraftingCheckResult result,
                                     final RequirementComponents reqComponent,
                                     final Map<IOType, Map<RequirementType<?, ?>, List<ProcessingComponent<?>>>> typeCopiedComp)
    {

        ComponentRequirement<?, ?> req = reqComponent.getRequirement();
        req.startRequirementCheck(ResultChance.GUARANTEED, this);

        List<ProcessingComponent<?>> compList = reqComponent.getComponents();
        if (!compList.isEmpty()) {
            if (req instanceof ComponentRequirement.MultiComponent) {
                List<ProcessingComponent<?>> copiedCompList = typeCopiedComp.computeIfAbsent(
                        req.actionType, v -> new Object2ObjectArrayMap<>()).computeIfAbsent(
                        req.requirementType, v -> req.copyComponents(compList));

                CraftCheck check = req.canStartCrafting(copiedCompList, this);
                if (check.isSuccess()) {
                    return true;
                }
                result.addError(check.getUnlocalizedMessage());
                return false;
            }

            List<String> errorMessages = new ArrayList<>();
            for (ProcessingComponent<?> component : compList) {
                CraftCheck check = req.canStartCrafting(component, this, this.currentRestrictions);

                if (check.isSuccess()) {
                    req.endRequirementCheck();
                    return true;
                }

                if (!check.isInvalid() && !check.getUnlocalizedMessage().isEmpty()) {
                    errorMessages.add(check.getUnlocalizedMessage());
                }
            }
            errorMessages.forEach(result::addError);
        } else {
            // No component found that would apply for the given req
            result.addError(req.getMissingComponentErrorMessage(req.actionType));
        }

        req.endRequirementCheck();
        return false;
    }

    public void updateComponents(List<ProcessingComponent<?>> components) {
        this.typeComponents = components;
        updateRequirementComponents();
    }

    public void updateRequirementComponents() {
        requirementComponents.clear();
        requirements.forEach(req ->
                requirementComponents.add(new RequirementComponents(req, getComponentsFor(req, req.tag))));
    }

    public void addModifier(SingleBlockModifierReplacement replacement) {
        addModifier(replacement.getModifiers());
    }

    public void addModifier(RecipeModifier modifier) {
        if (modifier != null) {
            RequirementType<?, ?> target = modifier.getTarget();
            if (target == null) {
                target = RequirementTypesMM.REQUIREMENT_DURATION;
            }
            this.modifiers.computeIfAbsent(target, t -> new LinkedList<>()).add(modifier);
            updateModifierApplier(target);
        }
    }

    public void addModifier(Collection<RecipeModifier> modifiers) {
        Set<RequirementType<?, ?>> changed = new HashSet<>();

        for (RecipeModifier modifier : modifiers) {
            RequirementType<?, ?> target = modifier.getTarget();
            if (target == null) {
                target = RequirementTypesMM.REQUIREMENT_DURATION;
            }
            this.modifiers.computeIfAbsent(target, t -> new LinkedList<>()).add(modifier);
            changed.add(target);
        }

        changed.forEach(this::updateModifierApplier);
    }

    public void addModifier(List<RecipeModifier> modifiers) {
        if (modifiers.isEmpty()) {
            return;
        }
        if (modifiers.size() == 1) {
            addModifier(modifiers.get(0));
            return;
        }

        addModifier((Collection<RecipeModifier>) modifiers);
    }

    public void addPermanentModifier(RecipeModifier modifier) {
        if (modifier != null) {
            this.permanentModifierList.add(modifier);
            addModifier(modifier);
        }
    }

    public void updateModifierApplier(RequirementType<?, ?> reqType) {
        addModifierApplier(reqType, modifiers.computeIfAbsent(reqType, v -> new ArrayList<>()));
    }

    public void addModifierApplier(final RequirementType<?, ?> reqType, final List<RecipeModifier> recipeModifiers) {
        RecipeModifier.ModifierApplier applier = new RecipeModifier.ModifierApplier();
        RecipeModifier.ModifierApplier chancedApplier = new RecipeModifier.ModifierApplier();

        recipeModifiers.forEach(mod -> RecipeModifier.applyValueToApplier(mod.affectsChance() ? chancedApplier : applier, mod));

        if (!applier.isDefault()) {
            modifierAppliers.put(reqType, applier);
        }
        if (!chancedApplier.isDefault()) {
            chanceModifierAppliers.put(reqType, chancedApplier);
        }
    }

    public void overrideModifier(Collection<RecipeModifier> modifiers) {
        this.modifiers.clear();
        this.modifierAppliers.clear();
        this.chanceModifierAppliers.clear();
        addModifier(modifiers);
        addModifier(permanentModifierList);
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
