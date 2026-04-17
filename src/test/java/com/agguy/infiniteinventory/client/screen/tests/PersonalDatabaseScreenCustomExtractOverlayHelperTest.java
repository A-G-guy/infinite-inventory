package com.agguy.infiniteinventory.client.screen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PersonalDatabaseScreenCustomExtractOverlayHelperTest {
    @Test
    void saturatedAmountShouldAcceptNormalValues() {
        assertEquals(0L, PersonalDatabaseScreenCustomExtractOverlayHelper.saturatedAmount(""));
        assertEquals(7L, PersonalDatabaseScreenCustomExtractOverlayHelper.saturatedAmount("7"));
        assertEquals(123456789L, PersonalDatabaseScreenCustomExtractOverlayHelper.saturatedAmount("123456789"));
    }

    @Test
    void saturatedAmountShouldClampOverflowToLongMaxValue() {
        assertEquals(
                Long.MAX_VALUE,
                PersonalDatabaseScreenCustomExtractOverlayHelper.saturatedAmount("9223372036854775808")
        );
        assertEquals(
                Long.MAX_VALUE,
                PersonalDatabaseScreenCustomExtractOverlayHelper.saturatedAmount("999999999999999999999999999999999999")
        );
    }

    @Test
    void saturatedAmountShouldRejectNonDecimalInput() {
        assertEquals(0L, PersonalDatabaseScreenCustomExtractOverlayHelper.saturatedAmount("12a3"));
        assertEquals(0L, PersonalDatabaseScreenCustomExtractOverlayHelper.saturatedAmount("-1"));
    }
}
