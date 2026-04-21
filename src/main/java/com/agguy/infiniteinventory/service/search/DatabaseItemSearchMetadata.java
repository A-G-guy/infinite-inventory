package com.agguy.infiniteinventory.service.search;

import com.agguy.infiniteinventory.database.StoredStackKey;
import java.util.LinkedHashSet;
import java.util.List;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public record DatabaseItemSearchMetadata(
        String modDisplayName,
        String modDisplayNameNormalized,
        String modDisplayNameCompact,
        List<String> itemTagIdsNormalized,
        List<String> itemTagIdsCompact
) {
    public DatabaseItemSearchMetadata {
        modDisplayName = modDisplayName == null ? "" : modDisplayName;
        modDisplayNameNormalized = modDisplayNameNormalized == null ? "" : modDisplayNameNormalized;
        modDisplayNameCompact = modDisplayNameCompact == null ? "" : modDisplayNameCompact;
        itemTagIdsNormalized = List.copyOf(itemTagIdsNormalized == null ? List.of() : itemTagIdsNormalized);
        itemTagIdsCompact = List.copyOf(itemTagIdsCompact == null ? List.of() : itemTagIdsCompact);
    }

    public static DatabaseItemSearchMetadata of(StoredStackKey key) {
        String modDisplayName = DatabaseModMetadataResolver.INSTANCE.displayNameForNamespace(key.registryNamespace());
        LinkedHashSet<String> normalizedTagIds = new LinkedHashSet<>();
        LinkedHashSet<String> compactTagIds = new LinkedHashSet<>();
        key.displayStack().getItem().builtInRegistryHolder().tags()
                .map(TagKey::location)
                .map(Object::toString)
                .forEach(tagId -> {
                    String normalizedTagId = SearchTextNormalizer.normalizeIdentifierText(tagId);
                    if (!normalizedTagId.isEmpty()) {
                        normalizedTagIds.add(normalizedTagId);
                    }
                    String compactTagId = SearchTextNormalizer.compactIdentifierText(tagId);
                    if (!compactTagId.isEmpty()) {
                        compactTagIds.add(compactTagId);
                    }
                });
        return new DatabaseItemSearchMetadata(
                modDisplayName,
                SearchTextNormalizer.normalizeNaturalText(modDisplayName),
                SearchTextNormalizer.compactNaturalText(modDisplayName),
                List.copyOf(normalizedTagIds),
                List.copyOf(compactTagIds)
        );
    }
}
