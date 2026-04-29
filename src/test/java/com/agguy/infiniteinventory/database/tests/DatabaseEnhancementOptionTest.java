package com.agguy.infiniteinventory.database.tests;

import com.agguy.infiniteinventory.database.DatabaseEnhancementOption;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DatabaseEnhancementOptionTest {

    @Test
    void shouldHaveThreeOptions() {
        assertEquals(3, DatabaseEnhancementOption.values().length);
    }

    @Test
    void eachOptionShouldHaveTranslationKey() {
        for (DatabaseEnhancementOption option : DatabaseEnhancementOption.values()) {
            assertNotNull(option.translationKey());
        }
    }

    @Test
    void orderedValuesShouldMatchDeclarationOrder() {
        var ordered = DatabaseEnhancementOption.orderedValues();
        assertEquals(3, ordered.size());
        assertEquals(DatabaseEnhancementOption.AUTO_STORE_PICKED_UP_ITEMS, ordered.get(0));
        assertEquals(DatabaseEnhancementOption.SHOW_AMOUNT_IN_TOOLTIP, ordered.get(1));
        assertEquals(DatabaseEnhancementOption.FORCE_SAVE_ON_CRITICAL_MUTATION, ordered.get(2));
    }
}
