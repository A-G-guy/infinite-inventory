package com.agguy.infiniteinventory.registry.tests;

import com.agguy.infiniteinventory.InfiniteInventory;
import com.agguy.infiniteinventory.registry.ModItems;
import com.agguy.infiniteinventory.tests.MinecraftTestBootstrap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ModItemsTest {
    static {
        MinecraftTestBootstrap.ensureBootstrapped();
    }

    @Test
    void registerShouldHaveCorrectModId() {
        assertEquals(InfiniteInventory.MODID, ModItems.REGISTER.getNamespace());
    }

    @Test
    void databaseAccessItemShouldBeRegistered() {
        assertNotNull(ModItems.DATABASE_ACCESS_ITEM);
        assertEquals("database_access_item", ModItems.DATABASE_ACCESS_ITEM.getId().getPath());
    }
}
