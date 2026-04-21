package com.agguy.infiniteinventory.service.search;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;

public record DatabaseSearchEnvironment(
        FeatureFlagSet enabledFeatures,
        boolean hasPermissions,
        RegistryAccess registryAccess
) {
    private static final RegistryAccess DEFAULT_REGISTRY_ACCESS = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    private static final DatabaseSearchEnvironment DEFAULT = new DatabaseSearchEnvironment(
            FeatureFlags.DEFAULT_FLAGS,
            false,
            DEFAULT_REGISTRY_ACCESS
    );

    public DatabaseSearchEnvironment {
        enabledFeatures = enabledFeatures == null ? FeatureFlags.DEFAULT_FLAGS : enabledFeatures;
        registryAccess = registryAccess == null ? DEFAULT_REGISTRY_ACCESS : registryAccess;
    }

    public static DatabaseSearchEnvironment defaultEnvironment() {
        return DEFAULT;
    }

    public DatabaseSearchEnvironmentSignature signature() {
        return new DatabaseSearchEnvironmentSignature(this.enabledFeatures.hashCode(), this.hasPermissions);
    }
}
