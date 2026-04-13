package com.agguy.infiniteinventory.service;

import com.agguy.infiniteinventory.service.search.DatabaseSearchRanking;

public record DatabaseSortSnapshot(
        String displayNameNormalized,
        String registryName,
        String registryNamespace,
        String registryPath,
        long amount,
        long firstAdded,
        long lastModified,
        int stackHash,
        DatabaseSearchRanking searchRanking
) {
    public DatabaseSortSnapshot {
        displayNameNormalized = displayNameNormalized == null ? "" : displayNameNormalized;
        registryName = registryName == null ? "" : registryName;
        registryNamespace = registryNamespace == null ? "" : registryNamespace;
        registryPath = registryPath == null ? "" : registryPath;
        amount = Math.max(0L, amount);
        firstAdded = Math.max(0L, firstAdded);
        lastModified = Math.max(0L, lastModified);
        searchRanking = searchRanking == null ? DatabaseSearchRanking.unfiltered() : searchRanking;
    }

    public DatabaseSortSnapshot withSearchRanking(DatabaseSearchRanking ranking) {
        return new DatabaseSortSnapshot(
                this.displayNameNormalized,
                this.registryName,
                this.registryNamespace,
                this.registryPath,
                this.amount,
                this.firstAdded,
                this.lastModified,
                this.stackHash,
                ranking
        );
    }
}
