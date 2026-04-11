package com.agguy.infiniteinventory.database;

public enum DatabaseSearchWeight {
    OFF("screen.infiniteinventory.search_weight.off", 0, 0),
    LOW("screen.infiniteinventory.search_weight.low", 1, 1),
    MEDIUM("screen.infiniteinventory.search_weight.medium", 2, 2),
    HIGH("screen.infiniteinventory.search_weight.high", 3, 3);

    private final String translationKey;
    private final int rank;
    private final int multiplier;

    DatabaseSearchWeight(String translationKey, int rank, int multiplier) {
        this.translationKey = translationKey;
        this.rank = rank;
        this.multiplier = multiplier;
    }

    public String translationKey() {
        return this.translationKey;
    }

    public int rank() {
        return this.rank;
    }

    public int multiplier() {
        return this.multiplier;
    }
}
