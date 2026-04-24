package com.agguy.infiniteinventory.service.search.tests;

import com.agguy.infiniteinventory.service.search.DatabaseSearchEnvironment;
import com.agguy.infiniteinventory.service.search.DatabaseSearchEnvironmentSignature;
import com.agguy.infiniteinventory.tests.MinecraftTestBootstrap;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseSearchEnvironmentTest {
    static {
        MinecraftTestBootstrap.ensureBootstrapped();
    }

    @Test
    void defaultEnvironmentShouldBeAvailable() {
        DatabaseSearchEnvironment env = DatabaseSearchEnvironment.defaultEnvironment();

        assertNotNull(env);
        assertEquals(FeatureFlags.DEFAULT_FLAGS, env.enabledFeatures());
        assertNotNull(env.registryAccess());
    }

    @Test
    void shouldHandleNullArguments() {
        DatabaseSearchEnvironment env = new DatabaseSearchEnvironment(null, true, null);

        assertEquals(FeatureFlags.DEFAULT_FLAGS, env.enabledFeatures());
        assertEquals(true, env.hasPermissions());
        assertNotNull(env.registryAccess());
    }

    @Test
    void signatureShouldReflectState() {
        DatabaseSearchEnvironment env = new DatabaseSearchEnvironment(
                FeatureFlags.DEFAULT_FLAGS, true,
                RegistryAccess.fromRegistryOfRegistries(net.minecraft.core.registries.BuiltInRegistries.REGISTRY));

        DatabaseSearchEnvironmentSignature sig = env.signature();

        assertNotNull(sig);
        assertEquals(env.enabledFeatures().hashCode(), sig.featureHash());
        assertEquals(env.hasPermissions(), sig.hasPermissions());
    }

    @Test
    void differentPermissionsShouldProduceDifferentSignature() {
        DatabaseSearchEnvironment env1 = new DatabaseSearchEnvironment(
                FeatureFlags.DEFAULT_FLAGS, true, null);
        DatabaseSearchEnvironment env2 = new DatabaseSearchEnvironment(
                FeatureFlags.DEFAULT_FLAGS, false, null);

        assertEquals(false, env2.signature().hasPermissions());
    }
}
