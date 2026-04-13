package com.agguy.infiniteinventory.database;

import java.util.List;

public enum DatabaseEnhancementOption {
    AUTO_STORE_PICKED_UP_ITEMS("screen.infiniteinventory.enhancement.option.auto_store_picked_up_items");

    private static final List<DatabaseEnhancementOption> ORDERED_VALUES = List.of(values());

    private final String translationKey;

    DatabaseEnhancementOption(String translationKey) {
        this.translationKey = translationKey;
    }

    public String translationKey() {
        return this.translationKey;
    }

    public static List<DatabaseEnhancementOption> orderedValues() {
        return ORDERED_VALUES;
    }
}
