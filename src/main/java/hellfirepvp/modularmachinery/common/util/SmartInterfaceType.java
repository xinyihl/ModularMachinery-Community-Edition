package hellfirepvp.modularmachinery.common.util;

import crafttweaker.annotations.ZenRegister;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenGetter;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenRegister
@ZenClass("mods.modularmachinery.SmartInterfaceType")
public class SmartInterfaceType {
    private final String type;
    private final float defaultValue;
    private String headerInfo = "";
    private String valueInfo = "";
    private String footerInfo = "";
    private String notEqualMessage = "";
    private String jeiTooltip = "";
    private int jeiTooltipArgsCount = 2;

    public SmartInterfaceType(String type, float defaultValue) {
        this.type = type;
        this.defaultValue = defaultValue;
    }

    @ZenMethod
    public static SmartInterfaceType create(String type, float defaultValue) {
        return new SmartInterfaceType(type, defaultValue);
    }

    @ZenGetter("type")
    public String getType() {
        return type;
    }

    @ZenGetter("defaultValue")
    public float getDefaultValue() {
        return defaultValue;
    }

    @ZenGetter("headerInfo")
    public String getHeaderInfo() {
        return headerInfo;
    }

    @ZenGetter("valueInfo")
    public String getValueInfo() {
        return valueInfo;
    }

    @ZenGetter("footerInfo")
    public String getFooterInfo() {
        return footerInfo;
    }

    @ZenGetter("notEqualMessage")
    public String getNotEqualMessage() {
        return notEqualMessage;
    }

    @ZenGetter("jeiTooltip")
    public String getJeiTooltip() {
        return jeiTooltip;
    }

    @ZenGetter("jeiTooltipArgsCount")
    public int getJeiTooltipArgsCount() {
        return jeiTooltipArgsCount;
    }

    @ZenMethod
    public SmartInterfaceType setHeaderInfo(String headerInfo) {
        this.headerInfo = headerInfo;
        return this;
    }

    @ZenMethod
    public SmartInterfaceType setValueInfo(String valueInfo) {
        this.valueInfo = valueInfo;
        return this;
    }

    @ZenMethod
    public SmartInterfaceType setFooterInfo(String footerInfo) {
        this.footerInfo = footerInfo;
        return this;
    }

    @ZenMethod
    public SmartInterfaceType setNotEqualMessage(String notEqualMessage) {
        this.notEqualMessage = notEqualMessage;
        return this;
    }

    @ZenMethod
    public SmartInterfaceType setJeiDescription(String jeiTooltip, int argsCount) {
        this.jeiTooltip = jeiTooltip;
        this.jeiTooltipArgsCount = argsCount;
        return this;
    }
}
