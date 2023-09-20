/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.util;

import github.kasuminova.mmce.common.helper.AdvancedItemChecker;
import github.kasuminova.mmce.common.util.OredictCache;
import hellfirepvp.modularmachinery.common.crafting.helper.ProcessingComponent;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import hellfirepvp.modularmachinery.common.util.nbt.NBTMatchingHelper;
import io.netty.util.collection.IntObjectHashMap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.oredict.OreDictionary;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: ItemUtils
 * Created by HellFirePvP
 * Date: 28.06.2017 / 12:42
 */
public class ItemUtils {

    public static void decrStackInInventory(ItemStackHandler handler, int slot) {
        if (slot < 0 || slot >= handler.getSlots()) return;
        ItemStack st = handler.getStackInSlot(slot);
        if (st.isEmpty()) return;
        st.setCount(st.getCount() - 1);
        if (st.getCount() <= 0) {
            handler.setStackInSlot(slot, ItemStack.EMPTY);
        }
    }

    //Negative amount: overhead fuel burnt
    //Positive amount: Failure/couldn't find enough fuel
    public static int consumeFromInventoryFuel(IItemHandlerModifiable handler, int fuelAmtToConsume, boolean simulate, @Nullable NBTTagCompound matchNBTTag) {
        Map<Integer, ItemStack> contents = findItemsIndexedInInventoryFuel(handler, matchNBTTag);
        if (contents.isEmpty()) {
            return fuelAmtToConsume;
        }

        for (int slot : contents.keySet()) {
            ItemStack inSlot = contents.get(slot);
            if (inSlot.getItem().hasContainerItem(inSlot)) {
                if (inSlot.getCount() > 1) {
                    continue; //uh... rip. we won't consume 16 buckets at once.
                }
                ItemStack stack = ForgeHooks.getContainerItem(inSlot);
                fuelAmtToConsume -= TileEntityFurnace.getItemBurnTime(inSlot);
                if (!simulate) {
                    handler.setStackInSlot(slot, stack.copy());
                }
                if (fuelAmtToConsume <= 0) {
                    break;
                }
            }
            int fuelPer = TileEntityFurnace.getItemBurnTime(inSlot);
            int toConsumeDiv = fuelAmtToConsume / fuelPer;
            int fuelMod = fuelAmtToConsume % fuelPer;

            int toConsume = toConsumeDiv + (fuelMod > 0 ? 1 : 0);
            int toRemove = Math.min(toConsume, inSlot.getCount());

            fuelAmtToConsume -= toRemove * fuelPer;
            if (!simulate) {
                handler.setStackInSlot(slot, copyStackWithSize(inSlot, inSlot.getCount() - toRemove));
            }
            if (fuelAmtToConsume <= 0) {
                break;
            }
        }
        return fuelAmtToConsume;
    }

    public static boolean consumeFromInventory(IItemHandlerModifiable handler, ItemStack toConsume, boolean simulate, @Nullable NBTTagCompound matchNBTTag) {
        Map<Integer, ItemStack> contents = findItemsIndexedInInventory(handler, toConsume, false, matchNBTTag);
        if (contents.isEmpty()) return false;

        int cAmt = toConsume.getCount();
        for (int slot : contents.keySet()) {
            ItemStack inSlot = contents.get(slot);
            if (inSlot.getItem().hasContainerItem(inSlot)) {
                if (inSlot.getCount() > 1) {
                    continue; //uh... rip. we won't consume 16 buckets at once.
                }
                ItemStack stack = ForgeHooks.getContainerItem(inSlot);
                cAmt--;
                if (!simulate) {
                    handler.setStackInSlot(slot, stack.copy());
                }
                if (cAmt <= 0) {
                    break;
                }
            }
            int toRemove = Math.min(cAmt, inSlot.getCount());
            cAmt -= toRemove;
            if (!simulate) {
                handler.setStackInSlot(slot, copyStackWithSize(inSlot, inSlot.getCount() - toRemove));
            }
            if (cAmt <= 0) {
                break;
            }
        }
        return cAmt <= 0;
    }

    public static boolean consumeFromInventory(IItemHandlerModifiable handler, ItemStack toConsume, boolean simulate, AdvancedItemChecker itemChecker, TileMultiblockMachineController controller) {
        Map<Integer, ItemStack> contents = findItemsIndexedInInventory(handler, toConsume, false, itemChecker, controller);
        if (contents.isEmpty()) return false;

        int cAmt = toConsume.getCount();
        for (int slot : contents.keySet()) {
            ItemStack inSlot = contents.get(slot);
            if (inSlot.getItem().hasContainerItem(inSlot)) {
                if (inSlot.getCount() > 1) {
                    continue; //uh... rip. we won't consume 16 buckets at once.
                }
                ItemStack stack = ForgeHooks.getContainerItem(inSlot);
                cAmt--;
                if (!simulate) {
                    handler.setStackInSlot(slot, stack.copy());
                }
                if (cAmt <= 0) {
                    break;
                }
            }
            int toRemove = Math.min(cAmt, inSlot.getCount());
            cAmt -= toRemove;
            if (!simulate) {
                handler.setStackInSlot(slot, copyStackWithSize(inSlot, inSlot.getCount() - toRemove));
            }
            if (cAmt <= 0) {
                break;
            }
        }
        return cAmt <= 0;
    }

    public static int consumeAll(IItemHandlerModifiable handler, ItemStack toConsume, int maxMultiplier, AdvancedItemChecker itemChecker, TileMultiblockMachineController controller) {
        Map<Integer, ItemStack> contents = findItemsIndexedInInventory(handler, toConsume, false, itemChecker, controller);
        if (toConsume.getCount() <= 0 || contents.isEmpty()) {
            return 0;
        }
        return consumeAllInternal(handler, contents, maxMultiplier * toConsume.getCount());
    }

    public static int consumeAll(IItemHandlerModifiable handler, ItemStack toConsume, int maxMultiplier, @Nullable NBTTagCompound matchNBTTag) {
        Map<Integer, ItemStack> contents = findItemsIndexedInInventory(handler, toConsume, false, matchNBTTag);
        if (toConsume.getCount() <= 0 || contents.isEmpty()) {
            return 0;
        }
        return consumeAllInternal(handler, contents, maxMultiplier * toConsume.getCount());
    }

    public static int consumeAll(IItemHandlerModifiable handler, String oreName, int amount, int maxMultiplier, AdvancedItemChecker itemChecker, TileMultiblockMachineController controller) {
        Map<Integer, ItemStack> contents = findItemsIndexedInInventoryOreDict(handler, oreName, itemChecker, controller);
        if (amount <= 0 || contents.isEmpty()) {
            return 0;
        }
        return consumeAllInternal(handler, contents, maxMultiplier * amount);
    }

    public static int consumeAll(IItemHandlerModifiable handler, String oreName, int amount, int maxMultiplier, @Nullable NBTTagCompound matchNBTTag) {
        Map<Integer, ItemStack> contents = findItemsIndexedInInventoryOreDict(handler, oreName, matchNBTTag);
        if (amount <= 0 || contents.isEmpty()) {
            return 0;
        }
        return consumeAllInternal(handler, contents, maxMultiplier * amount);
    }

    public static int insertAll(@Nonnull ItemStack stack, IItemHandlerModifiable handler, int maxInsert) {
        if (stack.getCount() <= 0) {
            return 0;
        }

        int inserted = 0;
        for (int i = 0; i < handler.getSlots() && stack.getCount() > 0; i++) {
            int maxStackSize = handler.getSlotLimit(i);
            ItemStack in = handler.getStackInSlot(i);
            int count = in.getCount();
            if (count >= maxStackSize) {
                continue;
            }

            if (in.isEmpty()) {
                int toInsert = Math.min(maxInsert - inserted, maxStackSize);
                handler.setStackInSlot(i, copyStackWithSize(stack, toInsert));
                inserted += toInsert;
            } else {
                if (stackEqualsNonNBT(stack, in) && matchTags(stack, in)) {
                    int toInsert = Math.min(maxInsert - inserted, maxStackSize - count);
                    handler.setStackInSlot(i, copyStackWithSize(stack, toInsert + count));
                    inserted += toInsert;
                }
            }

            if (inserted >= maxInsert) {
                break;
            }
        }

        return inserted;
    }

    private static int consumeAllInternal(IItemHandlerModifiable handler, Map<Integer, ItemStack> contents, int maxConsume) {
        int cAmt = 0;
        for (Map.Entry<Integer, ItemStack> content : contents.entrySet()) {
            int slot = content.getKey();
            ItemStack stack = content.getValue();
            int count = stack.getCount();

            if (count >= 1) {
                if (stack.getItem().hasContainerItem(stack)) {
                    continue; //uh... rip. we won't consume 16 buckets at once.
                }
            }

            int toConsume = Math.min(maxConsume - cAmt, count);
            handler.setStackInSlot(slot, copyStackWithSize(stack, count - toConsume));
            cAmt += toConsume;

            if (cAmt >= maxConsume) {
                break;
            }
        }

        return cAmt;
    }

    public static boolean consumeFromInventoryOreDict(IItemHandlerModifiable handler, String oreName, int amount, boolean simulate, @Nullable NBTTagCompound matchNBTTag) {
        Map<Integer, ItemStack> contents = findItemsIndexedInInventoryOreDict(handler, oreName, matchNBTTag);
        if (contents.isEmpty()) return false;

        int cAmt = amount;
        for (int slot : contents.keySet()) {
            ItemStack inSlot = contents.get(slot);
            if (inSlot.getItem().hasContainerItem(inSlot)) {
                if (inSlot.getCount() > 1) {
                    continue; //uh... rip. we won't consume 16 buckets at once.
                }
                ItemStack stack = ForgeHooks.getContainerItem(inSlot);
                cAmt--;
                if (!simulate) {
                    handler.setStackInSlot(slot, stack.copy());
                }
                if (cAmt <= 0) {
                    break;
                }
            }
            int toRemove = Math.min(cAmt, inSlot.getCount());
            cAmt -= toRemove;
            if (!simulate) {
                handler.setStackInSlot(slot, copyStackWithSize(inSlot, inSlot.getCount() - toRemove));
            }
            if (cAmt <= 0) {
                break;
            }
        }
        return cAmt <= 0;
    }

    public static boolean consumeFromInventoryOreDict(IItemHandlerModifiable handler, String oreName, int amount, boolean simulate, AdvancedItemChecker itemChecker, TileMultiblockMachineController controller) {
        Map<Integer, ItemStack> contents = findItemsIndexedInInventoryOreDict(handler, oreName, itemChecker, controller);
        if (contents.isEmpty()) return false;

        int cAmt = amount;
        for (int slot : contents.keySet()) {
            ItemStack inSlot = contents.get(slot);
            if (inSlot.getItem().hasContainerItem(inSlot)) {
                if (inSlot.getCount() > 1) {
                    continue; //uh... rip. we won't consume 16 buckets at once.
                }
                ItemStack stack = ForgeHooks.getContainerItem(inSlot);
                cAmt--;
                if (!simulate) {
                    handler.setStackInSlot(slot, stack.copy());
                }
                if (cAmt <= 0) {
                    break;
                }
            }
            int toRemove = Math.min(cAmt, inSlot.getCount());
            cAmt -= toRemove;
            if (!simulate) {
                handler.setStackInSlot(slot, copyStackWithSize(inSlot, inSlot.getCount() - toRemove));
            }
            if (cAmt <= 0) {
                break;
            }
        }
        return cAmt <= 0;
    }

    //Returns the amount inserted
    public static int tryPlaceItemInInventory(@Nonnull ItemStack stack, IItemHandlerModifiable handler, boolean simulate) {
        return tryPlaceItemInInventory(stack, handler, 0, handler.getSlots(), simulate);
    }

    public static int tryPlaceItemInInventory(@Nonnull ItemStack stack, IItemHandlerModifiable handler, int start, int end, boolean simulate) {
        ItemStack toAdd = stack.copy();
//        if (!hasInventorySpace(toAdd, handler, start, end)) {
//            return 0;
//        }
        int insertedAmt = 0;
        int max = toAdd.getMaxStackSize();

        for (int i = start; i < end; i++) {
            ItemStack in = handler.getStackInSlot(i);
            if (in.isEmpty()) {
                int added = Math.min(stack.getCount(), max);
                stack.shrink(added);
                if (!simulate) {
                    handler.setStackInSlot(i, copyStackWithSize(toAdd, added));
                }
                insertedAmt += added;
                if (stack.getCount() <= 0)
                    return insertedAmt;
            } else {
                if (stackEqualsNonNBT(toAdd, in) && matchTags(toAdd, in)) {
                    int space = max - in.getCount();
                    int added = Math.min(stack.getCount(), space);
                    insertedAmt += added;
                    stack.setCount(stack.getCount() - added);
                    if (!simulate) {
                        in.setCount(in.getCount() + added);
                    }
                    if (stack.getCount() <= 0)
                        return insertedAmt;
                }
            }
        }
        return insertedAmt;
    }

    public static boolean hasInventorySpace(@Nonnull ItemStack stack, IItemHandler handler, int rangeMin, int rangeMax) {
        int size = stack.getCount();
        int max = stack.getMaxStackSize();
        for (int i = rangeMin; i < rangeMax && size > 0; i++) {
            ItemStack in = handler.getStackInSlot(i);
            if (in.isEmpty()) {
                size -= max;
            } else {
                if (stackEqualsNonNBT(stack, in) && matchTags(stack, in)) {
                    int space = max - in.getCount();
                    size -= space;
                }
            }
        }
        return size <= 0;
    }

    public static boolean stackEqualsNonNBT(@Nonnull ItemStack stack, @Nonnull ItemStack other) {
        if (stack.isEmpty() && other.isEmpty())
            return true;
        if (stack.isEmpty() || other.isEmpty())
            return false;
        Item sItem = stack.getItem();
        Item oItem = other.getItem();
        if (sItem.getHasSubtypes() || oItem.getHasSubtypes()) {
            return sItem.equals(other.getItem()) &&
                   (stack.getItemDamage() == other.getItemDamage() ||
                    stack.getItemDamage() == OreDictionary.WILDCARD_VALUE ||
                    other.getItemDamage() == OreDictionary.WILDCARD_VALUE);
        } else {
            return sItem.equals(other.getItem());
        }
    }

    public static boolean matchTags(@Nonnull ItemStack stack, @Nonnull ItemStack other) {
        return ItemStack.areItemStackTagsEqual(stack, other);
    }

    @Nonnull
    public static ItemStack copyStackWithSize(@Nonnull ItemStack stack, int amount) {
        if (stack.isEmpty() || amount <= 0) return ItemStack.EMPTY;
        ItemStack s = stack.copy();
        s.setCount(amount);
        return s;
    }

    /**
     * 向指定容器插入指定的物品，返回未插入的物品。
     *
     * @param external 容器
     * @param willBeInserted 要插入的物品
     * @return 未被插入的物品，如果全部插入，返回空物品
     */
    public static ItemStack insertItemStackToContainer(IItemHandler external, ItemStack willBeInserted) {
        ItemStack beInserted = willBeInserted;
        for (int externalSlotId = 0; externalSlotId < external.getSlots(); externalSlotId++) {
            ItemStack stackInSlot = external.getStackInSlot(externalSlotId);

            if (stackInSlot.isEmpty()) {
                ItemStack notInserted = external.insertItem(externalSlotId, beInserted, false);
                if (notInserted.isEmpty()) {
                    return ItemStack.EMPTY;
                } else {
                    beInserted = notInserted;
                    continue;
                }
            }

            if (matchStacks(stackInSlot, willBeInserted)) {
                ItemStack notInserted = external.insertItem(externalSlotId, beInserted, false);
                if (notInserted.isEmpty()) {
                    return ItemStack.EMPTY;
                } else {
                    beInserted = notInserted;
                }
            }
        }

        return beInserted;
    }

    public static Map<Integer, ItemStack> findItemsIndexedInInventoryFuel(IItemHandlerModifiable handler, @Nullable NBTTagCompound matchNBTTag) {
        Map<Integer, ItemStack> stacksOut = new IntObjectHashMap<>(handler.getSlots() * 2);
        for (int j = 0; j < handler.getSlots(); j++) {
            ItemStack s = handler.getStackInSlot(j);
            if (TileEntityFurnace.getItemBurnTime(s) > 0 && NBTMatchingHelper.matchNBTCompound(matchNBTTag, s.getTagCompound())) {
                stacksOut.put(j, s);
            }
        }
        return stacksOut;
    }

    public static Map<Integer, ItemStack> findItemsIndexedInInventoryOreDict(IItemHandlerModifiable handler, String oreDict, @Nullable NBTTagCompound matchNBTTag) {
        Map<Integer, ItemStack> stacksOut = new IntObjectHashMap<>(handler.getSlots() * 2);
        for (int j = 0; j < handler.getSlots(); j++) {
            ItemStack s = handler.getStackInSlot(j);
            if (s.isEmpty()) continue;
            int[] ids = OredictCache.getOreIDsFast(s);
            for (int id : ids) {
                if (OreDictionary.getOreName(id).equals(oreDict) && NBTMatchingHelper.matchNBTCompound(matchNBTTag, s.getTagCompound())) {
                    stacksOut.put(j, s);
                    break;
                }
            }
        }
        return stacksOut;
    }

    public static Map<Integer, ItemStack> findItemsIndexedInInventoryOreDict(IItemHandlerModifiable handler, String oreDict, AdvancedItemChecker itemChecker, TileMultiblockMachineController controller) {
        Map<Integer, ItemStack> stacksOut = new IntObjectHashMap<>(handler.getSlots() * 2);
        for (int j = 0; j < handler.getSlots(); j++) {
            ItemStack s = handler.getStackInSlot(j);
            if (s.isEmpty()) continue;
            int[] ids = OredictCache.getOreIDsFast(s);
            for (int id : ids) {
                if (OreDictionary.getOreName(id).equals(oreDict) && itemChecker.isMatch(controller, s)) {
                    stacksOut.put(j, s);
                    break;
                }
            }
        }
        return stacksOut;
    }

    public static Map<Integer, ItemStack> findItemsIndexedInInventory(IItemHandlerModifiable handler, ItemStack match, boolean strict, @Nullable NBTTagCompound matchNBTTag) {
        Map<Integer, ItemStack> stacksOut = new IntObjectHashMap<>(handler.getSlots() * 2);
        for (int j = 0; j < handler.getSlots(); j++) {
            ItemStack s = handler.getStackInSlot(j);
            if ((strict ? matchStacks(s, match) : matchStackLoosely(s, match)) && NBTMatchingHelper.matchNBTCompound(matchNBTTag, s.getTagCompound())) {
                stacksOut.put(j, s);
            }
        }
        return stacksOut;
    }

    public static Map<Integer, ItemStack> findItemsIndexedInInventory(IItemHandlerModifiable handler, ItemStack match, boolean strict, AdvancedItemChecker itemChecker, TileMultiblockMachineController controller) {
        Map<Integer, ItemStack> stacksOut = new IntObjectHashMap<>(handler.getSlots() * 2);
        for (int j = 0; j < handler.getSlots(); j++) {
            ItemStack s = handler.getStackInSlot(j);
            if ((strict ? matchStacks(s, match) : matchStackLoosely(s, match)) && itemChecker.isMatch(controller, s)) {
                stacksOut.put(j, s);
            }
        }
        return stacksOut;
    }

    public static boolean matchStacks(@Nonnull ItemStack stack, @Nonnull ItemStack other) {
        if (!ItemStack.areItemsEqual(stack, other)) return false;
        return ItemStack.areItemStackTagsEqual(stack, other);
    }

    public static boolean matchStackLoosely(@Nonnull ItemStack stack, @Nonnull ItemStack other) {
        if (stack.isEmpty()) return other.isEmpty();
        return OreDictionary.itemMatches(other, stack, false);
    }

    public static boolean stackNotInList(final List<ItemStack> list, final ItemStack stackFromBlockState) {
        for (final ItemStack stack : list) {
            if (matchStacks(stackFromBlockState, stack)) {
                return false;
            }
        }
        return true;
    }

    public static ItemStack getOredictItem(final RecipeCraftingContext context, final String oreDictName, final NBTTagCompound tag) {
        ItemStack stack = ItemStack.EMPTY;
        for (ItemStack oreInstance : OreDictionary.getOres(oreDictName)) {
            if (oreInstance.isEmpty()) {
                continue;
            }
            stack = copyStackWithSize(oreInstance, 1);

            if (!stack.isEmpty()) { //Try all options first..
                break;
            }
        }

        if (stack.isEmpty()) {
            throw new IllegalArgumentException("Unknown ItemStack: Cannot find an item in oredict '" + oreDictName + "'!");
        }

        if (tag != null) {
            stack.setTagCompound(tag.copy());
        }
        return stack;
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    public static List<ProcessingComponent<?>> copyItemHandlerComponents(final List<ProcessingComponent<?>> components) {
        List<ProcessingComponent<?>> list = new ArrayList<>();
        for (ProcessingComponent<?> component : components) {
            ProcessingComponent<Object> objectProcessingComponent = new ProcessingComponent<>(
                    (MachineComponent<Object>) component.component(),
                    ((IItemHandlerImpl) component.getProvidedComponent()).copy(),
                    component.getTag());
            list.add(objectProcessingComponent);
        }
        return list;
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    public static List<ProcessingComponent<?>> fastCopyItemHandlerComponents(final List<ProcessingComponent<?>> components) {
        List<ProcessingComponent<?>> list = new ArrayList<>();
        for (ProcessingComponent<?> component : components) {
            ProcessingComponent<Object> objectProcessingComponent = new ProcessingComponent<>(
                    (MachineComponent<Object>) component.component(),
                    ((IItemHandlerImpl) component.getProvidedComponent()).fastCopy(),
                    component.getTag());
            list.add(objectProcessingComponent);
        }
        return list;
    }
}
