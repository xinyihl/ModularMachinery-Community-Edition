/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.crafting;

import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.data.IData;
import crafttweaker.api.minecraft.CraftTweakerMC;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.lib.RequirementTypesMM;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.machine.RecipeFailureActions;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import hellfirepvp.modularmachinery.common.tiles.TileMachineController;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.Constants;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenGetter;
import stanhebben.zenscript.annotations.ZenSetter;

import javax.annotation.Nonnull;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: ActiveMachineRecipe
 * Created by HellFirePvP
 * Date: 29.06.2017 / 15:50
 */
@ZenRegister
@ZenClass("mods.modularmachinery.ActiveMachineRecipe")
public class ActiveMachineRecipe {

    private final MachineRecipe recipe;
    private NBTTagCompound data = new NBTTagCompound();
    private int tick = 0, totalTick;
    private int maxParallelism, parallelism = 1;

    public ActiveMachineRecipe(MachineRecipe recipe, int maxParallelism) {
        this.recipe = recipe;
        this.totalTick = recipe.getRecipeTotalTickTime();
        this.maxParallelism = maxParallelism;
    }

    public ActiveMachineRecipe(NBTTagCompound serialized) {
        this.recipe = RecipeRegistry.getRecipe(new ResourceLocation(serialized.getString("recipeName")));
        this.tick = serialized.getInteger("tick");
        this.totalTick = serialized.getInteger("totalTick");
        if (serialized.hasKey("data", Constants.NBT.TAG_COMPOUND)) {
            data = serialized.getCompoundTag("data");
        }
        if (serialized.hasKey("maxParallelism")) {
            maxParallelism = serialized.getInteger("maxParallelism");
        }
        if (serialized.hasKey("parallelism")) {
            parallelism = serialized.getInteger("parallelism");
        }
    }

    public void reset() {
        this.tick = 0;
        this.data = new NBTTagCompound();
    }

    public MachineRecipe getRecipe() {
        return recipe;
    }

    @Nonnull
    public TileMachineController.CraftingStatus tick(TileMachineController ctrl, RecipeCraftingContext context) {
        totalTick = Math.round(RecipeModifier.applyModifiers(
                context.getModifiers(RequirementTypesMM.REQUIREMENT_DURATION),
                RequirementTypesMM.REQUIREMENT_DURATION, null, this.recipe.getRecipeTotalTickTime(), false));

        //Skip per-tick logic until controller can finish the recipe
        if (this.isCompleted()) {
            return TileMultiblockMachineController.CraftingStatus.working();
        }

        RecipeCraftingContext.CraftingCheckResult check;
        if ((check = context.ioTick(tick)).isFailure()) {
            //On Failure
            DynamicMachine machine = ctrl.getFoundMachine();
            //Some Actions
            if (machine != null) {
                RecipeFailureActions action = machine.getFailureAction();
                doFailureAction(action);
            } else {
                doFailureAction(RecipeFailureActions.getDefaultAction());
            }
            return TileMultiblockMachineController.CraftingStatus.failure(check.getFirstErrorMessage(""));
        } else {
            //Success
            this.tick++;
            return TileMultiblockMachineController.CraftingStatus.working();
        }
    }

    public void doFailureAction(RecipeFailureActions action) {
        switch (action) {
            case RESET:
                this.tick = 0;
                break;
            case DECREASE:
                if (this.tick > 0) {
                    this.tick--;
                }
                break;
        }
    }

    public boolean isCompleted() {
        return this.tick >= totalTick;
    }

    public void start(RecipeCraftingContext context) {
        context.startCrafting();
        totalTick = Math.round(RecipeModifier.applyModifiers(
                context.getModifiers(RequirementTypesMM.REQUIREMENT_DURATION),
                RequirementTypesMM.REQUIREMENT_DURATION, null, this.recipe.getRecipeTotalTickTime(), false));
    }

    public NBTTagCompound serialize() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("tick", this.tick);
        tag.setInteger("totalTick", this.totalTick);
        tag.setString("recipeName", this.recipe.getRegistryName().toString());
        tag.setInteger("maxParallelism", this.maxParallelism);
        tag.setInteger("parallelism", this.parallelism);

        if (!data.isEmpty()) {
            tag.setTag("data", data);
        }
        return tag;
    }

    @ZenSetter("maxParallelism")
    public void setMaxParallelism(int maxParallelism) {
        this.maxParallelism = maxParallelism;
    }

    @ZenGetter("maxParallelism")
    public int getMaxParallelism() {
        return maxParallelism;
    }

    @ZenGetter("parallelism")
    public int getParallelism() {
        return parallelism;
    }

    @ZenSetter("parallelism")
    public void setParallelism(int parallelism) {
        this.parallelism = parallelism;
    }

    @ZenGetter("tick")
    public int getTick() {
        return tick;
    }

    @ZenSetter("tick")
    public void setTick(int tick) {
        this.tick = tick;
    }

    @ZenGetter("totalTick")
    public int getTotalTick() {
        return totalTick;
    }

    @ZenSetter("totalTick")
    public void setTotalTick(int totalTick) {
        this.totalTick = totalTick;
    }

    @ZenGetter("registryName")
    public String getRegistryName() {
        return recipe.getRegistryName().toString();
    }

    @ZenGetter("data")
    public IData getData() {
        return CraftTweakerMC.getIDataModifyable(data);
    }

    @ZenSetter("data")
    public void setData(IData data) {
        this.data = CraftTweakerMC.getNBTCompound(data);
    }
}
