package github.kasuminova.mmce.common.upgrade;

import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.data.IData;
import crafttweaker.api.minecraft.CraftTweakerMC;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.helper.IFunction;
import net.minecraft.nbt.NBTTagCompound;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenGetter;
import stanhebben.zenscript.annotations.ZenSetter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@ZenRegister
@ZenClass("mods.modularmachinery.SimpleDynamicMachineUpgrade")
public class SimpleDynamicMachineUpgrade extends DynamicMachineUpgrade {
    private IFunction<SimpleDynamicMachineUpgrade, String[]> descriptionHandler = null;
    private IFunction<SimpleDynamicMachineUpgrade, String[]> busGuiDescriptionHandler = null;

    private NBTTagCompound itemData = new NBTTagCompound();
    private NBTTagCompound customData = new NBTTagCompound();

    public SimpleDynamicMachineUpgrade(final UpgradeType type) {
        super(type);
    }

    @ZenGetter("itemData")
    public IData getItemData() {
        return CraftTweakerMC.getIDataModifyable(itemData);
    }

    @ZenGetter("customData")
    public IData getCustomData() {
        return CraftTweakerMC.getIDataModifyable(customData);
    }

    @ZenSetter("itemData")
    public void setItemData(final IData itemData) {
        this.itemData = CraftTweakerMC.getNBTCompound(itemData);
    }

    @ZenSetter("customData")
    public void setCustomData(final IData customData) {
        this.customData = CraftTweakerMC.getNBTCompound(customData);
    }

    public void setDescriptionHandler(final IFunction<SimpleDynamicMachineUpgrade, String[]> handler) {
        this.descriptionHandler = handler;
    }

    public void setBusGUIDescriptionHandler(final IFunction<SimpleDynamicMachineUpgrade, String[]> handler) {
        this.busGuiDescriptionHandler = handler;
    }

    @Override
    public void readItemNBT(final NBTTagCompound tag) {
        itemData = tag;
    }

    @Override
    public NBTTagCompound writeItemNBT() {
        return itemData;
    }

    @Override
    public void readNBT(final NBTTagCompound tag) {
        super.readNBT(tag);
        customData = tag;
    }

    @Override
    public NBTTagCompound writeNBT() {
        return customData;
    }

    @Override
    public List<String> getDescriptions() {
        return descriptionHandler == null ? Collections.emptyList() : Arrays.asList(descriptionHandler.apply(this));
    }

    @Override
    public List<String> getBusGUIDescriptions() {
        return busGuiDescriptionHandler == null ? Collections.emptyList() : Arrays.asList(busGuiDescriptionHandler.apply(this));
    }

    @Override
    public SimpleDynamicMachineUpgrade copy() {
        SimpleDynamicMachineUpgrade upgrade = new SimpleDynamicMachineUpgrade(getType());
        upgrade.descriptionHandler = descriptionHandler;
        upgrade.busGuiDescriptionHandler = busGuiDescriptionHandler;
        upgrade.eventProcessor.putAll(eventProcessor);
        return upgrade;
    }
}
