/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.tiles;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.block.BlockController;
import hellfirepvp.modularmachinery.common.crafting.ActiveMachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.MachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.RecipeRegistry;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentSelectorTag;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.data.Config;
import hellfirepvp.modularmachinery.common.item.ItemBlueprint;
import hellfirepvp.modularmachinery.common.lib.BlocksMM;
import hellfirepvp.modularmachinery.common.lib.RequirementTypesMM;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.machine.MachineRegistry;
import hellfirepvp.modularmachinery.common.machine.TaggedPositionBlockArray;
import hellfirepvp.modularmachinery.common.modifier.ModifierReplacement;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import hellfirepvp.modularmachinery.common.tiles.base.ColorableMachineTile;
import hellfirepvp.modularmachinery.common.tiles.base.MachineComponentTile;
import hellfirepvp.modularmachinery.common.tiles.base.TileEntityRestrictedTick;
import hellfirepvp.modularmachinery.common.util.IOInventory;
import hellfirepvp.modularmachinery.common.util.MiscUtils;
import hellfirepvp.modularmachinery.common.util.nbt.NBTHelper;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.CapabilityItemHandler;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: TileMachineController
 * Created by HellFirePvP
 * Date: 28.06.2017 / 17:14
 */
public class TileMachineController extends TileEntityRestrictedTick {
    public static final int BLUEPRINT_SLOT = 0;
    public static final int ACCELERATOR_SLOT = 1;

    private CraftingStatus craftingStatus = CraftingStatus.MISSING_STRUCTURE;
    private DynamicMachine.ModifierReplacementMap foundReplacements = null;
    private IOInventory inventory;
    private volatile DynamicMachine foundMachine = null;
    private volatile TaggedPositionBlockArray foundPattern = null;
    private volatile EnumFacing patternRotation = null;
    private volatile RecipeCraftingContext context = null;
    private volatile ActiveMachineRecipe activeRecipe = null;

    private List<Tuple<MachineComponent<?>, ComponentSelectorTag>> foundComponents = Lists.newArrayList();
    private Map<BlockPos, List<ModifierReplacement>> foundModifiers = new HashMap<>();

    public TileMachineController() {
        this.inventory = buildInventory();
        this.inventory.setStackLimit(1, BLUEPRINT_SLOT);
    }

    public ActiveMachineRecipe getActiveRecipe() {
        return activeRecipe;
    }

    private IOInventory buildInventory() {
        return new IOInventory(this, new int[0], new int[0])
                .setMiscSlots(BLUEPRINT_SLOT, ACCELERATOR_SLOT);
    }

    public IOInventory getInventory() {
        return inventory;
    }

    @Override
    public void doRestrictedTick() {
        if (!getWorld().isRemote) {
            if (getWorld().getStrongPower(getPos()) > 0) {
                return;
            }

            ModularMachinery.PARALLEL_EXECUTOR.addPreTickTask(() -> {
                checkStructure();
                updateComponents();
            });

            if (this.foundMachine != null && this.foundPattern != null && this.patternRotation != null) {
                if (this.activeRecipe == null) {
                    if (this.ticksExisted % 40 == 0) {
                        ModularMachinery.PARALLEL_EXECUTOR.addPreTickTask(this::searchAndUpdateRecipe);
                    }
                } else {
                    context = this.foundMachine.createContext(this.activeRecipe, this, this.foundComponents, MiscUtils.flatten(this.foundModifiers.values()));
                    //Handle perTick IO and tick progression
                    this.craftingStatus = this.activeRecipe.tick(this, context);

                    if (this.activeRecipe.getRecipe().doesCancelRecipeOnPerTickFailure() && !this.craftingStatus.isCrafting()) {
                        this.activeRecipe = null;
                    } else if (this.activeRecipe.isCompleted()) {
                        this.activeRecipe.complete(context);
                        this.activeRecipe.reset();
                        context = this.foundMachine.createContext(this.activeRecipe, this, this.foundComponents, MiscUtils.flatten(this.foundModifiers.values()));
                        ModularMachinery.PARALLEL_EXECUTOR.addPostTickTask(this::tryRedoRecipe);
                    }
                    markForUpdate();
                }
            } else {
                craftingStatus = CraftingStatus.MISSING_STRUCTURE;
                markForUpdate();
            }
        }
    }

    public int getRecipeTotalTime() {
        if (foundMachine == null || context == null) return 0;

        int recipeTotalTickTime = activeRecipe.getRecipe().getRecipeTotalTickTime();
        recipeTotalTickTime = Math.round(RecipeModifier.applyModifiers(context.getModifiers(RequirementTypesMM.REQUIREMENT_DURATION), RequirementTypesMM.REQUIREMENT_DURATION, null, recipeTotalTickTime, false));

        return recipeTotalTickTime;
    }

    private void tryRedoRecipe() {
        RecipeCraftingContext.CraftingCheckResult tryResult = context.canStartCrafting();

        if (!tryResult.isFailure()) {
            activeRecipe = context.getActiveRecipe();
            synchronized (TileMachineController.class) {
                activeRecipe.start(context);
                markForUpdate();
            }
        } else {
            activeRecipe = null;
            craftingStatus = CraftingStatus.failure(Iterables.getFirst(tryResult.getUnlocalizedErrorMessages(), ""));
        }
    }

    private void searchAndUpdateRecipe() {
        Iterable<MachineRecipe> availableRecipes = RecipeRegistry.getRecipesFor(foundMachine);

        MachineRecipe highestValidity = null;
        RecipeCraftingContext.CraftingCheckResult highestValidityResult = null;
        float validity = 0F;

        for (MachineRecipe recipe : availableRecipes) {
            ActiveMachineRecipe aRecipe = new ActiveMachineRecipe(recipe);
            RecipeCraftingContext context = foundMachine.createContext(aRecipe, TileMachineController.this, foundComponents, MiscUtils.flatten(foundModifiers.values()));
            RecipeCraftingContext.CraftingCheckResult result = context.canStartCrafting();
            if (!result.isFailure()) {
                activeRecipe = aRecipe;
                synchronized (TileMachineController.class) {
                    activeRecipe.start(context);
                    markForUpdate();
                }
                break;
            } else if (highestValidity == null ||
                    (result.getValidity() >= 0.5F && result.getValidity() > validity)) {
                highestValidity = recipe;
                highestValidityResult = result;
                validity = result.getValidity();
            }
        }

        if (activeRecipe == null) {
            if (highestValidity != null) {
                craftingStatus = CraftingStatus.failure(
                        Iterables.getFirst(highestValidityResult.getUnlocalizedErrorMessages(), ""));
            } else {
                craftingStatus = CraftingStatus.failure(Type.NO_RECIPE.getUnlocalizedDescription());
            }
        } else {
            craftingStatus = CraftingStatus.working();
        }
    }

    private void resetMachine(boolean resetAll) {
        if (resetAll) {
            this.activeRecipe = null;
            this.context = null;
            this.craftingStatus = CraftingStatus.MISSING_STRUCTURE;
        }
        this.foundMachine = null;
        this.foundPattern = null;
        this.patternRotation = null;
        this.foundReplacements = null;
    }

    private void checkStructure() {
        if (ticksExisted % 30 == 0) {
            if (this.foundMachine != null && this.foundPattern != null && this.patternRotation != null) {
                if (this.foundMachine.requiresBlueprint() && !this.foundMachine.equals(getBlueprintMachine())) {
                    resetMachine(true);
                } else if (!foundPattern.matches(getWorld(), getPos(), true, this.foundReplacements)) {
                    resetMachine(true);
                }
            }
            if (this.foundMachine == null || this.foundPattern == null || this.patternRotation == null || this.foundReplacements == null) {
                resetMachine(false);

                DynamicMachine blueprint = getBlueprintMachine();
                if (blueprint != null) {
                    if (matchesRotation(blueprint.getPattern(), blueprint)) {
                        this.world.setBlockState(pos, BlocksMM.blockController.getDefaultState().withProperty(BlockController.FACING, this.patternRotation));

                        if (this.foundMachine.getMachineColor() != Config.machineColor) {
                            synchronized (TileMachineController.class) {
                                distributeCasingColor();
                            }
                        }
                    }
                } else {
                    for (DynamicMachine machine : MachineRegistry.getRegistry()) {
                        if (machine.requiresBlueprint()) continue;
                        if (matchesRotation(machine.getPattern(), machine)) {
                            synchronized (TileMachineController.class) {
                                this.world.setBlockState(pos, BlocksMM.blockController.getDefaultState().withProperty(BlockController.FACING, this.patternRotation));
                            }

                            if (this.foundMachine.getMachineColor() != Config.machineColor) {
                                synchronized (TileMachineController.class) {
                                    distributeCasingColor();
                                }
                            }
                            break;
                        }
                    }
                }
            }
        }
    }

    private void distributeCasingColor() {
        if (this.foundMachine != null && this.foundPattern != null) {
            int color = this.foundMachine.getMachineColor();
            tryColorize(getPos(), color);
            for (BlockPos pos : this.foundPattern.getPattern().keySet()) {
                tryColorize(this.getPos().add(pos), color);
            }
        }
    }

    /**
     * 尝试对方块上色。
     * 此方法内容不应被修改，因为内容已被 gugu_utils 所覆盖。
     *
     * @param pos   位置
     * @param color 颜色
     */
    private void tryColorize(BlockPos pos, int color) {
        TileEntity te = this.getWorld().getTileEntity(pos);
        if (te instanceof ColorableMachineTile) {
            ((ColorableMachineTile) te).setMachineColor(color);
            getWorld().addBlockEvent(pos, getWorld().getBlockState(pos).getBlock(), 1, 1);
        }
    }

    private boolean matchesRotation(TaggedPositionBlockArray pattern, DynamicMachine machine) {
        EnumFacing face = EnumFacing.NORTH;
        DynamicMachine.ModifierReplacementMap replacements = machine.getModifiersAsMatchingReplacements();
        do {
            if (pattern.matches(getWorld(), getPos(), false, replacements)) {
                this.foundPattern = pattern;
                this.patternRotation = face;
                this.foundMachine = machine;
                this.foundReplacements = replacements;
                return true;
            }
            face = face.rotateYCCW();
            pattern = pattern.rotateYCCW();
            replacements = replacements.rotateYCCW();
        } while (face != EnumFacing.NORTH);
        this.foundPattern = null;
        this.patternRotation = null;
        this.foundMachine = null;
        this.foundReplacements = null;
        return false;
    }

    private void updateComponents() {
        if (this.foundMachine == null || this.foundPattern == null || this.patternRotation == null || this.foundReplacements == null) {
            this.foundComponents.clear();
            this.foundModifiers.clear();

            resetMachine(false);
            return;
        }
        if (ticksExisted % 40 == 0) {
            this.foundComponents = Lists.newArrayList();
            for (BlockPos potentialPosition : this.foundPattern.getPattern().keySet()) {
                BlockPos realPos = getPos().add(potentialPosition);
                TileEntity te = getWorld().getTileEntity(realPos);
                if (te instanceof MachineComponentTile) {
                    ComponentSelectorTag tag = this.foundPattern.getTag(potentialPosition);
                    MachineComponent<?> component = ((MachineComponentTile) te).provideComponent();
                    if (component != null) {
                        this.foundComponents.add(new Tuple<>(component, tag));
                    }
                }
            }

            int rotations = 0;
            EnumFacing rot = EnumFacing.NORTH;
            while (rot != this.patternRotation) {
                rot = rot.rotateYCCW();
                rotations++;
            }

            this.foundModifiers = Maps.newHashMap();
            for (Map.Entry<BlockPos, List<ModifierReplacement>> offsetModifiers : this.foundMachine.getModifiers().entrySet()) {
                BlockPos at = offsetModifiers.getKey();
                for (int i = 0; i < rotations; i++) {
                    at = new BlockPos(at.getZ(), at.getY(), -at.getX());
                }
                BlockPos realAt = this.getPos().add(at);
                for (ModifierReplacement mod : offsetModifiers.getValue()) {
                    if (mod.getBlockInformation().matches(this.world, realAt, true)) {
                        this.foundModifiers.putIfAbsent(offsetModifiers.getKey(), Lists.newArrayList());
                        this.foundModifiers.get(offsetModifiers.getKey()).add(mod);
                    }
                }
            }
        }
    }

    public float getCurrentActiveRecipeProgress(float partial) {
        if (activeRecipe == null) return 0F;
        float tick = activeRecipe.getTick() + partial;
        float maxTick = activeRecipe.getTotalTick();
        return MathHelper.clamp(tick / maxTick, 0F, 1F);
    }

    public boolean hasActiveRecipe() {
        return this.activeRecipe != null;
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newSate) {
        return oldState.getBlock() != newSate.getBlock();
    }

    @Nullable
    public DynamicMachine getFoundMachine() {
        return foundMachine;
    }

    @Nullable
    public DynamicMachine getBlueprintMachine() {
        ItemStack blueprintSlotted = this.inventory.getStackInSlot(BLUEPRINT_SLOT);
        if (!blueprintSlotted.isEmpty()) {
            return ItemBlueprint.getAssociatedMachine(blueprintSlotted);
        }
        return null;
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return (T) inventory;
        }
        return super.getCapability(capability, facing);
    }

    public CraftingStatus getCraftingStatus() {
        return craftingStatus;
    }

    @Override
    public void readCustomNBT(NBTTagCompound compound) {
        super.readCustomNBT(compound);
        this.inventory = IOInventory.deserialize(this, compound.getCompoundTag("items"));
        this.inventory.setStackLimit(1, BLUEPRINT_SLOT);

        if (compound.hasKey("status")) { //Legacy support
            this.craftingStatus = new CraftingStatus(Type.values()[compound.getInteger("status")], "");
        } else {
            this.craftingStatus = CraftingStatus.deserialize(compound.getCompoundTag("statusTag"));
        }

        if (compound.hasKey("machine") && compound.hasKey("rotation")) {
            ResourceLocation rl = new ResourceLocation(compound.getString("machine"));
            DynamicMachine machine = MachineRegistry.getRegistry().getMachine(rl);
            if (machine == null) {
                ModularMachinery.log.info("Couldn't find machine named " + rl + " for controller at " + getPos());
                resetMachine(false);
            } else {
                EnumFacing rot = EnumFacing.byHorizontalIndex(compound.getInteger("rotation"));
                EnumFacing offset = EnumFacing.NORTH;
                TaggedPositionBlockArray pattern = machine.getPattern();
                DynamicMachine.ModifierReplacementMap replacements = machine.getModifiersAsMatchingReplacements();
                while (offset != rot) {
                    replacements = replacements.rotateYCCW();
                    pattern = pattern.rotateYCCW();
                    offset = offset.rotateY();
                }
                this.patternRotation = rot;
                this.foundPattern = pattern;
                this.foundMachine = machine;
                this.foundReplacements = replacements;

                if (compound.hasKey("modifierOffsets")) {
                    NBTTagList list = compound.getTagList("modifierOffsets", Constants.NBT.TAG_COMPOUND);
                    for (int i = 0; i < list.tagCount(); i++) {
                        NBTTagCompound posTag = list.getCompoundTagAt(i);
                        BlockPos modOffset = NBTUtil.getPosFromTag(posTag.getCompoundTag("position"));
                        IBlockState state = NBTHelper.getBlockState(posTag, "state");
                        if (state != null) {
                            for (ModifierReplacement mod : this.foundMachine.getModifiers().getOrDefault(modOffset, Lists.newArrayList())) {
                                if (mod.getBlockInformation().matchesState(state)) {
                                    this.foundModifiers.putIfAbsent(modOffset, Lists.newArrayList());
                                    this.foundModifiers.get(modOffset).add(mod);
                                }
                            }
                        }
                    }
                }
            }
        } else {
            resetMachine(false);
        }
        if (compound.hasKey("activeRecipe")) {
            NBTTagCompound tag = compound.getCompoundTag("activeRecipe");
            ActiveMachineRecipe recipe = new ActiveMachineRecipe(tag);
            if (recipe.getRecipe() == null) {
                ModularMachinery.log.info("Couldn't find recipe named " + tag.getString("recipeName") + " for controller at " + getPos());
                this.activeRecipe = null;
            } else {
                this.activeRecipe = recipe;
            }
        } else {
            this.activeRecipe = null;
        }
    }

    @Override
    public void writeCustomNBT(NBTTagCompound compound) {
        super.writeCustomNBT(compound);
        compound.setTag("items", this.inventory.writeNBT());
        compound.setTag("statusTag", this.craftingStatus.serialize());

        if (this.foundMachine != null && this.patternRotation != null) {
            compound.setString("machine", this.foundMachine.getRegistryName().toString());
            compound.setInteger("rotation", this.patternRotation.getHorizontalIndex());

            NBTTagList listModifierOffsets = new NBTTagList();
            for (BlockPos offset : this.foundModifiers.keySet()) {
                NBTTagCompound tag = new NBTTagCompound();

                tag.setTag("position", NBTUtil.createPosTag(offset));
                NBTHelper.setBlockState(tag, "state", world.getBlockState(getPos().add(offset)));

                listModifierOffsets.appendTag(tag);
            }
            compound.setTag("modifierOffsets", listModifierOffsets);
        }
        if (this.activeRecipe != null) {
            compound.setTag("activeRecipe", this.activeRecipe.serialize());
        }
    }

    public enum Type {

        MISSING_STRUCTURE,
        NO_RECIPE,
        CRAFTING;

        public String getUnlocalizedDescription() {
            return "gui.controller.status." + this.name().toLowerCase();
        }

    }

    public static class CraftingStatus {

        private static final CraftingStatus SUCCESS = new CraftingStatus(Type.CRAFTING, "");
        private static final CraftingStatus MISSING_STRUCTURE = new CraftingStatus(Type.MISSING_STRUCTURE, "");

        private final Type status;
        private final String unlocalizedMessage;

        private CraftingStatus(Type status, String unlocalizedMessage) {
            this.status = status;
            this.unlocalizedMessage = unlocalizedMessage;
        }

        public static CraftingStatus working() {
            return SUCCESS;
        }

        public static CraftingStatus failure(String unlocMessage) {
            return new CraftingStatus(Type.NO_RECIPE, unlocMessage);
        }

        private static CraftingStatus deserialize(NBTTagCompound tag) {
            Type type = Type.values()[tag.getInteger("type")];
            String unlocMessage = tag.getString("message");
            return new CraftingStatus(type, unlocMessage);
        }

        public Type getStatus() {
            return status;
        }

        public String getUnlocMessage() {
            return !unlocalizedMessage.isEmpty() ? unlocalizedMessage : this.status.getUnlocalizedDescription();
        }

        public boolean isCrafting() {
            return this.status == Type.CRAFTING;
        }

        private NBTTagCompound serialize() {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger("type", this.status.ordinal());
            tag.setString("message", this.unlocalizedMessage);
            return tag;
        }
    }
}
