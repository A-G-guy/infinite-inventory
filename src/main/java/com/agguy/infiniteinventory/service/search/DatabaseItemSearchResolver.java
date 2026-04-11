package com.agguy.infiniteinventory.service.search;

import com.agguy.infiniteinventory.database.StoredStackKey;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
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

    private final Map<StoredStackKey, DatabaseSearchIndex> indexCache = new ConcurrentHashMap<>();
    private final Map<String, Map<String, String>> translationsByNamespace = new ConcurrentHashMap<>();

    private DatabaseItemSearchResolver() {
    }

    public DatabaseSearchIndex resolve(StoredStackKey key) {
        return this.indexCache.computeIfAbsent(key, this::createIndex);
    }

    private DatabaseSearchIndex createIndex(StoredStackKey key) {
        ItemStack displayStack = key.displayStack();
        String hoverName = displayStack.getHoverName().getString();
        boolean customName = displayStack.has(DataComponents.CUSTOM_NAME);
        String translatedName = customName ? hoverName : this.lookupZhCnName(displayStack, key).orElse(hoverName);
        boolean supportsPinyin = customName || !translatedName.equals(hoverName) || containsChineseCharacters(translatedName);
        PinyinIndexData pinyinIndexData = supportsPinyin ? this.toPinyinIndex(translatedName) : PinyinIndexData.empty();
        return DatabaseSearchIndex.of(key, translatedName, pinyinIndexData);
    }

    private Optional<String> lookupZhCnName(ItemStack stack, StoredStackKey key) {
        String descriptionId = stack.getDescriptionId();
        if (descriptionId == null || descriptionId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(this.translationsForNamespace(key.registryNamespace()).get(descriptionId))
                .filter(value -> !value.isBlank());
    }

    private Map<String, String> translationsForNamespace(String namespace) {
        return this.translationsByNamespace.computeIfAbsent(namespace, this::loadTranslations);
    }

    private Map<String, String> loadTranslations(String namespace) {
        String resourcePath = "assets/" + namespace + "/lang/zh_cn.json";
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

        Path resourceFile = modFileInfo.get().getFile().findResource("assets", namespace, "lang", "zh_cn.json");
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
        List<String> pinyinTokens = new ArrayList<>();
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
}
