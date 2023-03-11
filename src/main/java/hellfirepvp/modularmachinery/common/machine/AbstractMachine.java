package hellfirepvp.modularmachinery.common.machine;

import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.data.Config;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;
import java.util.Objects;

public abstract class AbstractMachine {
    @Nonnull
    protected final ResourceLocation registryName;

    protected String localizedName = "";
    protected int definedColor = Config.machineColor;
    protected boolean requiresBlueprint = false;
    protected RecipeFailureActions failureAction = RecipeFailureActions.getFailureAction("still");

    public AbstractMachine(String registryName) {
        this.registryName = new ResourceLocation(ModularMachinery.MODID, registryName);
    }

    public RecipeFailureActions getFailureAction() {
        return failureAction;
    }

    public void setFailureAction(RecipeFailureActions failureAction) {
        this.failureAction = failureAction;
    }

    public void setRequiresBlueprint(boolean requiresBlueprint) {
        this.requiresBlueprint = requiresBlueprint;
    }

    public boolean requiresBlueprint() {
        return requiresBlueprint;
    }

    public String getLocalizedName() {
        String localizationKey = registryName.getNamespace() + "." + registryName.getPath();
        return I18n.hasKey(localizationKey) ? I18n.format(localizationKey) :
                localizedName != null ? localizedName : localizationKey;
    }

    public void setLocalizedName(String localizedName) {
        this.localizedName = localizedName;
    }

    public int getMachineColor() {
        return definedColor;
    }

    public void setDefinedColor(int definedColor) {
        this.definedColor = definedColor;
    }

    @Nonnull
    public ResourceLocation getRegistryName() {
        return registryName;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof AbstractMachine)) return false;
        AbstractMachine machine = (AbstractMachine) obj;
        return machine.registryName.toString().equals(registryName.toString());
    }

    @Override
    public int hashCode() {
        return Objects.hash(registryName);
    }
}
