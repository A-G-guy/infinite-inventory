package com.agguy.infiniteinventory.service.tests;

import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.DatabaseSelectionEntry;
import com.agguy.infiniteinventory.database.DatabaseTabs;
import com.agguy.infiniteinventory.database.StoredItemDatabase;
import com.agguy.infiniteinventory.database.StoredStackKey;
import com.agguy.infiniteinventory.tests.MinecraftTestBootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PersonalDatabaseExtractionHelper 包级可见方法的白盒测试。
 *
 * <p>覆盖选择条目规范化、selection key 解析与页内转移等纯逻辑方法。</p>
 */
class PersonalDatabaseExtractionHelperTest {
    private static final Method NORMALIZE_SELECTION_ENTRIES;
    private static final Method SELECTION_KEY;
    private static final Method TRANSFER_SELECTION;

    static {
        MinecraftTestBootstrap.ensureBootstrapped();
        try {
            Class<?> helperClass = Class.forName("com.agguy.infiniteinventory.service.PersonalDatabaseExtractionHelper");
            NORMALIZE_SELECTION_ENTRIES = helperClass.getDeclaredMethod("normalizeSelectionEntries", List.class);
            NORMALIZE_SELECTION_ENTRIES.setAccessible(true);
            SELECTION_KEY = helperClass.getDeclaredMethod("selectionKey", DatabaseSelectionEntry.class);
            SELECTION_KEY.setAccessible(true);
            TRANSFER_SELECTION = helperClass.getDeclaredMethod("transferSelection", StoredItemDatabase.class, List.class, String.class);
            TRANSFER_SELECTION.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static DatabaseSelectionEntry makeEntry(String tabId, ItemStack stack) {
        return new DatabaseSelectionEntry(DatabaseScope.PERSONAL, tabId, stack);
    }

    // ---------- normalizeSelectionEntries ----------

    @Test
    @SuppressWarnings("unchecked")
    void normalizeSelectionEntriesShouldReturnEmptyListForNull() throws ReflectiveOperationException {
        List<DatabaseSelectionEntry> result = (List<DatabaseSelectionEntry>) NORMALIZE_SELECTION_ENTRIES.invoke(null, (List<DatabaseSelectionEntry>) null);
        assertTrue(result.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void normalizeSelectionEntriesShouldReturnEmptyListForEmptyInput() throws ReflectiveOperationException {
        List<DatabaseSelectionEntry> result = (List<DatabaseSelectionEntry>) NORMALIZE_SELECTION_ENTRIES.invoke(null, List.of());
        assertTrue(result.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void normalizeSelectionEntriesShouldDeduplicateAndFilterNulls() throws ReflectiveOperationException {
        DatabaseSelectionEntry entry = makeEntry(DatabaseTabs.DEFAULT_TAB_ID, new ItemStack(Items.STONE));
        List<DatabaseSelectionEntry> input = Arrays.asList(entry, entry, null, entry);
        List<DatabaseSelectionEntry> result = (List<DatabaseSelectionEntry>) NORMALIZE_SELECTION_ENTRIES.invoke(null, input);
        assertEquals(1, result.size());
        assertEquals(entry, result.getFirst());
    }

    @Test
    @SuppressWarnings("unchecked")
    void normalizeSelectionEntriesShouldFilterEmptyEntries() throws ReflectiveOperationException {
        DatabaseSelectionEntry validEntry = makeEntry(DatabaseTabs.DEFAULT_TAB_ID, new ItemStack(Items.STONE));
        DatabaseSelectionEntry emptyEntry = new DatabaseSelectionEntry(DatabaseScope.PERSONAL, "", ItemStack.EMPTY);
        List<DatabaseSelectionEntry> input = List.of(validEntry, emptyEntry);
        List<DatabaseSelectionEntry> result = (List<DatabaseSelectionEntry>) NORMALIZE_SELECTION_ENTRIES.invoke(null, input);
        assertEquals(1, result.size());
    }

    // ---------- selectionKey ----------

    @Test
    void selectionKeyShouldReturnNullForNullEntry() throws ReflectiveOperationException {
        assertNull(SELECTION_KEY.invoke(null, (DatabaseSelectionEntry) null));
    }

    @Test
    void selectionKeyShouldReturnKeyForValidEntry() throws ReflectiveOperationException {
        DatabaseSelectionEntry entry = makeEntry(DatabaseTabs.DEFAULT_TAB_ID, new ItemStack(Items.STONE));
        StoredStackKey result = (StoredStackKey) SELECTION_KEY.invoke(null, entry);
        assertNotNull(result);
        assertEquals(StoredStackKey.of(new ItemStack(Items.STONE)), result);
    }

    @Test
    void selectionKeyShouldReturnNullForEmptyEntry() throws ReflectiveOperationException {
        DatabaseSelectionEntry entry = new DatabaseSelectionEntry(DatabaseScope.PERSONAL, "", ItemStack.EMPTY);
        assertNull(SELECTION_KEY.invoke(null, entry));
    }

    // ---------- transferSelection ----------

    @Test
    void transferSelectionShouldMoveEntriesToTargetTab() throws ReflectiveOperationException {
        StoredItemDatabase database = new StoredItemDatabase();
        StoredStackKey key = StoredStackKey.of(new ItemStack(Items.STONE));
        database.store(new ItemStack(Items.STONE, 8), "source_tab");
        DatabaseSelectionEntry selectionEntry = makeEntry("source_tab", new ItemStack(Items.STONE));

        boolean changed = (boolean) TRANSFER_SELECTION.invoke(null, database, List.of(selectionEntry), "dest_tab");
        assertTrue(changed);
        assertEquals("dest_tab", database.entries().get(key).tabId());
    }

    @Test
    void transferSelectionShouldReturnFalseForEmptySelection() throws ReflectiveOperationException {
        StoredItemDatabase database = new StoredItemDatabase();
        boolean changed = (boolean) TRANSFER_SELECTION.invoke(null, database, List.of(), "dest_tab");
        assertFalse(changed);
    }

    @Test
    void transferSelectionShouldReturnFalseForNullSelection() throws ReflectiveOperationException {
        StoredItemDatabase database = new StoredItemDatabase();
        boolean changed = (boolean) TRANSFER_SELECTION.invoke(null, database, null, "dest_tab");
        assertFalse(changed);
    }

    @Test
    void transferSelectionShouldIgnoreMismatchedSourceTab() throws ReflectiveOperationException {
        StoredItemDatabase database = new StoredItemDatabase();
        StoredStackKey key = StoredStackKey.of(new ItemStack(Items.STONE));
        database.store(new ItemStack(Items.STONE, 8), "actual_tab");
        DatabaseSelectionEntry selectionEntry = makeEntry("wrong_tab", new ItemStack(Items.STONE));

        boolean changed = (boolean) TRANSFER_SELECTION.invoke(null, database, List.of(selectionEntry), "dest_tab");
        assertFalse(changed);
        assertEquals("actual_tab", database.entries().get(key).tabId());
    }
}
