package com.agguy.infiniteinventory.database.tests;

import com.agguy.infiniteinventory.database.StoredStackKey;
import com.agguy.infiniteinventory.tests.MinecraftTestBootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StoredStackKeyTest {
    static {
        MinecraftTestBootstrap.ensureBootstrapped();
    }

    @Test
    void toStackShouldClampRequestedCountToItemMaxStackSize() {
        StoredStackKey key = StoredStackKey.of(new ItemStack(Items.DIAMOND_SWORD));

        ItemStack extracted = key.toStack(32);

        assertEquals(1, extracted.getCount());
    }
}
