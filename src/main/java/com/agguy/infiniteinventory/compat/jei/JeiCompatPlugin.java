package com.agguy.infiniteinventory.compat.jei;

import com.agguy.infiniteinventory.InfiniteInventory;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;

/**
 * JEI 插件入口。
 */
@JeiPlugin
public final class JeiCompatPlugin implements IModPlugin {
    private static IJeiRuntime runtime;

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(InfiniteInventory.MODID, "plugin");
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }

    static IJeiRuntime runtime() {
        return runtime;
    }
}
