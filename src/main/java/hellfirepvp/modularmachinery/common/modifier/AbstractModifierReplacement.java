package hellfirepvp.modularmachinery.common.modifier;

import com.google.common.collect.Lists;
import hellfirepvp.modularmachinery.common.util.MiscUtils;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractModifierReplacement {
    private static final AtomicInteger DEFAULT_NAME_COUNTER = new AtomicInteger(0);
    protected final String modifierName;
    protected final List<RecipeModifier> modifier;
    protected final List<String> description;

    public AbstractModifierReplacement(List<RecipeModifier> modifier, String description) {
        this(null, modifier, description);
    }

    public AbstractModifierReplacement(String modifierName, List<RecipeModifier> modifier, String description) {
        this.modifierName = modifierName == null ? "ReplacementModifier - " + DEFAULT_NAME_COUNTER.getAndIncrement() : modifierName;
        this.modifier = modifier;
        this.description = description.isEmpty() ? Lists.newArrayList() : MiscUtils.splitStringBy(description, "\n");
    }

    public AbstractModifierReplacement(String modifierName, List<RecipeModifier> modifier, List<String> description) {
        this.modifierName = modifierName;
        this.modifier = modifier;
        this.description = description;
    }

    public String getModifierName() {
        return modifierName;
    }

    public List<RecipeModifier> getModifiers() {
        return Collections.unmodifiableList(modifier);
    }

    public List<String> getDescriptionLines() {
        return description;
    }
}
