package com.agguy.infiniteinventory.service.search.tests;

import com.agguy.infiniteinventory.service.search.PinyinIndexData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PinyinIndexDataTest {

    @Test
    void emptyShouldReturnDefaultValues() {
        PinyinIndexData data = PinyinIndexData.empty();

        assertEquals("", data.fullPinyin());
        assertEquals("", data.initials());
        assertTrue(data.tokens().isEmpty());
    }

    @Test
    void shouldHandleNullFullPinyinAndInitials() {
        PinyinIndexData data = new PinyinIndexData(null, null, List.of());

        assertEquals("", data.fullPinyin());
        assertEquals("", data.initials());
        assertTrue(data.tokens().isEmpty());
    }

    @Test
    void shouldPreserveValidValues() {
        PinyinIndexData data = new PinyinIndexData("zhongwen", "zw", List.of("zhong", "wen"));

        assertEquals("zhongwen", data.fullPinyin());
        assertEquals("zw", data.initials());
        assertEquals(2, data.tokens().size());
        assertEquals("zhong", data.tokens().get(0));
        assertEquals("wen", data.tokens().get(1));
    }

    @Test
    void tokensShouldBeDefensivelyCopied() {
        List<String> mutableTokens = new java.util.ArrayList<>(List.of("token1"));
        PinyinIndexData data = new PinyinIndexData("full", "i", mutableTokens);

        mutableTokens.add("token2");

        assertEquals(1, data.tokens().size());
    }
}
