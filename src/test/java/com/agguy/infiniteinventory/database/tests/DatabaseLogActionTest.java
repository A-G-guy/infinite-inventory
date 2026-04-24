package com.agguy.infiniteinventory.database.tests;

import com.agguy.infiniteinventory.database.DatabaseLogAction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DatabaseLogActionTest {

    @Test
    void shouldHaveFourActions() {
        assertEquals(4, DatabaseLogAction.values().length);
    }

    @Test
    void eachActionShouldHaveTranslationKey() {
        for (DatabaseLogAction action : DatabaseLogAction.values()) {
            assertNotNull(action.translationKey());
        }
    }

    @Test
    void normalizeShouldReturnDefaultForNull() {
        assertEquals(DatabaseLogAction.DEPOSIT, DatabaseLogAction.normalize(null));
    }

    @Test
    void normalizeShouldReturnSameForNonNull() {
        assertEquals(DatabaseLogAction.EXTRACT, DatabaseLogAction.normalize(DatabaseLogAction.EXTRACT));
    }

    @Test
    void readShouldReturnDefaultForNullString() {
        assertEquals(DatabaseLogAction.DEPOSIT, DatabaseLogAction.read(null));
    }

    @Test
    void readShouldReturnDefaultForBlankString() {
        assertEquals(DatabaseLogAction.DEPOSIT, DatabaseLogAction.read("   "));
    }

    @Test
    void readShouldReturnCorrectEnumForValidName() {
        assertEquals(DatabaseLogAction.DELETE, DatabaseLogAction.read("DELETE"));
        assertEquals(DatabaseLogAction.TRANSFER, DatabaseLogAction.read("TRANSFER"));
    }

    @Test
    void readShouldReturnDefaultForInvalidName() {
        assertEquals(DatabaseLogAction.DEPOSIT, DatabaseLogAction.read("INVALID_ACTION"));
    }
}
