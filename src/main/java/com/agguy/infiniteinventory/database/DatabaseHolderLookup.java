package com.agguy.infiniteinventory.database;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

final class DatabaseHolderLookup {
    private static final Logger LOGGER = LogManager.getLogger();

    @Nullable
    private static HolderLookup.Provider fallbackProvider;
    private static boolean fallbackResolved;

    private DatabaseHolderLookup() {
    }

    @Nullable
    static HolderLookup.Provider resolve(@Nullable HolderLookup.Provider provider) {
        if (provider != null) {
            return provider;
        }
        return fallbackProvider();
    }

    static HolderLookup.Provider require(@Nullable HolderLookup.Provider provider, String action) {
        HolderLookup.Provider resolvedProvider = resolve(provider);
        if (resolvedProvider != null) {
            return resolvedProvider;
        }
        throw new IllegalStateException("No HolderLookup.Provider available for " + action);
    }

    @Nullable
    private static synchronized HolderLookup.Provider fallbackProvider() {
        if (fallbackResolved) {
            return fallbackProvider;
        }
        try {
            fallbackProvider = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        } catch (RuntimeException exception) {
            LOGGER.warn("无法从 BuiltInRegistries 构建回退 HolderLookup.Provider", exception);
            fallbackProvider = null;
        }
        fallbackResolved = true;
        return fallbackProvider;
    }
}
