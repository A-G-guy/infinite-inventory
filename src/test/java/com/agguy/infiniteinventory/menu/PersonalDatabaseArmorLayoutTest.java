package com.agguy.infiniteinventory.menu;

import net.minecraft.world.entity.EquipmentSlot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PersonalDatabaseArmorLayoutTest {
    @Test
    void armorOrderShouldRunFromHeadToFeet() {
        assertArrayEquals(
                new EquipmentSlot[] {
                        EquipmentSlot.HEAD,
                        EquipmentSlot.CHEST,
                        EquipmentSlot.LEGS,
                        EquipmentSlot.FEET
                },
                PersonalDatabaseMenuSupport.ARMOR_ORDER
        );
    }

    @Test
    void armorHelpersShouldMatchDisplayedOrderAndPlayerInventoryIndexes() {
        assertEquals(0, PersonalDatabaseMenuSupport.armorSlotOffset(EquipmentSlot.HEAD));
        assertEquals(1, PersonalDatabaseMenuSupport.armorSlotOffset(EquipmentSlot.CHEST));
        assertEquals(2, PersonalDatabaseMenuSupport.armorSlotOffset(EquipmentSlot.LEGS));
        assertEquals(3, PersonalDatabaseMenuSupport.armorSlotOffset(EquipmentSlot.FEET));

        assertEquals(39, PersonalDatabaseMenuSupport.armorInventoryIndex(EquipmentSlot.HEAD));
        assertEquals(38, PersonalDatabaseMenuSupport.armorInventoryIndex(EquipmentSlot.CHEST));
        assertEquals(37, PersonalDatabaseMenuSupport.armorInventoryIndex(EquipmentSlot.LEGS));
        assertEquals(36, PersonalDatabaseMenuSupport.armorInventoryIndex(EquipmentSlot.FEET));
    }
}
