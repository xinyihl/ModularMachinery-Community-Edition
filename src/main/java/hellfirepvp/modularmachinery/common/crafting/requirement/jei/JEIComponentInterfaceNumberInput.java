package hellfirepvp.modularmachinery.common.crafting.requirement.jei;

import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementInterfaceNumInput;
import hellfirepvp.modularmachinery.common.integration.recipe.RecipeLayoutPart;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.awt.*;
import java.util.Collections;
import java.util.List;

public class JEIComponentInterfaceNumberInput extends ComponentRequirement.JEIComponent<Float> {
    private final RequirementInterfaceNumInput requirement;

    public JEIComponentInterfaceNumberInput(RequirementInterfaceNumInput requirement) {
        this.requirement = requirement;
    }

    @Override
    public Class<Float> getJEIRequirementClass() {
        return Float.class;
    }

    @Override
    public final List<Float> getJEIIORequirements() {
        return Collections.singletonList(requirement.getMinValue());
    }

    @Override
    public RecipeLayoutPart<Float> getLayoutPart(Point offset) {
        return new RequirementInterfaceNumInputEmptyLayoutPart(offset);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public RecipeLayoutPart<Float> getTemplateLayout() {
        return super.getTemplateLayout();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void onJEIHoverTooltip(int slotIndex, boolean input, Float ingredient, List<String> tooltip) {

    }

    private static class RequirementInterfaceNumInputEmptyLayoutPart extends RecipeLayoutPart.EmptyLayoutPart<Float> {
        private RequirementInterfaceNumInputEmptyLayoutPart(Point offset) {
            super(offset);
        }

        @Override
        public Class<Float> getLayoutTypeClass() {
            return Float.class;
        }
    }
}
