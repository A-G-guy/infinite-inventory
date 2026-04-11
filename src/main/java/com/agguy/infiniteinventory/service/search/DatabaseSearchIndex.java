package com.agguy.infiniteinventory.service.search;

import com.agguy.infiniteinventory.database.StoredStackKey;
import java.util.List;

public record DatabaseSearchIndex(
        String displayName,
        String displayNameNormalized,
        String displayNameCompact,
        List<String> displayNameTokens,
        String registryNameNormalized,
        String registryNameCompact,
        String registryPathNormalized,
        String registryPathCompact,
        List<String> registryPathTokens,
        String modNamespace,
        String pinyinFull,
        String pinyinInitials,
        List<String> pinyinTokens
) {
    public DatabaseSearchIndex {
        displayName = displayName == null ? "" : displayName;
        displayNameNormalized = displayNameNormalized == null ? "" : displayNameNormalized;
        displayNameCompact = displayNameCompact == null ? "" : displayNameCompact;
        displayNameTokens = List.copyOf(displayNameTokens);
        registryNameNormalized = registryNameNormalized == null ? "" : registryNameNormalized;
        registryNameCompact = registryNameCompact == null ? "" : registryNameCompact;
        registryPathNormalized = registryPathNormalized == null ? "" : registryPathNormalized;
        registryPathCompact = registryPathCompact == null ? "" : registryPathCompact;
        registryPathTokens = List.copyOf(registryPathTokens);
        modNamespace = modNamespace == null ? "" : modNamespace;
        pinyinFull = pinyinFull == null ? "" : pinyinFull;
        pinyinInitials = pinyinInitials == null ? "" : pinyinInitials;
        pinyinTokens = List.copyOf(pinyinTokens);
    }

    public static DatabaseSearchIndex of(StoredStackKey key, String displayName, PinyinIndexData pinyinIndexData) {
        return new DatabaseSearchIndex(
                displayName,
                SearchTextNormalizer.normalizeNaturalText(displayName),
                SearchTextNormalizer.compactNaturalText(displayName),
                SearchTextNormalizer.tokenizeNaturalText(displayName),
                SearchTextNormalizer.normalizeIdentifierText(key.registryName()),
                SearchTextNormalizer.compactIdentifierText(key.registryName()),
                SearchTextNormalizer.normalizeIdentifierText(key.registryPath()),
                SearchTextNormalizer.compactIdentifierText(key.registryPath()),
                SearchTextNormalizer.tokenizeIdentifierText(key.registryPath()),
                SearchTextNormalizer.normalizeIdentifierText(key.registryNamespace()),
                pinyinIndexData.fullPinyin(),
                pinyinIndexData.initials(),
                pinyinIndexData.tokens()
        );
    }
}
