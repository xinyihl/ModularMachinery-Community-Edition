package hellfirepvp.modularmachinery.common.crafting.adapter.nco;

import hellfirepvp.modularmachinery.common.crafting.adapter.RecipeAdapter;
import net.minecraft.util.ResourceLocation;

public abstract class AdapterNCOMachine extends RecipeAdapter {
    public AdapterNCOMachine(ResourceLocation registryName) {
        super(registryName);
    }
}
