package com.agguy.infiniteinventory.localization.tests;

import com.agguy.infiniteinventory.localization.ViewerLanguage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ViewerLanguageTest {

    @Test
    void defaultLanguageShouldBeEnglish() {
        assertEquals(ViewerLanguage.EN_US, ViewerLanguage.defaultLanguage());
    }

    @Test
    void chineseLanguageShouldBeDetected() {
        assertTrue(ViewerLanguage.ZH_CN.isChinese());
        assertFalse(ViewerLanguage.EN_US.isChinese());
    }

    @Test
    void alternateShouldSwitchBetweenLanguages() {
        assertEquals(ViewerLanguage.EN_US, ViewerLanguage.ZH_CN.alternate());
        assertEquals(ViewerLanguage.ZH_CN, ViewerLanguage.EN_US.alternate());
    }

    @ParameterizedTest
    @ValueSource(strings = {"zh_cn", "zh-CN", "ZH_CN", "zh_Cn"})
    void shouldResolveChineseLanguage(String code) {
        assertEquals(ViewerLanguage.ZH_CN, ViewerLanguage.resolve(code));
    }

    @ParameterizedTest
    @ValueSource(strings = {"en_us", "en-US", "EN_US", "en_Us"})
    void shouldResolveEnglishLanguage(String code) {
        assertEquals(ViewerLanguage.EN_US, ViewerLanguage.resolve(code));
    }

    @Test
    void nullOrBlankShouldFallbackToDefault() {
        assertEquals(ViewerLanguage.defaultLanguage(), ViewerLanguage.resolve(null));
        assertEquals(ViewerLanguage.defaultLanguage(), ViewerLanguage.resolve(""));
        assertEquals(ViewerLanguage.defaultLanguage(), ViewerLanguage.resolve("   "));
    }

    @ParameterizedTest
    @ValueSource(strings = {"fr_fr", "de_de", "ja_jp", "unknown"})
    void unsupportedLanguageShouldFallbackToDefault(String code) {
        assertEquals(ViewerLanguage.defaultLanguage(), ViewerLanguage.resolve(code));
    }

    @Test
    void codeShouldMatchEnumValue() {
        assertEquals("zh_cn", ViewerLanguage.ZH_CN.code());
        assertEquals("en_us", ViewerLanguage.EN_US.code());
    }
}
