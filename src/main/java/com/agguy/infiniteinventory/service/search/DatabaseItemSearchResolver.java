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
import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class DatabaseItemSearchResolver {
    public static final DatabaseItemSearchResolver INSTANCE = new DatabaseItemSearchResolver();

    private static final Logger LOGGER = LogManager.getLogger();
    private static final HanyuPinyinOutputFormat PINYIN_FORMAT = createPinyinFormat();

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
                    containsChineseCharacters(hoverName) ? this.toPinyinIndex(hoverName) : PinyinIndexData.empty()
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
        String chineseSource = containsChineseCharacters(primaryName) ? primaryName : null;
        for (String candidate : candidates) {
            if (candidate.equals(primaryName)) {
                continue;
            }
            aliases.add(candidate);
            if (chineseSource == null && containsChineseCharacters(candidate)) {
                chineseSource = candidate;
            }
        }
        PinyinIndexData pinyinIndexData = chineseSource == null ? PinyinIndexData.empty() : this.toPinyinIndex(chineseSource);
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

    private PinyinIndexData toPinyinIndex(String text) {
        List<String> pinyinTokens = new java.util.ArrayList<>();
        StringBuilder asciiToken = new StringBuilder();
        StringBuilder initials = new StringBuilder();
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            String pinyin = this.toPinyin(character);
            if (pinyin != null) {
                this.flushAsciiToken(asciiToken, pinyinTokens, initials);
                pinyinTokens.add(pinyin);
                initials.append(pinyin.charAt(0));
                continue;
            }
            if (Character.isLetterOrDigit(character)) {
                asciiToken.append(Character.toLowerCase(character));
                continue;
            }
            this.flushAsciiToken(asciiToken, pinyinTokens, initials);
        }
        this.flushAsciiToken(asciiToken, pinyinTokens, initials);
        if (pinyinTokens.isEmpty()) {
            return PinyinIndexData.empty();
        }
        return new PinyinIndexData(String.join("", pinyinTokens), initials.toString(), List.copyOf(pinyinTokens));
    }

    private String toPinyin(char character) {
        try {
            String[] pinyinValues = PinyinHelper.toHanyuPinyinStringArray(character, PINYIN_FORMAT);
            if (pinyinValues == null || pinyinValues.length == 0) {
                return null;
            }
            return SearchTextNormalizer.compactIdentifierText(pinyinValues[0]);
        } catch (Exception exception) {
            return null;
        }
    }

    private void flushAsciiToken(StringBuilder asciiToken, List<String> pinyinTokens, StringBuilder initials) {
        if (asciiToken.isEmpty()) {
            return;
        }
        String token = asciiToken.toString();
        pinyinTokens.add(token);
        initials.append(token.charAt(0));
        asciiToken.setLength(0);
    }

    private static HanyuPinyinOutputFormat createPinyinFormat() {
        HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
        format.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        format.setVCharType(HanyuPinyinVCharType.WITH_V);
        return format;
    }

    private static boolean containsChineseCharacters(String text) {
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            Character.UnicodeBlock block = Character.UnicodeBlock.of(character);
            if (block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                    || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                    || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                    || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS) {
                return true;
            }
        }
        return false;
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
