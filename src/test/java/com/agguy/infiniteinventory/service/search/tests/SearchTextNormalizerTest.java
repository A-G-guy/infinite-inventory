package com.agguy.infiniteinventory.service.search.tests;

import com.agguy.infiniteinventory.service.search.SearchTextNormalizer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SearchTextNormalizer 纯逻辑方法白盒测试。
 */
class SearchTextNormalizerTest {

    // ---------- normalizeQueryText ----------

    @Test
    void normalizeQueryTextShouldReturnEmptyForNull() {
        assertEquals("", SearchTextNormalizer.normalizeQueryText(null));
    }

    @Test
    void normalizeQueryTextShouldReturnEmptyForBlank() {
        assertEquals("", SearchTextNormalizer.normalizeQueryText("   "));
    }

    @Test
    void normalizeQueryTextShouldReturnEmptyForEmptyString() {
        assertEquals("", SearchTextNormalizer.normalizeQueryText(""));
    }

    @Test
    void normalizeQueryTextShouldLowerCase() {
        assertEquals("hello world", SearchTextNormalizer.normalizeQueryText("Hello World"));
    }

    @Test
    void normalizeQueryTextShouldCollapseMultipleSpaces() {
        assertEquals("a b c", SearchTextNormalizer.normalizeQueryText("a   b    c"));
    }

    @Test
    void normalizeQueryTextShouldTrimLeadingSpaces() {
        assertEquals("hello", SearchTextNormalizer.normalizeQueryText("   hello"));
    }

    @Test
    void normalizeQueryTextShouldTrimTrailingSpaces() {
        assertEquals("hello", SearchTextNormalizer.normalizeQueryText("hello   "));
    }

    @Test
    void normalizeQueryTextShouldHandleTabsAndNewlines() {
        assertEquals("hello world", SearchTextNormalizer.normalizeQueryText("hello\t\nworld"));
    }

    // ---------- splitTerms ----------

    @Test
    void splitTermsShouldReturnEmptyForNull() {
        assertTrue(SearchTextNormalizer.splitTerms(null).isEmpty());
    }

    @Test
    void splitTermsShouldReturnEmptyForBlank() {
        assertTrue(SearchTextNormalizer.splitTerms("   ").isEmpty());
    }

    @Test
    void splitTermsShouldReturnSingleTerm() {
        assertEquals(List.of("hello"), SearchTextNormalizer.splitTerms("hello"));
    }

    @Test
    void splitTermsShouldReturnMultipleTerms() {
        assertEquals(List.of("hello", "world"), SearchTextNormalizer.splitTerms("hello world"));
    }

    @Test
    void splitTermsShouldFilterMeaninglessTerms() {
        assertEquals(List.of("aaa"), SearchTextNormalizer.splitTerms("... aaa ???"));
    }

    // ---------- normalizeNaturalText ----------

    @Test
    void normalizeNaturalTextShouldReturnEmptyForNull() {
        assertEquals("", SearchTextNormalizer.normalizeNaturalText(null));
    }

    @Test
    void normalizeNaturalTextShouldReturnEmptyForBlank() {
        assertEquals("", SearchTextNormalizer.normalizeNaturalText("   "));
    }

    @Test
    void normalizeNaturalTextShouldKeepLettersAndDigits() {
        assertEquals("abc 123", SearchTextNormalizer.normalizeNaturalText("abc 123"));
    }

    @Test
    void normalizeNaturalTextShouldKeepChineseCharacters() {
        assertEquals("hello 世界", SearchTextNormalizer.normalizeNaturalText("Hello 世界!"));
    }

    @Test
    void normalizeNaturalTextShouldReplacePunctuationWithSpace() {
        assertEquals("hello world", SearchTextNormalizer.normalizeNaturalText("hello, world!"));
    }

    @Test
    void normalizeNaturalTextShouldCollapseMultipleSeparators() {
        assertEquals("hello world", SearchTextNormalizer.normalizeNaturalText("hello... world"));
    }

    // ---------- compactNaturalText ----------

    @Test
    void compactNaturalTextShouldReturnEmptyForNull() {
        assertEquals("", SearchTextNormalizer.compactNaturalText(null));
    }

    @Test
    void compactNaturalTextShouldRemoveAllSpaces() {
        assertEquals("helloworld", SearchTextNormalizer.compactNaturalText("hello world"));
    }

    @Test
    void compactNaturalTextShouldLowerCaseAndRemovePunctuation() {
        assertEquals("helloworld", SearchTextNormalizer.compactNaturalText("Hello, World!"));
    }

    // ---------- tokenizeNaturalText ----------

    @Test
    void tokenizeNaturalTextShouldReturnEmptyForNull() {
        assertTrue(SearchTextNormalizer.tokenizeNaturalText(null).isEmpty());
    }

    @Test
    void tokenizeNaturalTextShouldSplitOnSpaces() {
        List<String> tokens = SearchTextNormalizer.tokenizeNaturalText("hello world");
        assertEquals(List.of("hello", "world"), tokens);
    }

    @Test
    void tokenizeNaturalTextShouldFilterEmptyTokens() {
        List<String> tokens = SearchTextNormalizer.tokenizeNaturalText("hello,,,world");
        assertEquals(List.of("hello", "world"), tokens);
    }

    // ---------- normalizeIdentifierText ----------

    @Test
    void normalizeIdentifierTextShouldReturnEmptyForNull() {
        assertEquals("", SearchTextNormalizer.normalizeIdentifierText(null));
    }

    @Test
    void normalizeIdentifierTextShouldReturnEmptyForBlank() {
        assertEquals("", SearchTextNormalizer.normalizeIdentifierText("   "));
    }

    @Test
    void normalizeIdentifierTextShouldTrimAndLowerCase() {
        assertEquals("minecraft", SearchTextNormalizer.normalizeIdentifierText("  Minecraft  "));
    }

    @Test
    void normalizeIdentifierTextShouldKeepUnderscoresAndColons() {
        assertEquals("minecraft:stone", SearchTextNormalizer.normalizeIdentifierText("Minecraft:Stone"));
    }

    // ---------- compactIdentifierText ----------

    @Test
    void compactIdentifierTextShouldReturnEmptyForNull() {
        assertEquals("", SearchTextNormalizer.compactIdentifierText(null));
    }

    @Test
    void compactIdentifierTextShouldRemoveSpaces() {
        // normalizeIdentifierText 将非字母数字字符转为空格，compact 移除空格
        assertEquals("minecraftstone", SearchTextNormalizer.compactIdentifierText("minecraft : stone"));
    }

    // ---------- tokenizeIdentifierText ----------

    @Test
    void tokenizeIdentifierTextShouldReturnEmptyForNull() {
        assertTrue(SearchTextNormalizer.tokenizeIdentifierText(null).isEmpty());
    }

    @Test
    void tokenizeIdentifierTextShouldSplitIdentifier() {
        // 下划线不是字母数字，会被转为空格分隔
        List<String> tokens = SearchTextNormalizer.tokenizeIdentifierText("minecraft iron_sword");
        assertEquals(List.of("minecraft", "iron", "sword"), tokens);
    }

    // ---------- isSubsequence ----------

    @Test
    void isSubsequenceShouldReturnFalseWhenTermIsEmpty() {
        assertFalse(SearchTextNormalizer.isSubsequence("hello", ""));
    }

    @Test
    void isSubsequenceShouldReturnFalseWhenTextIsEmpty() {
        assertFalse(SearchTextNormalizer.isSubsequence("", "hello"));
    }

    @Test
    void isSubsequenceShouldReturnFalseWhenTermLongerThanText() {
        assertFalse(SearchTextNormalizer.isSubsequence("hi", "hello"));
    }

    @Test
    void isSubsequenceShouldReturnTrueForExactMatch() {
        assertTrue(SearchTextNormalizer.isSubsequence("hello", "hello"));
    }

    @Test
    void isSubsequenceShouldReturnTrueForPrefix() {
        assertTrue(SearchTextNormalizer.isSubsequence("hello world", "hello"));
    }

    @Test
    void isSubsequenceShouldReturnTrueForScatteredSubsequence() {
        assertTrue(SearchTextNormalizer.isSubsequence("abcdef", "ace"));
    }

    @Test
    void isSubsequenceShouldReturnFalseWhenCharacterMissing() {
        assertFalse(SearchTextNormalizer.isSubsequence("abcdef", "acg"));
    }

    @Test
    void isSubsequenceShouldReturnFalseWhenOrderViolated() {
        assertFalse(SearchTextNormalizer.isSubsequence("abcdef", "ba"));
    }

    @Test
    void isSubsequenceShouldBeCaseSensitive() {
        assertFalse(SearchTextNormalizer.isSubsequence("Hello", "hello"));
    }
}
