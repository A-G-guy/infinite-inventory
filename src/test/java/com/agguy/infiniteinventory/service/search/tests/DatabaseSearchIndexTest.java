package com.agguy.infiniteinventory.service.search.tests;

import com.agguy.infiniteinventory.database.StoredStackKey;
import com.agguy.infiniteinventory.service.search.DatabaseSearchIndex;
import com.agguy.infiniteinventory.service.search.PinyinIndexData;
import com.agguy.infiniteinventory.tests.MinecraftTestBootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DatabaseSearchIndex 构建与字段规范化白盒测试。
 */
class DatabaseSearchIndexTest {

    static {
        MinecraftTestBootstrap.ensureBootstrapped();
    }

    private static StoredStackKey stoneKey() {
        return StoredStackKey.of(new ItemStack(Items.STONE));
    }

    // ---------- compact constructor normalization ----------

    @Test
    void constructorShouldNormalizeNullDisplayName() {
        DatabaseSearchIndex index = new DatabaseSearchIndex(
                null, null, null, List.of(), List.of(), List.of(),
                null, null, null, null, List.of(),
                null, null, null, List.of(),
                null, null, null, List.of(), List.of(), List.of()
        );
        assertEquals("", index.displayName());
        assertEquals("", index.displayNameNormalized());
        assertEquals("", index.displayNameCompact());
    }

    @Test
    void constructorShouldNormalizeNullRegistryFields() {
        DatabaseSearchIndex index = new DatabaseSearchIndex(
                "", "", "", List.of(), List.of(), List.of(),
                null, null, null, null, List.of(),
                null, null, null, List.of(),
                null, null, null, List.of(), List.of(), List.of()
        );
        assertEquals("", index.registryNameNormalized());
        assertEquals("", index.registryNameCompact());
        assertEquals("", index.registryPathNormalized());
        assertEquals("", index.registryPathCompact());
        assertEquals("", index.modNamespace());
    }

    @Test
    void constructorShouldNormalizeNullPinyinFields() {
        DatabaseSearchIndex index = new DatabaseSearchIndex(
                "", "", "", List.of(), List.of(), List.of(),
                "", "", "", "", List.of(),
                "", null, null, List.of(),
                null, null, null, List.of(), List.of(), List.of()
        );
        assertEquals("", index.pinyinFull());
        assertEquals("", index.pinyinInitials());
        assertTrue(index.pinyinTokens().isEmpty());
    }

    @Test
    void constructorShouldNormalizeNullNoteFields() {
        DatabaseSearchIndex index = new DatabaseSearchIndex(
                "", "", "", List.of(), List.of(), List.of(),
                "", "", "", "", List.of(),
                "", "", "", List.of(),
                null, null, null, List.of(), List.of(), List.of()
        );
        assertEquals("", index.note());
        assertEquals("", index.noteNormalized());
        assertEquals("", index.noteCompact());
        assertTrue(index.noteTokens().isEmpty());
    }

    @Test
    void constructorShouldDefensivelyCopyLists() {
        List<String> mutableTokens = new java.util.ArrayList<>(List.of("a", "b"));
        DatabaseSearchIndex index = new DatabaseSearchIndex(
                "", "", "", mutableTokens, List.of(), List.of(),
                "", "", "", "", List.of(),
                "", "", "", List.of(),
                "", "", "", List.of(), List.of(), List.of()
        );
        mutableTokens.add("c");
        assertEquals(2, index.displayNameTokens().size());
    }

    // ---------- of(StoredStackKey, String, List, PinyinIndexData) ----------

    @Test
    void ofShouldPopulateDisplayNameFields() {
        DatabaseSearchIndex index = DatabaseSearchIndex.of(
                stoneKey(), "Stone", List.of(), PinyinIndexData.empty()
        );
        assertEquals("Stone", index.displayName());
        assertFalse(index.displayNameNormalized().isEmpty());
        assertFalse(index.displayNameCompact().isEmpty());
    }

    @Test
    void ofShouldPopulateRegistryFields() {
        DatabaseSearchIndex index = DatabaseSearchIndex.of(
                stoneKey(), "Stone", List.of(), PinyinIndexData.empty()
        );
        assertEquals("minecraft:stone", index.registryNameNormalized());
        assertEquals("stone", index.registryPathCompact());
    }

    @Test
    void ofShouldIncludeAliasesInSearchTexts() {
        DatabaseSearchIndex index = DatabaseSearchIndex.of(
                stoneKey(), "Stone", List.of("Rock", "Pebble"), PinyinIndexData.empty()
        );
        assertTrue(index.displayNameSearchNormalizedTexts().size() >= 3,
                "应包含主名称与两个别名的规范化文本，实际: " + index.displayNameSearchNormalizedTexts().size());
    }

    @Test
    void ofShouldHandleNullAliases() {
        DatabaseSearchIndex index = DatabaseSearchIndex.of(
                stoneKey(), "Stone", null, PinyinIndexData.empty()
        );
        assertNotNull(index.displayNameSearchNormalizedTexts());
    }

    @Test
    void ofShouldHandlePinyinData() {
        PinyinIndexData pinyin = new PinyinIndexData("shijie", "sj", List.of("shi", "jie"));
        DatabaseSearchIndex index = DatabaseSearchIndex.of(
                stoneKey(), "Stone", List.of(), pinyin
        );
        assertEquals("shijie", index.pinyinFull());
        assertEquals("sj", index.pinyinInitials());
        assertEquals(List.of("shi", "jie"), index.pinyinTokens());
    }

    @Test
    void ofShouldHaveEmptyNoteByDefault() {
        DatabaseSearchIndex index = DatabaseSearchIndex.of(
                stoneKey(), "Stone", List.of(), PinyinIndexData.empty()
        );
        assertEquals("", index.note());
        assertTrue(index.noteTokens().isEmpty());
    }

    // ---------- of(StoredStackKey, String, List, PinyinIndexData, String) ----------

    @Test
    void ofWithNoteShouldPopulateNoteFields() {
        DatabaseSearchIndex index = DatabaseSearchIndex.of(
                stoneKey(), "Stone", List.of(), PinyinIndexData.empty(), "My favorite block"
        );
        assertEquals("My favorite block", index.note());
        assertFalse(index.noteNormalized().isEmpty());
        assertFalse(index.noteTokens().isEmpty());
    }

    @Test
    void ofWithNoteShouldHandleEmptyString() {
        DatabaseSearchIndex index = DatabaseSearchIndex.of(
                stoneKey(), "Stone", List.of(), PinyinIndexData.empty(), ""
        );
        assertEquals("", index.note());
    }

    @Test
    void ofWithNoteShouldHandleNullNote() {
        DatabaseSearchIndex index = DatabaseSearchIndex.of(
                stoneKey(), "Stone", List.of(), PinyinIndexData.empty(), null
        );
        assertEquals("", index.note());
    }

    // ---------- withNote ----------

    @Test
    void withNoteShouldUpdateNote() {
        DatabaseSearchIndex original = DatabaseSearchIndex.of(
                stoneKey(), "Stone", List.of(), PinyinIndexData.empty()
        );
        DatabaseSearchIndex updated = original.withNote("New note");
        assertEquals("New note", updated.note());
        assertFalse(updated.noteTokens().isEmpty());
    }

    @Test
    void withNoteShouldPreserveOtherFields() {
        DatabaseSearchIndex original = DatabaseSearchIndex.of(
                stoneKey(), "Stone", List.of(), PinyinIndexData.empty()
        );
        DatabaseSearchIndex updated = original.withNote("New note");
        assertEquals(original.displayName(), updated.displayName());
        assertEquals(original.registryNameNormalized(), updated.registryNameNormalized());
    }

    @Test
    void withNoteShouldClearNoteOnNull() {
        DatabaseSearchIndex original = DatabaseSearchIndex.of(
                stoneKey(), "Stone", List.of(), PinyinIndexData.empty(), "Old note"
        );
        DatabaseSearchIndex updated = original.withNote(null);
        assertEquals("", updated.note());
        assertTrue(updated.noteTokens().isEmpty());
    }

    @Test
    void withNoteShouldClearNoteOnBlank() {
        DatabaseSearchIndex original = DatabaseSearchIndex.of(
                stoneKey(), "Stone", List.of(), PinyinIndexData.empty(), "Old note"
        );
        DatabaseSearchIndex updated = original.withNote("   ");
        assertEquals("", updated.note());
    }
}
