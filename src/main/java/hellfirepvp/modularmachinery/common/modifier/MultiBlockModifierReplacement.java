package hellfirepvp.modularmachinery.common.modifier;

import crafttweaker.annotations.ZenRegister;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.event.IMachineController;
import hellfirepvp.modularmachinery.common.tiles.TileMachineController;
import hellfirepvp.modularmachinery.common.util.BlockArray;
import hellfirepvp.modularmachinery.common.util.BlockArrayCache;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenGetter;
import stanhebben.zenscript.annotations.ZenMethod;

import java.util.List;

@ZenRegister
@ZenClass("mods.modularmachinery.MultiBlockModifierReplacement")
public class MultiBlockModifierReplacement extends AbstractModifierReplacement {
    private final BlockArray blockArray;

    public MultiBlockModifierReplacement(String modifierName, BlockArray blockArray, List<RecipeModifier> modifiers, List<String> description) {
        super(modifierName, modifiers, description);
        this.blockArray = blockArray;
    }

    @ZenGetter("blockArray")
    public BlockArray getBlockArray() {
        return blockArray;
    }

    @ZenMethod
    public boolean matches(IMachineController machineController) {
        TileMachineController ctrl = machineController.getController();
        BlockArray blockArray = BlockArrayCache.getBlockArrayCache(this.blockArray, ctrl.getControllerRotation());
        return blockArray.matches(ctrl.getWorld(), ctrl.getPos(), false, null);
    }
}
