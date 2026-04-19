package com.agguy.infiniteinventory.service.search;

import com.github.promeg.pinyinhelper.EmbeddedPinyin;
import java.util.ArrayList;
import java.util.List;

public final class PinyinSearchIndexer {
    public static final PinyinSearchIndexer INSTANCE = new PinyinSearchIndexer();

    private PinyinSearchIndexer() {
    }

    public PinyinIndexData toIndex(String text) {
        if (text == null || text.isBlank()) {
            return PinyinIndexData.empty();
        }

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

    public boolean containsChineseCharacters(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        for (int index = 0; index < text.length(); index++) {
            if (this.isChineseCharacter(text.charAt(index))) {
                return true;
            }
        }
        return false;
    }

    private String toPinyin(char character) {
        if (!EmbeddedPinyin.isChinese(character)) {
            return null;
        }
        return SearchTextNormalizer.compactIdentifierText(EmbeddedPinyin.toPinyin(character));
    }

    private boolean isChineseCharacter(char character) {
        return EmbeddedPinyin.isChinese(character);
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
}
