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
 * StoredItemDatabaseTabHelper 包级可见方法的白盒测试。
 *
 * <p>通过反射调用 package-private 方法，覆盖标签页转移、条目迁移
 * 和标签页分配校验等关键路径的边界情况。</p>
 */
class StoredItemDatabaseTabHelperTest {
    private static final Method TRANSFER_TAB;
    private static final Method MOVE_ENTRY_TO_TAB;
    private static final Method ENSURE_TAB_ASSIGNMENTS;

    static {
        MinecraftTestBootstrap.ensureBootstrapped();
        try {
            Class<?> helperClass = Class.forName("com.agguy.infiniteinventory.database.StoredItemDatabaseTabHelper");
            TRANSFER_TAB = helperClass.getDeclaredMethod("transferTab", StoredItemDatabase.class, String.class, String.class);
            TRANSFER_TAB.setAccessible(true);
            MOVE_ENTRY_TO_TAB = helperClass.getDeclaredMethod("moveEntryToTab", StoredItemDatabase.class, StoredStackKey.class, String.class, String.class);
            MOVE_ENTRY_TO_TAB.setAccessible(true);
            ENSURE_TAB_ASSIGNMENTS = helperClass.getDeclaredMethod("ensureTabAssignments", StoredItemDatabase.class, DatabaseTabDirectory.class);
            ENSURE_TAB_ASSIGNMENTS.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 创建包含指定 ID 标签页的目录。
     *
     * <p>{@link DatabaseTabDirectory#addCustomTab} 会生成随机 ID，
     * 而我们需要精确控制 tab ID 以匹配存入时的 tabId。
     * 因此通过反射直接向 concreteTabs 列表插入我们指定的 tab。</p>
     */
    @SuppressWarnings("unchecked")
    private static DatabaseTabDirectory directoryWith(String... tabIds) throws ReflectiveOperationException {
        DatabaseTabDirectory directory = new DatabaseTabDirectory();
        java.lang.reflect.Field concreteTabsField = DatabaseTabDirectory.class.getDeclaredField("concreteTabs");
        concreteTabsField.setAccessible(true);
        java.util.List<DatabaseTab> concreteTabs = (java.util.List<DatabaseTab>) concreteTabsField.get(directory);
        for (String tabId : tabIds) {
            concreteTabs.add(new DatabaseTab(tabId, tabId, "", "", false, true));
        }
        return directory;
    }

    // ---------- transferTab ----------

    @Test
    void transferTabShouldReturnFalseWhenSourceEqualsTarget() throws ReflectiveOperationException {
        StoredItemDatabase database = new StoredItemDatabase();
        database.store(new ItemStack(Items.STONE, 8), "blocks");
        boolean changed = (boolean) TRANSFER_TAB.invoke(null, database, "blocks", "blocks");
        assertFalse(changed);
        assertEquals("blocks", database.entries().values().iterator().next().tabId());
    }

    @Test
    void transferTabShouldMoveAllEntriesToTargetTab() throws ReflectiveOperationException {
        StoredItemDatabase database = new StoredItemDatabase();
        database.store(new ItemStack(Items.STONE, 8), "blocks");
        database.store(new ItemStack(Items.DIRT, 4), "blocks");
        database.store(new ItemStack(Items.COBBLESTONE, 16), "ores");

        boolean changed = (boolean) TRANSFER_TAB.invoke(null, database, "blocks", "building");
        assertTrue(changed);

        for (StoredStackEntry entry : database.entries().values()) {
            if (StoredStackKey.of(new ItemStack(Items.COBBLESTONE)).equals(
                    database.entries().keySet().stream()
                            .filter(k -> k.equals(StoredStackKey.of(new ItemStack(Items.COBBLESTONE))))
                            .findFirst().orElse(null))) {
                continue;
            }
            assertNotEquals("blocks", entry.tabId());
        }
    }

    @Test
    void transferTabShouldAlsoMoveUnresolvedEntries() throws ReflectiveOperationException {
        StoredItemDatabase database = new StoredItemDatabase();
        CompoundTag stackTag = DatabaseTestReflectionHelper.invalidStackTag("missing:ghost");
        UnresolvedStoredEntry unresolved = new UnresolvedStoredEntry(stackTag, 5L, "blocks", 10L, 8L);
        DatabaseTestReflectionHelper.forceUnresolvedEntry(database, unresolved);

        boolean changed = (boolean) TRANSFER_TAB.invoke(null, database, "blocks", "storage");
        assertTrue(changed);
        assertEquals("storage", database.unresolvedEntries().getFirst().tabId());
    }

    @Test
    void transferTabShouldReturnFalseWhenSourceTabHasNoEntries() throws ReflectiveOperationException {
        StoredItemDatabase database = new StoredItemDatabase();
        database.store(new ItemStack(Items.STONE, 8), "blocks");
        boolean changed = (boolean) TRANSFER_TAB.invoke(null, database, "empty_tab", "building");
        assertFalse(changed);
    }

    // ---------- moveEntryToTab ----------

    @Test
    void moveEntryToTabShouldReturnFalseForNullKey() throws ReflectiveOperationException {
        StoredItemDatabase database = new StoredItemDatabase();
        boolean changed = (boolean) MOVE_ENTRY_TO_TAB.invoke(null, database, null, "from", "to");
        assertFalse(changed);
    }

    @Test
    void moveEntryToTabShouldReturnFalseWhenKeyNotFound() throws ReflectiveOperationException {
        StoredItemDatabase database = new StoredItemDatabase();
        StoredStackKey key = StoredStackKey.of(new ItemStack(Items.STONE));
        boolean changed = (boolean) MOVE_ENTRY_TO_TAB.invoke(null, database, key, "from", "to");
        assertFalse(changed);
    }

    @Test
    void moveEntryToTabShouldReturnFalseWhenSourceTabMismatch() throws ReflectiveOperationException {
        StoredItemDatabase database = new StoredItemDatabase();
        StoredStackKey key = StoredStackKey.of(new ItemStack(Items.STONE));
        database.store(new ItemStack(Items.STONE, 8), "blocks");
        boolean changed = (boolean) MOVE_ENTRY_TO_TAB.invoke(null, database, key, "wrong_tab", "building");
        assertFalse(changed);
        assertEquals("blocks", database.entries().get(key).tabId());
    }

    @Test
    void moveEntryToTabShouldReturnFalseWhenAlreadyInTargetTab() throws ReflectiveOperationException {
        StoredItemDatabase database = new StoredItemDatabase();
        StoredStackKey key = StoredStackKey.of(new ItemStack(Items.STONE));
        database.store(new ItemStack(Items.STONE, 8), "blocks");
        boolean changed = (boolean) MOVE_ENTRY_TO_TAB.invoke(null, database, key, "blocks", "blocks");
        assertFalse(changed);
    }

    // ---------- ensureTabAssignments ----------

    @Test
    void ensureTabAssignmentsShouldReturnFalseForNullDirectory() throws ReflectiveOperationException {
        StoredItemDatabase database = new StoredItemDatabase();
        database.store(new ItemStack(Items.STONE, 8));
        boolean changed = (boolean) ENSURE_TAB_ASSIGNMENTS.invoke(null, database, null);
        assertFalse(changed);
    }

    @Test
    void ensureTabAssignmentsShouldReturnFalseWhenAllTabsValid() throws ReflectiveOperationException {
        StoredItemDatabase database = new StoredItemDatabase();
        DatabaseTabDirectory directory = directoryWith("blocks", "tools");
        database.store(new ItemStack(Items.STONE, 8), "blocks");
        database.store(new ItemStack(Items.DIAMOND_PICKAXE, 1), "tools");
        boolean changed = (boolean) ENSURE_TAB_ASSIGNMENTS.invoke(null, database, directory);
        assertFalse(changed);
    }

    @Test
    void ensureTabAssignmentsShouldFallbackOrphanEntryToDefaultTab() throws ReflectiveOperationException {
        StoredItemDatabase database = new StoredItemDatabase();
        DatabaseTabDirectory directory = directoryWith("blocks");
        database.store(new ItemStack(Items.STONE, 8), "blocks");
        database.store(new ItemStack(Items.DIAMOND, 3), "deleted_tab");

        boolean changed = (boolean) ENSURE_TAB_ASSIGNMENTS.invoke(null, database, directory);
        assertTrue(changed);

        StoredStackKey diamondKey = StoredStackKey.of(new ItemStack(Items.DIAMOND));
        assertEquals(directory.defaultConcreteTab().id(), database.entries().get(diamondKey).tabId());
    }

    @Test
    void ensureTabAssignmentsShouldFallbackOrphanUnresolvedEntry() throws ReflectiveOperationException {
        StoredItemDatabase database = new StoredItemDatabase();
        DatabaseTabDirectory directory = directoryWith("blocks");
        CompoundTag stackTag = DatabaseTestReflectionHelper.invalidStackTag("missing:ghost");
        UnresolvedStoredEntry unresolved = new UnresolvedStoredEntry(stackTag, 5L, "deleted_tab", 10L, 8L);
        DatabaseTestReflectionHelper.forceUnresolvedEntry(database, unresolved);

        boolean changed = (boolean) ENSURE_TAB_ASSIGNMENTS.invoke(null, database, directory);
        assertTrue(changed);
        assertEquals(directory.defaultConcreteTab().id(), database.unresolvedEntries().getFirst().tabId());
    }

    @Test
    void ensureTabAssignmentsShouldReturnFalseWhenNoChangesNeeded() throws ReflectiveOperationException {
        StoredItemDatabase database = new StoredItemDatabase();
        DatabaseTabDirectory directory = new DatabaseTabDirectory();
        boolean changed = (boolean) ENSURE_TAB_ASSIGNMENTS.invoke(null, database, directory);
        assertFalse(changed);
    }

    @Test
    void ensureTabAssignmentsShouldPreserveEntriesInValidTabs() throws ReflectiveOperationException {
        StoredItemDatabase database = new StoredItemDatabase();
        DatabaseTabDirectory directory = directoryWith("blocks", "ores");
        database.store(new ItemStack(Items.STONE, 8), "blocks");
        database.store(new ItemStack(Items.COAL, 5), "ores");
        DatabaseTestReflectionHelper.forceEntry(database,
                StoredStackKey.of(new ItemStack(Items.DIAMOND)),
                new StoredStackEntry("deleted_tab", 3L, 1L));

        boolean changed = (boolean) ENSURE_TAB_ASSIGNMENTS.invoke(null, database, directory);
        assertTrue(changed);
        assertEquals("blocks", database.entries().get(StoredStackKey.of(new ItemStack(Items.STONE))).tabId());
        assertEquals("ores", database.entries().get(StoredStackKey.of(new ItemStack(Items.COAL))).tabId());
    }
}
