package hellfirepvp.modularmachinery.common.util;

public class SmartInterfaceType {
    private final String type;
    private final float defaultValue;
    private String headerInfo = "";
    private String footerInfo = "";

    public SmartInterfaceType(String type, float defaultValue, String headerInfo, String footerInfo) {
        this.type = type;
        this.defaultValue = defaultValue;
        this.headerInfo = headerInfo;
        this.footerInfo = footerInfo;
    }

    public SmartInterfaceType(String type, float defaultValue) {
        this.type = type;
        this.defaultValue = defaultValue;
    }

    public String getType() {
        return type;
    }

    public float getDefaultValue() {
        return defaultValue;
    }

    public String getHeaderInfo() {
        return headerInfo;
    }

    public String getFooterInfo() {
        return footerInfo;
    }

    public void setHeaderInfo(String headerInfo) {
        this.headerInfo = headerInfo;
    }

    public void setFooterInfo(String footerInfo) {
        this.footerInfo = footerInfo;
    }
}
