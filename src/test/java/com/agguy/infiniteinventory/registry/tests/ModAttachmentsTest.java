package com.agguy.infiniteinventory.registry.tests;

import com.agguy.infiniteinventory.InfiniteInventory;
import com.agguy.infiniteinventory.registry.ModAttachments;
import com.agguy.infiniteinventory.tests.MinecraftTestBootstrap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ModAttachmentsTest {
    static {
        MinecraftTestBootstrap.ensureBootstrapped();
    }

    @Test
    void registerShouldHaveCorrectModId() {
        assertEquals(InfiniteInventory.MODID, ModAttachments.REGISTER.getNamespace());
    }

    @Test
    void personalDatabaseAttachmentShouldBeRegistered() {
        assertNotNull(ModAttachments.PERSONAL_DATABASE);
        assertEquals("personal_database", ModAttachments.PERSONAL_DATABASE.getId().getPath());
    }

    @Test
    void databaseViewPreferencesAttachmentShouldBeRegistered() {
        assertNotNull(ModAttachments.DATABASE_VIEW_PREFERENCES);
        assertEquals("database_view_preferences", ModAttachments.DATABASE_VIEW_PREFERENCES.getId().getPath());
    }
}
