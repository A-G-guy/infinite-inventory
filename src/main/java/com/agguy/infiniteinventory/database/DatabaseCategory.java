package com.agguy.infiniteinventory.database;

public enum DatabaseCategory {
    ALL("screen.infiniteinventory.category.all"),
    BLOCKS("screen.infiniteinventory.category.blocks"),
    TOOLS_WEAPONS("screen.infiniteinventory.category.tools_weapons"),
    EQUIPMENT("screen.infiniteinventory.category.equipment"),
    CONSUMABLES("screen.infiniteinventory.category.consumables"),
    MATERIALS("screen.infiniteinventory.category.materials"),
    OTHER("screen.infiniteinventory.category.other");

    private final String translationKey;

    DatabaseCategory(String translationKey) {
        this.translationKey = translationKey;
    }

    public String translationKey() {
        return this.translationKey;
    }
}
