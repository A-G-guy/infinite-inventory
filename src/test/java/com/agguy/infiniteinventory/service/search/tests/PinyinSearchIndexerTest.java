package com.agguy.infiniteinventory.service.search.tests;

import com.agguy.infiniteinventory.service.search.PinyinIndexData;
import com.agguy.infiniteinventory.service.search.PinyinSearchIndexer;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PinyinSearchIndexerTest {
    @Test
    void shouldConvertChineseAndAsciiIntoStableSearchTokens() {
        PinyinIndexData indexData = PinyinSearchIndexer.INSTANCE.toIndex("苹果ABC");

        assertEquals("pingguoabc", indexData.fullPinyin());
        assertEquals("pga", indexData.initials());
        assertEquals(List.of("ping", "guo", "abc"), indexData.tokens());
    }

    @Test
    void shouldDetectChineseCharactersThroughLibraryRules() {
        assertTrue(PinyinSearchIndexer.INSTANCE.containsChineseCharacters("数据库"));
        assertFalse(PinyinSearchIndexer.INSTANCE.containsChineseCharacters("database"));
    }
}
