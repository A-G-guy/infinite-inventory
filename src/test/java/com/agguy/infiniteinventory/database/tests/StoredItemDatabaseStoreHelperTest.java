package com.agguy.infiniteinventory.database.tests;

import com.agguy.infiniteinventory.database.*;
import com.agguy.infiniteinventory.tests.MinecraftTestBootstrap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * StoredItemDatabaseStoreHelper 包级可见方法的白盒测试。
 *
 * <p>通过反射调用 package-private 方法，补充现有集成测试未覆盖的边界情况。</p>
 */
class StoredItemDatabaseStoreHelperTest {
    private static final Method STORE_DEFAULT;
    private static final Method STORE_TAB;
    private static final Method EXTRACT;
    private static final Method MERGE_FROM;

    static {
        MinecraftTestBootstrap.ensureBootstrapped();
        try {
            Class<?> helperClass = Class.forName("com.agguy.infiniteinventory.database.StoredItemDatabaseStoreHelper");
            STORE_DEFAULT = helperClass.getDeclaredMethod("store", StoredItemDatabase.class, ItemStack.class);
            STORE_DEFAULT.setAccessible(true);
            STORE_TAB = helperClass.getDeclaredMethod("store", StoredItemDatabase.class, ItemStack.class, String.class);
            STORE_TAB.setAccessible(true);
            EXTRACT = helperClass.getDeclaredMethod("extract", StoredItemDatabase.class, StoredStackKey.class, int.class);
            EXTRACT.setAccessible(true);
            MERGE_FROM = helperClass.getDeclaredMethod("mergeFrom", StoredItemDatabase.class, StoredItemDatabase.class);
            MERGE_FROM.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    // ---------- store ----------

    @Test
    void storeShouldSilentlyIgnoreEmptyStack() throws ReflectiveOperationException {
        StoredItemDatabase database = new StoredItemDatabase();
        STORE_DEFAULT.invoke(null, database, ItemStack.EMPTY);
        assertEquals(0, database.entryCount());
    }

    @Test
    void storeShouldSilentlyIgnoreEmptyStackWithExplicitTab() throws ReflectiveOperationException {
        StoredItemDatabase database = new StoredItemDatabase();
        STORE_TAB.invoke(null, database, ItemStack.EMPTY, "blocks");
        assertEquals(0, database.entryCount());
    }

    @Test
    void storeShouldAssignCorrectTabId() throws ReflectiveOperationException {
        StoredItemDatabase database = new StoredItemDatabase();
        STORE_TAB.invoke(null, database, new ItemStack(Items.STONE, 8), "building_blocks");
        assertEquals(1, database.entryCount());
        StoredStackEntry entry = database.entries().values().iterator().next();
        assertEquals("building_blocks", entry.tabId());
        assertEquals(8L, entry.amount());
    }

    @Test
    void storeShouldMergeMatchingKeyIntoSameTab() throws ReflectiveOperationException {
        StoredItemDatabase database = new StoredItemDatabase();
        STORE_TAB.invoke(null, database, new ItemStack(Items.STONE, 8), "blocks");
        STORE_TAB.invoke(null, database, new ItemStack(Items.STONE, 4), "blocks");
        assertEquals(1, database.entryCount());
        StoredStackKey key = StoredStackKey.of(new ItemStack(Items.STONE));
        assertEquals(12L, database.getAmount(key));
    }

    @Test
    void storeShouldCreateSeparateEntriesForDifferentItems() throws ReflectiveOperationException {
        StoredItemDatabase database = new StoredItemDatabase();
        STORE_DEFAULT.invoke(null, database, new ItemStack(Items.STONE, 4));
        STORE_DEFAULT.invoke(null, database, new ItemStack(Items.DIRT, 8));
        assertEquals(2, database.entryCount());
    }

    @Test
    void storeShouldAdvanceNextSequence() throws ReflectiveOperationException {
        StoredItemDatabase database = new StoredItemDatabase();
        long beforeSequence = DatabaseTestReflectionHelper.readNextSequence(database);
        STORE_DEFAULT.invoke(null, database, new ItemStack(Items.STONE, 1));
        long afterSequence = DatabaseTestReflectionHelper.readNextSequence(database);
        assertTrue(afterSequence > beforeSequence);
    }

    // ---------- extract ----------

    @Test
    void extractShouldReturnEmptyForZeroRequestedAmount() throws ReflectiveOperationException {
        StoredItemDatabase database = new StoredItemDatabase();
        StoredStackKey key = StoredStackKey.of(new ItemStack(Items.STONE));
        database.store(new ItemStack(Items.STONE, 64));
        ItemStack result = (ItemStack) EXTRACT.invoke(null, database, key, 0);
        assertTrue(result.isEmpty());
    }

    @Test
    void extractShouldReturnEmptyForNegativeRequestedAmount() throws ReflectiveOperationException {
        StoredItemDatabase database = new StoredItemDatabase();
        StoredStackKey key = StoredStackKey.of(new ItemStack(Items.STONE));
        database.store(new ItemStack(Items.STONE, 64));
        ItemStack result = (ItemStack) EXTRACT.invoke(null, database, key, -1);
        assertTrue(result.isEmpty());
    }

    @Test
    void extractShouldReturnEmptyWhenKeyNotFound() throws ReflectiveOperationException {
        StoredItemDatabase database = new StoredItemDatabase();
        StoredStackKey key = StoredStackKey.of(new ItemStack(Items.STONE));
        ItemStack result = (ItemStack) EXTRACT.invoke(null, database, key, 10);
        assertTrue(result.isEmpty());
    }

    @Test
    void extractShouldRemoveEntryWhenAmountReachesZero() throws ReflectiveOperationException {
        StoredItemDatabase database = new StoredItemDatabase();
        StoredStackKey key = StoredStackKey.of(new ItemStack(Items.STONE));
        database.store(new ItemStack(Items.STONE, 5));
        ItemStack result = (ItemStack) EXTRACT.invoke(null, database, key, 5);
        assertEquals(5, result.getCount());
        assertEquals(0, database.entryCount());
    }

    @Test
    void extractShouldNotRemoveEntryWhenPartialAmountRemains() throws ReflectiveOperationException {
        StoredItemDatabase database = new StoredItemDatabase();
        StoredStackKey key = StoredStackKey.of(new ItemStack(Items.STONE));
        database.store(new ItemStack(Items.STONE, 10));
        ItemStack result = (ItemStack) EXTRACT.invoke(null, database, key, 3);
        assertEquals(3, result.getCount());
        assertEquals(1, database.entryCount());
        assertEquals(7L, database.getAmount(key));
    }

    // ---------- mergeFrom ----------

    @Test
    void mergeFromShouldHandleNullSource() throws ReflectiveOperationException {
        StoredItemDatabase database = new StoredItemDatabase();
        database.store(new ItemStack(Items.STONE, 1));
        MERGE_FROM.invoke(null, database, (StoredItemDatabase) null);
        assertEquals(1, database.entryCount());
    }

    @Test
    void mergeFromShouldHandleEmptySource() throws ReflectiveOperationException {
        StoredItemDatabase database = new StoredItemDatabase();
        database.store(new ItemStack(Items.STONE, 1));
        MERGE_FROM.invoke(null, database, new StoredItemDatabase());
        assertEquals(1, database.entryCount());
    }

    @Test
    void mergeFromShouldMergeNotes() throws ReflectiveOperationException {
        StoredItemDatabase target = new StoredItemDatabase();
        StoredItemDatabase source = new StoredItemDatabase();
        StoredStackKey key = StoredStackKey.of(new ItemStack(Items.STONE));
        source.setNote(key, "my note");

        MERGE_FROM.invoke(null, target, source);

        assertEquals("my note", target.noteFor(key));
    }

    @Test
    void mergeFromShouldMergeStars() throws ReflectiveOperationException {
        StoredItemDatabase target = new StoredItemDatabase();
        StoredItemDatabase source = new StoredItemDatabase();
        StoredStackKey key = StoredStackKey.of(new ItemStack(Items.STONE));
        source.toggleStar(key);

        MERGE_FROM.invoke(null, target, source);

        assertTrue(target.isStarred(key));
    }

    @Test
    void mergeFromShouldMergeUnresolvedEntries() throws ReflectiveOperationException {
        StoredItemDatabase target = new StoredItemDatabase();
        StoredItemDatabase source = new StoredItemDatabase();
        CompoundTag stackTag = DatabaseTestReflectionHelper.invalidStackTag("missing:thing");
        UnresolvedStoredEntry unresolved = new UnresolvedStoredEntry(stackTag, 5L, DatabaseTabs.DEFAULT_TAB_ID, 10L, 10L);
        DatabaseTestReflectionHelper.forceUnresolvedEntry(source, unresolved);

        MERGE_FROM.invoke(null, target, source);

        assertEquals(1, target.unresolvedEntryCount());
    }

    @Test
    void mergeFromShouldUpdateNextSequenceAfterMerge() throws ReflectiveOperationException {
        StoredItemDatabase target = new StoredItemDatabase();
        StoredItemDatabase source = new StoredItemDatabase();
        DatabaseTestReflectionHelper.forceNextSequence(target, 5L);
        DatabaseTestReflectionHelper.forceNextSequence(source, 20L);
        // 必须有实际条目才能触发 changed 标志和序列号更新
        StoredStackKey key = DatabaseTestReflectionHelper.fakeKey("test:seq_item");
        DatabaseTestReflectionHelper.forceEntry(source, key, new StoredStackEntry(DatabaseTabs.DEFAULT_TAB_ID, 1L, 30L));

        MERGE_FROM.invoke(null, target, source);

        assertTrue(DatabaseTestReflectionHelper.readNextSequence(target) >= 31L);
    }

    @Test
    void mergeFromShouldNotChangeWhenSourceIsIdentical() throws ReflectiveOperationException {
        StoredItemDatabase target = new StoredItemDatabase();
        StoredItemDatabase source = new StoredItemDatabase();
        long beforeRevision = DatabaseTestReflectionHelper.readRevision(target);
        MERGE_FROM.invoke(null, target, source);
        assertEquals(beforeRevision, DatabaseTestReflectionHelper.readRevision(target));
    }
}
