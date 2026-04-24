package com.agguy.infiniteinventory.database.tests;

import com.agguy.infiniteinventory.database.DatabaseSearchWeight;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseSearchWeightTest {

    @Test
    void shouldHaveFourWeights() {
        assertEquals(4, DatabaseSearchWeight.values().length);
    }

    @Test
    void offShouldHaveZeroMultiplier() {
        assertEquals(0, DatabaseSearchWeight.OFF.multiplier());
    }

    @Test
    void highShouldHaveHigherMultiplierThanLow() {
        assertTrue(DatabaseSearchWeight.HIGH.multiplier() > DatabaseSearchWeight.LOW.multiplier());
    }

    @Test
    void mediumShouldHaveHigherMultiplierThanLow() {
        assertTrue(DatabaseSearchWeight.MEDIUM.multiplier() > DatabaseSearchWeight.LOW.multiplier());
    }

    @Test
    void eachWeightShouldHaveTranslationKey() {
        for (DatabaseSearchWeight weight : DatabaseSearchWeight.values()) {
            assertNotNull(weight.translationKey());
        }
    }
}
