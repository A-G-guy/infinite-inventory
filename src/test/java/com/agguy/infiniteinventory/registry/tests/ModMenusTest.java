package com.agguy.infiniteinventory.registry.tests;

import com.agguy.infiniteinventory.InfiniteInventory;
import com.agguy.infiniteinventory.registry.ModMenus;
import com.agguy.infiniteinventory.tests.MinecraftTestBootstrap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ModMenusTest {
    static {
        MinecraftTestBootstrap.ensureBootstrapped();
    }

    @Test
    void registerShouldHaveCorrectModId() {
        assertEquals(InfiniteInventory.MODID, ModMenus.REGISTER.getNamespace());
    }

    @Test
    void personalDatabaseMenuShouldBeRegistered() {
        assertNotNull(ModMenus.PERSONAL_DATABASE_MENU);
        assertEquals("personal_database", ModMenus.PERSONAL_DATABASE_MENU.getId().getPath());
    }
}
