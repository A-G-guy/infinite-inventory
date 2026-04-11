package com.agguy.infiniteinventory.service.search;

import java.util.List;

public record PinyinIndexData(String fullPinyin, String initials, List<String> tokens) {
    private static final PinyinIndexData EMPTY = new PinyinIndexData("", "", List.of());

    public PinyinIndexData {
        fullPinyin = fullPinyin == null ? "" : fullPinyin;
        initials = initials == null ? "" : initials;
        tokens = List.copyOf(tokens);
    }

    public static PinyinIndexData empty() {
        return EMPTY;
    }
}
