package com.agguy.infiniteinventory.service.search;

import com.agguy.infiniteinventory.database.StoredStackKey;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public record DatabaseSearchIndex(
        String displayName,
        String displayNameNormalized,
        String displayNameCompact,
        List<String> displayNameTokens,
        List<String> displayNameSearchNormalizedTexts,
        List<String> displayNameSearchCompactTexts,
        String registryNameNormalized,
        String registryNameCompact,
        String registryPathNormalized,
        String registryPathCompact,
        List<String> registryPathTokens,
        String modNamespace,
        String pinyinFull,
        String pinyinInitials,
        List<String> pinyinTokens,
        String note,
        String noteNormalized,
        String noteCompact,
        List<String> noteTokens,
        List<String> noteSearchNormalizedTexts,
        List<String> noteSearchCompactTexts
) {
    public DatabaseSearchIndex {
        displayName = displayName == null ? "" : displayName;
        displayNameNormalized = displayNameNormalized == null ? "" : displayNameNormalized;
        displayNameCompact = displayNameCompact == null ? "" : displayNameCompact;
        displayNameTokens = List.copyOf(displayNameTokens);
        displayNameSearchNormalizedTexts = List.copyOf(displayNameSearchNormalizedTexts);
        displayNameSearchCompactTexts = List.copyOf(displayNameSearchCompactTexts);
        registryNameNormalized = registryNameNormalized == null ? "" : registryNameNormalized;
        registryNameCompact = registryNameCompact == null ? "" : registryNameCompact;
        registryPathNormalized = registryPathNormalized == null ? "" : registryPathNormalized;
        registryPathCompact = registryPathCompact == null ? "" : registryPathCompact;
        registryPathTokens = List.copyOf(registryPathTokens);
        modNamespace = modNamespace == null ? "" : modNamespace;
        pinyinFull = pinyinFull == null ? "" : pinyinFull;
        pinyinInitials = pinyinInitials == null ? "" : pinyinInitials;
        pinyinTokens = List.copyOf(pinyinTokens);
        note = note == null ? "" : note;
        noteNormalized = noteNormalized == null ? "" : noteNormalized;
        noteCompact = noteCompact == null ? "" : noteCompact;
        noteTokens = List.copyOf(noteTokens);
        noteSearchNormalizedTexts = List.copyOf(noteSearchNormalizedTexts);
        noteSearchCompactTexts = List.copyOf(noteSearchCompactTexts);
    }

    public static DatabaseSearchIndex of(
            StoredStackKey key,
            String displayName,
            List<String> displayNameAliases,
            PinyinIndexData pinyinIndexData
    ) {
        return of(key, displayName, displayNameAliases, pinyinIndexData, "");
    }

    public static DatabaseSearchIndex of(
            StoredStackKey key,
            String displayName,
            List<String> displayNameAliases,
            PinyinIndexData pinyinIndexData,
            String note
    ) {
        LinkedHashSet<String> searchNormalizedTexts = new LinkedHashSet<>();
        LinkedHashSet<String> searchCompactTexts = new LinkedHashSet<>();
        LinkedHashSet<String> searchTokens = new LinkedHashSet<>();
        appendDisplayNameSearchText(displayName, searchNormalizedTexts, searchCompactTexts, searchTokens);
        if (displayNameAliases != null) {
            for (String displayNameAlias : displayNameAliases) {
                appendDisplayNameSearchText(displayNameAlias, searchNormalizedTexts, searchCompactTexts, searchTokens);
            }
        }

        LinkedHashSet<String> noteNormalizedTexts = new LinkedHashSet<>();
        LinkedHashSet<String> noteCompactTexts = new LinkedHashSet<>();
        LinkedHashSet<String> noteSearchTokens = new LinkedHashSet<>();
        if (note != null && !note.isBlank()) {
            appendDisplayNameSearchText(note, noteNormalizedTexts, noteCompactTexts, noteSearchTokens);
        }

        return new DatabaseSearchIndex(
                displayName,
                SearchTextNormalizer.normalizeNaturalText(displayName),
                SearchTextNormalizer.compactNaturalText(displayName),
                List.copyOf(searchTokens),
                List.copyOf(searchNormalizedTexts),
                List.copyOf(searchCompactTexts),
                SearchTextNormalizer.normalizeIdentifierText(key.registryName()),
                SearchTextNormalizer.compactIdentifierText(key.registryName()),
                SearchTextNormalizer.normalizeIdentifierText(key.registryPath()),
                SearchTextNormalizer.compactIdentifierText(key.registryPath()),
                SearchTextNormalizer.tokenizeIdentifierText(key.registryPath()),
                SearchTextNormalizer.normalizeIdentifierText(key.registryNamespace()),
                pinyinIndexData.fullPinyin(),
                pinyinIndexData.initials(),
                pinyinIndexData.tokens(),
                note == null ? "" : note,
                SearchTextNormalizer.normalizeNaturalText(note == null ? "" : note),
                SearchTextNormalizer.compactNaturalText(note == null ? "" : note),
                List.copyOf(noteSearchTokens),
                List.copyOf(noteNormalizedTexts),
                List.copyOf(noteCompactTexts)
        );
    }

    public DatabaseSearchIndex withNote(String newNote) {
        if (newNote == null || newNote.isBlank()) {
            return new DatabaseSearchIndex(
                    this.displayName, this.displayNameNormalized, this.displayNameCompact,
                    this.displayNameTokens, this.displayNameSearchNormalizedTexts, this.displayNameSearchCompactTexts,
                    this.registryNameNormalized, this.registryNameCompact,
                    this.registryPathNormalized, this.registryPathCompact, this.registryPathTokens,
                    this.modNamespace, this.pinyinFull, this.pinyinInitials, this.pinyinTokens,
                    "", "", "", List.of(), List.of(), List.of()
            );
        }
        LinkedHashSet<String> noteNormalizedTexts = new LinkedHashSet<>();
        LinkedHashSet<String> noteCompactTexts = new LinkedHashSet<>();
        LinkedHashSet<String> noteSearchTokens = new LinkedHashSet<>();
        appendDisplayNameSearchText(newNote, noteNormalizedTexts, noteCompactTexts, noteSearchTokens);
        return new DatabaseSearchIndex(
                this.displayName, this.displayNameNormalized, this.displayNameCompact,
                this.displayNameTokens, this.displayNameSearchNormalizedTexts, this.displayNameSearchCompactTexts,
                this.registryNameNormalized, this.registryNameCompact,
                this.registryPathNormalized, this.registryPathCompact, this.registryPathTokens,
                this.modNamespace, this.pinyinFull, this.pinyinInitials, this.pinyinTokens,
                newNote,
                SearchTextNormalizer.normalizeNaturalText(newNote),
                SearchTextNormalizer.compactNaturalText(newNote),
                List.copyOf(noteSearchTokens),
                List.copyOf(noteNormalizedTexts),
                List.copyOf(noteCompactTexts)
        );
    }

    private static void appendDisplayNameSearchText(
            String text,
            Set<String> normalizedTexts,
            Set<String> compactTexts,
            Set<String> tokens
    ) {
        String normalizedText = SearchTextNormalizer.normalizeNaturalText(text);
        if (!normalizedText.isEmpty()) {
            normalizedTexts.add(normalizedText);
            tokens.addAll(SearchTextNormalizer.tokenizeNaturalText(text));
        }
        String compactText = SearchTextNormalizer.compactNaturalText(text);
        if (!compactText.isEmpty()) {
            compactTexts.add(compactText);
        }
    }
}
