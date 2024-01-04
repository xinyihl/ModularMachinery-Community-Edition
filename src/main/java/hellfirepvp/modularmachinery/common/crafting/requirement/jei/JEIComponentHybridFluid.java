/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.crafting.requirement.jei;

import hellfirepvp.modularmachinery.common.base.Mods;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementFluid;
import hellfirepvp.modularmachinery.common.integration.ingredient.HybridFluid;
import hellfirepvp.modularmachinery.common.integration.ingredient.HybridFluidGas;
import hellfirepvp.modularmachinery.common.integration.recipe.RecipeLayoutPart;
import mekanism.api.gas.GasStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.awt.*;
import java.util.Collections;
import java.util.List;

/**
 * This class is part of the Fracture Mod
 * The complete source code for this mod can be found on github.
 * Class: JEIComponentHybridFluid
 * Created by HellFirePvP
 * Date: 08.04.2018 / 12:49
 */
public class JEIComponentHybridFluid extends ComponentRequirement.JEIComponent<HybridFluid> {

    private final RequirementFluid requirement;

    public JEIComponentHybridFluid(RequirementFluid requirement) {
        this.requirement = requirement;
    }

    @Override
    public Class<HybridFluid> getJEIRequirementClass() {
        return HybridFluid.class;
    }

    @Override
    public Class<?> getTrueJEIRequirementClass() {
        if (Mods.MEKANISM.isPresent() && requirement.required instanceof HybridFluidGas) {
            return GasStack.class;
        }
        return FluidStack.class;
    }

    @Override
    public List<?> getTrueJEIIORequirements() {
        if (Mods.MEKANISM.isPresent() && requirement.required instanceof HybridFluidGas fluidGas) {
            return Collections.singletonList(fluidGas.asGasStack());
        }
        return Collections.singletonList(requirement.required.asFluidStack());
    }

    @Override
    public List<HybridFluid> getJEIIORequirements() {
        return Collections.singletonList(requirement.required);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public RecipeLayoutPart<HybridFluid> getLayoutPart(Point offset) {
        return new RecipeLayoutPart.Tank(offset);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void onJEIHoverTooltip(int slotIndex, boolean input, HybridFluid ingredient, List<String> tooltip) {
        JEIComponentItem.addChanceTooltip(input, tooltip, requirement.chance);
    }
}
