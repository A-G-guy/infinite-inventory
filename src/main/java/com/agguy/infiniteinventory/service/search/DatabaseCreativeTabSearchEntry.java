package com.agguy.infiniteinventory.service.search;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;

record DatabaseCreativeTabSearchEntry(
        CreativeModeTab tab,
        String registryNameNormalized,
        String registryNameCompact,
        String registryPathNormalized,
        String registryPathCompact,
        String displayNameNormalized,
        String displayNameCompact
) {
    static DatabaseCreativeTabSearchEntry of(CreativeModeTab tab, ResourceLocation tabId) {
        String displayName = tab.getDisplayName().getString();
        String registryName = tabId == null ? "" : tabId.toString();
        String registryPath = tabId == null ? "" : tabId.getPath();
        return new DatabaseCreativeTabSearchEntry(
                tab,
                SearchTextNormalizer.normalizeIdentifierText(registryName),
                SearchTextNormalizer.compactIdentifierText(registryName),
                SearchTextNormalizer.normalizeIdentifierText(registryPath),
                SearchTextNormalizer.compactIdentifierText(registryPath),
                SearchTextNormalizer.normalizeNaturalText(displayName),
                SearchTextNormalizer.compactNaturalText(displayName)
        );
    }
}
