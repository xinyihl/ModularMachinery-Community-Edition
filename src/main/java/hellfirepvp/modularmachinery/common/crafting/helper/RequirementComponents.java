package hellfirepvp.modularmachinery.common.crafting.helper;

import java.util.List;

public class RequirementComponents {
    private final ComponentRequirement<?, ?> requirement;
    private final List<ProcessingComponent<?>> components;

    public RequirementComponents(final ComponentRequirement<?, ?> requirement, final List<ProcessingComponent<?>> components) {
        this.requirement = requirement;
        this.components = components;
    }

    public ComponentRequirement<?, ?> getRequirement() {
        return requirement;
    }

    public List<ProcessingComponent<?>> getComponents() {
        return components;
    }
}
