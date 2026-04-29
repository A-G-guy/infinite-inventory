package com.agguy.infiniteinventory.compat.jei;

import com.agguy.infiniteinventory.InfiniteInventory;
import java.util.concurrent.atomic.AtomicReference;
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
    private static final AtomicReference<IJeiRuntime> RUNTIME = new AtomicReference<>();

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(InfiniteInventory.MODID, "plugin");
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        RUNTIME.set(jeiRuntime);
    }

    static IJeiRuntime runtime() {
        return RUNTIME.get();
    }
}
