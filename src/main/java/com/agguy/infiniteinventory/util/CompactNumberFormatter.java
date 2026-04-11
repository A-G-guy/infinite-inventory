package com.agguy.infiniteinventory.util;

import java.util.Locale;

public final class CompactNumberFormatter {
    private CompactNumberFormatter() {
    }

    public static String format(long value) {
        if (value < 1_000L) {
            return Long.toString(value);
        }
        if (value < 1_000_000L) {
            return compact(value, 1_000D, "K");
        }
        if (value < 1_000_000_000L) {
            return compact(value, 1_000_000D, "M");
        }
        return compact(value, 1_000_000_000D, "B");
    }

    private static String compact(long value, double divisor, String suffix) {
        double scaled = value / divisor;
        if (scaled >= 10D) {
            return String.format(Locale.ROOT, "%.0f%s", scaled, suffix);
        }
        return String.format(Locale.ROOT, "%.1f%s", scaled, suffix);
    }
}
