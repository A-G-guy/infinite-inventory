package com.agguy.infiniteinventory.item.tests;

import com.agguy.infiniteinventory.InfiniteInventory;
import com.agguy.infiniteinventory.registry.ModItems;
import com.agguy.infiniteinventory.tests.MinecraftTestBootstrap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DatabaseAccessItemTest {
    static {
        MinecraftTestBootstrap.ensureBootstrapped();
    }

    @Test
    void databaseAccessItemShouldBeRegistered() {
        assertNotNull(ModItems.DATABASE_ACCESS_ITEM);
        assertEquals("database_access_item", ModItems.DATABASE_ACCESS_ITEM.getId().getPath());
        assertEquals(InfiniteInventory.MODID, ModItems.DATABASE_ACCESS_ITEM.getId().getNamespace());
    }

    @Test
    void databaseAccessItemShouldBeDeferredItem() {
        assertNotNull(ModItems.DATABASE_ACCESS_ITEM);
    }
}
