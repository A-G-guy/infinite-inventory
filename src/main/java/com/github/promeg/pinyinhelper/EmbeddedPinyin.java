package com.github.promeg.pinyinhelper;

public final class EmbeddedPinyin {
    private EmbeddedPinyin() {
    }

    public static String toPinyin(char character) {
        if (!isChinese(character)) {
            return String.valueOf(character);
        }
        if (character == PinyinData.CHAR_12295) {
            return PinyinData.PINYIN_12295;
        }
        return PinyinData.PINYIN_TABLE[pinyinCode(character)];
    }

    public static boolean isChinese(char character) {
        return (PinyinData.MIN_VALUE <= character
                && character <= PinyinData.MAX_VALUE
                && pinyinCode(character) > 0)
                || PinyinData.CHAR_12295 == character;
    }

    private static int pinyinCode(char character) {
        int offset = character - PinyinData.MIN_VALUE;
        if (0 <= offset && offset < PinyinData.PINYIN_CODE_1_OFFSET) {
            return decodeIndex(PinyinCode1.PINYIN_CODE_PADDING, PinyinCode1.PINYIN_CODE, offset);
        }
        if (PinyinData.PINYIN_CODE_1_OFFSET <= offset && offset < PinyinData.PINYIN_CODE_2_OFFSET) {
            return decodeIndex(
                    PinyinCode2.PINYIN_CODE_PADDING,
                    PinyinCode2.PINYIN_CODE,
                    offset - PinyinData.PINYIN_CODE_1_OFFSET
            );
        }
        return decodeIndex(
                PinyinCode3.PINYIN_CODE_PADDING,
                PinyinCode3.PINYIN_CODE,
                offset - PinyinData.PINYIN_CODE_2_OFFSET
        );
    }

    private static int decodeIndex(byte[] paddings, byte[] indexes, int offset) {
        int index1 = offset / 8;
        int index2 = offset % 8;
        int realIndex = indexes[offset] & 0xff;
        if ((paddings[index1] & PinyinData.BIT_MASKS[index2]) != 0) {
            realIndex = realIndex | PinyinData.PADDING_MASK;
        }
        return realIndex;
    }
}
