package com.agguy.infiniteinventory.database.tests;

import com.agguy.infiniteinventory.database.DatabaseSelectionEntry;
import com.agguy.infiniteinventory.tests.MinecraftTestBootstrap;
import java.util.LinkedHashSet;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class DatabaseSelectionEntryTest {
    static {
        MinecraftTestBootstrap.ensureBootstrapped();
    }

    @Test
    void equalEntriesShouldCollapseInLinkedHashSet() {
        DatabaseSelectionEntry first = new DatabaseSelectionEntry("ore_tab", new ItemStack(Items.DIAMOND, 32));
        DatabaseSelectionEntry duplicate = new DatabaseSelectionEntry("ore_tab", new ItemStack(Items.DIAMOND, 1));
        LinkedHashSet<DatabaseSelectionEntry> selectedEntries = new LinkedHashSet<>();

        selectedEntries.add(first);
        selectedEntries.add(duplicate);

        assertEquals(1, selectedEntries.size());
    }

    @Test
    void differentSourceTabsShouldRemainDistinct() {
        DatabaseSelectionEntry first = new DatabaseSelectionEntry("ore_tab", new ItemStack(Items.DIAMOND, 1));
        DatabaseSelectionEntry second = new DatabaseSelectionEntry("gem_tab", new ItemStack(Items.DIAMOND, 1));

        assertNotEquals(first, second);
    }
}
