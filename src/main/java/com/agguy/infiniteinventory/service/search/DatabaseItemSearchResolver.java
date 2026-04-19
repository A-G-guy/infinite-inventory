package com.agguy.infiniteinventory.service.search;

import com.agguy.infiniteinventory.database.StoredStackKey;
import com.agguy.infiniteinventory.localization.ViewerLanguage;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModFileInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class DatabaseItemSearchResolver {
    public static final DatabaseItemSearchResolver INSTANCE = new DatabaseItemSearchResolver();

    private static final Logger LOGGER = LogManager.getLogger();
    private static final PinyinSearchIndexer PINYIN_SEARCH_INDEXER = PinyinSearchIndexer.INSTANCE;

    private final DatabaseSearchIndexCache indexCache = new DatabaseSearchIndexCache();
    private final Map<String, EnumMap<ViewerLanguage, Map<String, String>>> translationsByNamespace =
            new java.util.concurrent.ConcurrentHashMap<>();

    private DatabaseItemSearchResolver() {
    }

    public DatabaseSearchIndex resolve(StoredStackKey key, ViewerLanguage viewerLanguage) {
        ViewerLanguage normalizedLanguage = viewerLanguage == null ? ViewerLanguage.defaultLanguage() : viewerLanguage;
        return this.indexCache.resolve(key, normalizedLanguage, resolvedKey -> this.createIndex(resolvedKey, normalizedLanguage));
    }

    private DatabaseSearchIndex createIndex(StoredStackKey key, ViewerLanguage viewerLanguage) {
        ItemStack displayStack = key.displayStack();
        String hoverName = displayStack.getHoverName().getString();
        if (displayStack.has(DataComponents.CUSTOM_NAME)) {
            return DatabaseSearchIndex.of(
                    key,
                    hoverName,
                    List.of(),
                    PINYIN_SEARCH_INDEXER.containsChineseCharacters(hoverName) ? PINYIN_SEARCH_INDEXER.toIndex(hoverName) : PinyinIndexData.empty()
            );
        }
        ResolvedDisplayNames resolvedDisplayNames = this.resolveDisplayNames(displayStack, key, viewerLanguage, hoverName);
        return DatabaseSearchIndex.of(
                key,
                resolvedDisplayNames.primaryName(),
                resolvedDisplayNames.aliases(),
                resolvedDisplayNames.pinyinIndexData()
        );
    }

    private ResolvedDisplayNames resolveDisplayNames(
            ItemStack stack,
            StoredStackKey key,
            ViewerLanguage viewerLanguage,
            String hoverName
    ) {
        String descriptionId = stack.getDescriptionId();
        if (descriptionId == null || descriptionId.isBlank()) {
            return ResolvedDisplayNames.of(hoverName, List.of(), PinyinIndexData.empty());
        }
        String primaryTranslation = this.translationFor(key.registryNamespace(), viewerLanguage, descriptionId);
        String alternateTranslation = this.translationFor(key.registryNamespace(), viewerLanguage.alternate(), descriptionId);
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        appendCandidate(candidates, primaryTranslation);
        appendCandidate(candidates, alternateTranslation);
        appendCandidate(candidates, hoverName);
        if (candidates.isEmpty()) {
            return ResolvedDisplayNames.of(hoverName, List.of(), PinyinIndexData.empty());
        }

        String primaryName = firstNonBlank(primaryTranslation, alternateTranslation, hoverName);
        java.util.ArrayList<String> aliases = new java.util.ArrayList<>();
        String chineseSource = PINYIN_SEARCH_INDEXER.containsChineseCharacters(primaryName) ? primaryName : null;
        for (String candidate : candidates) {
            if (candidate.equals(primaryName)) {
                continue;
            }
            aliases.add(candidate);
            if (chineseSource == null && PINYIN_SEARCH_INDEXER.containsChineseCharacters(candidate)) {
                chineseSource = candidate;
            }
        }
        PinyinIndexData pinyinIndexData = chineseSource == null ? PinyinIndexData.empty() : PINYIN_SEARCH_INDEXER.toIndex(chineseSource);
        return ResolvedDisplayNames.of(primaryName, List.copyOf(aliases), pinyinIndexData);
    }

    private String translationFor(String namespace, ViewerLanguage viewerLanguage, String descriptionId) {
        return Optional.ofNullable(this.translationsForNamespace(namespace, viewerLanguage).get(descriptionId))
                .filter(value -> !value.isBlank())
                .orElse("");
    }

    private Map<String, String> translationsForNamespace(String namespace, ViewerLanguage viewerLanguage) {
        EnumMap<ViewerLanguage, Map<String, String>> namespaceTranslations = this.translationsByNamespace.computeIfAbsent(
                namespace,
                ignored -> new EnumMap<>(ViewerLanguage.class)
        );
        synchronized (namespaceTranslations) {
            return namespaceTranslations.computeIfAbsent(viewerLanguage, language -> this.loadTranslations(namespace, language));
        }
    }

    private Map<String, String> loadTranslations(String namespace, ViewerLanguage viewerLanguage) {
        String resourcePath = "assets/" + namespace + "/lang/" + viewerLanguage.code() + ".json";
        Map<String, String> translations = new HashMap<>();
        this.readTranslations(this.classpathResource(resourcePath), resourcePath, translations);
        if (!translations.isEmpty()) {
            return Map.copyOf(translations);
        }

        Optional<IModFileInfo> modFileInfo = Optional.ofNullable(ModList.get())
                .map(modList -> modList.getModFileById(namespace));
        if (modFileInfo.isEmpty()) {
            return Map.of();
        }

        Path resourceFile = modFileInfo.get().getFile().findResource("assets", namespace, "lang", viewerLanguage.code() + ".json");
        if (Files.notExists(resourceFile)) {
            return Map.of();
        }
        try (Reader reader = Files.newBufferedReader(resourceFile, StandardCharsets.UTF_8)) {
            this.readTranslations(reader, resourceFile.toString(), translations);
        } catch (IOException exception) {
            LOGGER.warn("读取语言文件失败: {}", resourceFile, exception);
        }
        return translations.isEmpty() ? Map.of() : Map.copyOf(translations);
    }

    private Reader classpathResource(String resourcePath) {
        InputStream resourceStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath);
        if (resourceStream == null) {
            resourceStream = DatabaseItemSearchResolver.class.getClassLoader().getResourceAsStream(resourcePath);
        }
        if (resourceStream == null) {
            return null;
        }
        return new BufferedReader(new InputStreamReader(resourceStream, StandardCharsets.UTF_8));
    }

    private void readTranslations(Reader reader, String sourceName, Map<String, String> translations) {
        if (reader == null) {
            return;
        }
        try (Reader closeableReader = reader) {
            JsonElement jsonElement = JsonParser.parseReader(closeableReader);
            if (!(jsonElement instanceof JsonObject jsonObject)) {
                return;
            }
            for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                if (entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isString()) {
                    translations.put(entry.getKey(), entry.getValue().getAsString());
                }
            }
        } catch (RuntimeException | IOException exception) {
            LOGGER.warn("解析语言文件失败: {}", sourceName, exception);
        }
    }

    private static void appendCandidate(LinkedHashSet<String> candidates, String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return;
        }
        candidates.add(candidate);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private record ResolvedDisplayNames(String primaryName, List<String> aliases, PinyinIndexData pinyinIndexData) {
        private static ResolvedDisplayNames of(String primaryName, List<String> aliases, PinyinIndexData pinyinIndexData) {
            return new ResolvedDisplayNames(
                    primaryName == null ? "" : primaryName,
                    aliases == null ? List.of() : List.copyOf(aliases),
                    pinyinIndexData == null ? PinyinIndexData.empty() : pinyinIndexData
            );
        }
    }
}
