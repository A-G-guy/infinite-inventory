package com.agguy.infiniteinventory.database.tests;

import com.agguy.infiniteinventory.database.DatabasePageEntry;
import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.StoredStackKey;
import com.agguy.infiniteinventory.database.VisibleDatabaseEntry;
import com.agguy.infiniteinventory.tests.MinecraftTestBootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DatabasePageEntryTest {
    static {
        MinecraftTestBootstrap.ensureBootstrapped();
    }

    @Test
    void shouldPreserveKeyAndView() throws Exception {
        ItemStack stack = new ItemStack(Items.DIAMOND, 1);
        StoredStackKey key = DatabaseTestReflectionHelper.fakeKey("minecraft:diamond", stack);
        VisibleDatabaseEntry view = new VisibleDatabaseEntry(
                DatabaseScope.PERSONAL, stack, 64, "__default", "minecraft:diamond",
                "", false);

        DatabasePageEntry entry = new DatabasePageEntry(key, view);

        assertNotNull(entry.key());
        assertNotNull(entry.view());
        assertEquals(key, entry.key());
        assertEquals(view, entry.view());
    }

    @Test
    void shouldAllowNullValues() {
        DatabasePageEntry entry = new DatabasePageEntry(null, null);

        assertEquals(null, entry.key());
        assertEquals(null, entry.view());
    }
}
