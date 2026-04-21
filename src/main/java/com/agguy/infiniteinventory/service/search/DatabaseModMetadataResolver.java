package com.agguy.infiniteinventory.service.search;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModInfo;

public final class DatabaseModMetadataResolver {
    public static final DatabaseModMetadataResolver INSTANCE = new DatabaseModMetadataResolver();

    private final Map<String, ModMetadata> metadataByNamespace = new ConcurrentHashMap<>();
    private volatile Set<String> modDisplayNameCandidates = Set.of();

    private DatabaseModMetadataResolver() {
    }

    public String displayNameForNamespace(String namespace) {
        String normalizedNamespace = SearchTextNormalizer.normalizeIdentifierText(namespace);
        if (normalizedNamespace.isEmpty()) {
            return "";
        }
        this.ensureLoaded();
        return this.metadataByNamespace.getOrDefault(normalizedNamespace, ModMetadata.fallback(normalizedNamespace)).displayName();
    }

    public Set<String> modDisplayNameCandidates() {
        this.ensureLoaded();
        return this.modDisplayNameCandidates;
    }

    private void ensureLoaded() {
        if (!this.metadataByNamespace.isEmpty()) {
            return;
        }
        synchronized (this.metadataByNamespace) {
            if (!this.metadataByNamespace.isEmpty()) {
                return;
            }
            ModList modList = ModList.get();
            if (modList == null) {
                this.modDisplayNameCandidates = Set.of();
                return;
            }

            LinkedHashSet<String> candidates = new LinkedHashSet<>();
            for (IModInfo modInfo : modList.getMods()) {
                if (modInfo == null) {
                    continue;
                }
                String namespace = SearchTextNormalizer.normalizeIdentifierText(modInfo.getNamespace());
                if (namespace.isEmpty()) {
                    namespace = SearchTextNormalizer.normalizeIdentifierText(modInfo.getModId());
                }
                if (namespace.isEmpty()) {
                    continue;
                }
                ModMetadata metadata = ModMetadata.of(namespace, modInfo.getDisplayName());
                this.metadataByNamespace.putIfAbsent(namespace, metadata);
                if (!metadata.displayNameNormalized().isEmpty()) {
                    candidates.add(metadata.displayNameNormalized());
                }
            }
            this.modDisplayNameCandidates = Set.copyOf(candidates);
        }
    }

    private record ModMetadata(
            String namespace,
            String displayName,
            String displayNameNormalized
    ) {
        private static ModMetadata of(String namespace, String displayName) {
            String resolvedNamespace = SearchTextNormalizer.normalizeIdentifierText(namespace);
            String resolvedDisplayName = displayName == null || displayName.isBlank() ? resolvedNamespace : displayName.trim();
            return new ModMetadata(
                    resolvedNamespace,
                    resolvedDisplayName,
                    SearchTextNormalizer.normalizeNaturalText(resolvedDisplayName)
            );
        }

        private static ModMetadata fallback(String namespace) {
            return of(namespace, namespace);
        }
    }
}
