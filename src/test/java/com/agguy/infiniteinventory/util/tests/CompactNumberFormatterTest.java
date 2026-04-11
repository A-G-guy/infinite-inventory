package com.agguy.infiniteinventory.util.tests;

import com.agguy.infiniteinventory.util.CompactNumberFormatter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CompactNumberFormatterTest {
    @Test
    void formatterShouldKeepSmallNumbersReadable() {
        assertEquals("999", CompactNumberFormatter.format(999));
        assertEquals("1.0K", CompactNumberFormatter.format(1_000));
        assertEquals("15K", CompactNumberFormatter.format(15_000));
        assertEquals("2.5M", CompactNumberFormatter.format(2_500_000));
        assertEquals("3.4B", CompactNumberFormatter.format(3_400_000_000L));
    }
}
