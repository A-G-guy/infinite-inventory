package com.agguy.infiniteinventory.client.screen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersonalDatabaseScreenFitProfileTest {
    @Test
    void practicalMinimumShouldResolveToCompactProfile() {
        PersonalDatabaseScreenFitProfile fitProfile = PersonalDatabaseScreenFitProfile.resolve(480, 270);

        assertEquals(PersonalDatabaseScreenFitProfile.COMPACT, fitProfile);
        assertTrue(fitProfile.supportsFullUi());
        assertEquals(1, fitProfile.maxVisiblePanels());
    }

    @Test
    void desktopSizedGuiShouldResolveToStandardProfile() {
        PersonalDatabaseScreenFitProfile fitProfile = PersonalDatabaseScreenFitProfile.resolve(640, 360);

        assertEquals(PersonalDatabaseScreenFitProfile.STANDARD, fitProfile);
        assertTrue(fitProfile.supportsFullUi());
        assertEquals(4, fitProfile.maxVisiblePanels());
    }

    @Test
    void smallerThanPracticalMinimumShouldUseSafeFallbackProfile() {
        PersonalDatabaseScreenFitProfile fitProfile = PersonalDatabaseScreenFitProfile.resolve(479, 269);

        assertEquals(PersonalDatabaseScreenFitProfile.UNSUPPORTED, fitProfile);
        assertFalse(fitProfile.supportsFullUi());
        assertEquals(1, fitProfile.maxVisiblePanels());
    }
}
