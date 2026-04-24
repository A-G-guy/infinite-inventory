package com.agguy.infiniteinventory.service.search.tests;

import com.agguy.infiniteinventory.service.search.DatabaseSearchEnvironmentSignature;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DatabaseSearchEnvironmentSignatureTest {

    @Test
    void shouldPreserveFeatureHash() {
        DatabaseSearchEnvironmentSignature sig = new DatabaseSearchEnvironmentSignature(12345, true);
        assertEquals(12345, sig.featureHash());
    }

    @Test
    void shouldPreservePermissionsFlag() {
        DatabaseSearchEnvironmentSignature sig = new DatabaseSearchEnvironmentSignature(0, true);
        assertEquals(true, sig.hasPermissions());

        DatabaseSearchEnvironmentSignature sig2 = new DatabaseSearchEnvironmentSignature(0, false);
        assertEquals(false, sig2.hasPermissions());
    }

    @Test
    void shouldSupportNegativeHash() {
        DatabaseSearchEnvironmentSignature sig = new DatabaseSearchEnvironmentSignature(-1, false);
        assertEquals(-1, sig.featureHash());
    }
}
