package github.kasuminova.mmce.common.upgrade;

import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.data.IData;
import crafttweaker.api.minecraft.CraftTweakerMC;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.helper.IFunction;
import net.minecraft.nbt.NBTTagCompound;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenGetter;
import stanhebben.zenscript.annotations.ZenSetter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@ZenRegister
@ZenClass("mods.modularmachinery.SimpleMachineUpgrade")
public class SimpleMachineUpgrade extends MachineUpgrade {
    private final List<String> descriptions = new ArrayList<>();
    private IFunction<SimpleMachineUpgrade, String[]> busGuiDescriptionHandler = null;
    private NBTTagCompound customData = new NBTTagCompound();

    public SimpleMachineUpgrade(final UpgradeType type) {
        super(type);
    }

    @Override
    public SimpleMachineUpgrade copy() {
        SimpleMachineUpgrade upgrade = new SimpleMachineUpgrade(getType());
        upgrade.descriptions.addAll(descriptions);
        upgrade.busGuiDescriptionHandler = busGuiDescriptionHandler;
        upgrade.eventProcessor.putAll(eventProcessor);
        return upgrade;
    }

    public void addDescription(String desc) {
        descriptions.add(desc);
    }

    public void setBusGUIDescriptionHandler(final IFunction<SimpleMachineUpgrade, String[]> handler) {
        this.busGuiDescriptionHandler = handler;
    }

    @ZenGetter("customData")
    public IData getCustomData() {
        return CraftTweakerMC.getIDataModifyable(customData);
    }

    @ZenSetter("customData")
    public void setCustomData(final IData customData) {
        this.customData = CraftTweakerMC.getNBTCompound(customData);
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
        return descriptions;
    }

    @Override
    public List<String> getBusGUIDescriptions() {
        return busGuiDescriptionHandler == null ? Collections.emptyList() : Arrays.asList(busGuiDescriptionHandler.apply(this));
    }
}
