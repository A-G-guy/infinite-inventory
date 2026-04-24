package com.agguy.infiniteinventory;

import net.neoforged.fml.common.Mod;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class InfiniteInventoryTest {

    @Test
    void modIdShouldBeInfiniteInventory() {
        assertEquals("infiniteinventory", InfiniteInventory.MODID);
    }

    @Test
    void modAnnotationShouldBePresent() {
        Mod annotation = InfiniteInventory.class.getAnnotation(Mod.class);

        assertNotNull(annotation);
        assertEquals(InfiniteInventory.MODID, annotation.value());
    }
}
