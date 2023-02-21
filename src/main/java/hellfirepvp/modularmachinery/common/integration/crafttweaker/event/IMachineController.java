package hellfirepvp.modularmachinery.common.integration.crafttweaker.event;

import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.world.IBlockPos;
import crafttweaker.api.world.IWorld;
import hellfirepvp.modularmachinery.common.crafting.ActiveMachineRecipe;
import hellfirepvp.modularmachinery.common.tiles.TileMachineController;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenGetter;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenRegister
@ZenClass("mods.modularmachinery.IMachineController")
public interface IMachineController {
    @ZenGetter("world")
    IWorld getIWorld();

    @ZenGetter("pos")
    IBlockPos getIPos();

    @ZenGetter("activeRecipe")
    ActiveMachineRecipe getActiveRecipe();

    @ZenGetter("formedMachineName")
    String getFormedMachineName();

    @ZenMethod
    void addModifier(String type, String ioType, int operation, int value, boolean affectChance);

    TileMachineController getController();
}
