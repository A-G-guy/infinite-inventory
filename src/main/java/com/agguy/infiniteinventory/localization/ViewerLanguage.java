package com.agguy.infiniteinventory.localization;

import java.util.Locale;

public enum ViewerLanguage {
    ZH_CN("zh_cn"),
    EN_US("en_us");

    private final String code;

    ViewerLanguage(String code) {
        this.code = code;
    }

    public String code() {
        return this.code;
    }

    public boolean isChinese() {
        return this == ZH_CN;
    }

    public ViewerLanguage alternate() {
        return this == ZH_CN ? EN_US : ZH_CN;
    }

    public static ViewerLanguage defaultLanguage() {
        return EN_US;
    }

    public static ViewerLanguage resolve(String languageCode) {
        if (languageCode == null || languageCode.isBlank()) {
            return defaultLanguage();
        }
        String normalizedCode = languageCode.trim()
                .replace('-', '_')
                .toLowerCase(Locale.ROOT);
        return switch (normalizedCode) {
            case "zh_cn" -> ZH_CN;
            case "en_us" -> EN_US;
            default -> defaultLanguage();
        };
    }
}
