package com.agguy.infiniteinventory.database.tests;

import com.agguy.infiniteinventory.database.*;
import com.agguy.infiniteinventory.tests.MinecraftTestBootstrap;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DatabaseQuerySupport 包级可见方法的白盒测试。
 *
 * <p>通过反射调用 package-private 方法，覆盖序列化/反序列化、
 * 规范化、scope 重定向与旧格式兼容等关键路径。</p>
 */
class DatabaseQuerySupportTest {
    private static final Class<?> SUPPORT_CLASS;
    private static final Method NORMALIZE_SCOPED_TAB;
    private static final Method NORMALIZE_VISIBLE_TABS;
    private static final Method NORMALIZE_TAB_STATES;
    private static final Method TO_SCOPED_REFS;
    private static final Method BUILD_LEGACY_TAB_STATES;
    private static final Method RETARGET_SCOPE;
    private static final Method QUERY_FOR_SCOPE;
    private static final Method TO_TAG;
    private static final Method FROM_TAG;

    static {
        MinecraftTestBootstrap.ensureBootstrapped();
        try {
            SUPPORT_CLASS = Class.forName("com.agguy.infiniteinventory.database.DatabaseQuerySupport");
            NORMALIZE_SCOPED_TAB = SUPPORT_CLASS.getDeclaredMethod("normalizeScopedTab", DatabaseScopedTabRef.class, DatabaseScopedTabRef.class);
            NORMALIZE_SCOPED_TAB.setAccessible(true);
            NORMALIZE_VISIBLE_TABS = SUPPORT_CLASS.getDeclaredMethod("normalizeVisibleTabs", List.class, DatabaseScopedTabRef.class);
            NORMALIZE_VISIBLE_TABS.setAccessible(true);
            NORMALIZE_TAB_STATES = SUPPORT_CLASS.getDeclaredMethod("normalizeTabStates", Map.class, List.class, DatabaseScopedTabRef.class);
            NORMALIZE_TAB_STATES.setAccessible(true);
            TO_SCOPED_REFS = SUPPORT_CLASS.getDeclaredMethod("toScopedRefs", DatabaseScope.class, List.class);
            TO_SCOPED_REFS.setAccessible(true);
            BUILD_LEGACY_TAB_STATES = SUPPORT_CLASS.getDeclaredMethod("buildLegacyTabStates",
                    DatabaseScope.class, String.class, List.class, Map.class, Map.class,
                    DatabaseSortOption.class, String.class, DatabaseSearchConfig.class);
            BUILD_LEGACY_TAB_STATES.setAccessible(true);
            RETARGET_SCOPE = SUPPORT_CLASS.getDeclaredMethod("retargetScope", DatabaseQuery.class, DatabaseScope.class);
            RETARGET_SCOPE.setAccessible(true);
            QUERY_FOR_SCOPE = SUPPORT_CLASS.getDeclaredMethod("queryForScope", DatabaseQuery.class, DatabaseScope.class);
            QUERY_FOR_SCOPE.setAccessible(true);
            TO_TAG = SUPPORT_CLASS.getDeclaredMethod("toTag", DatabaseQuery.class);
            TO_TAG.setAccessible(true);
            FROM_TAG = SUPPORT_CLASS.getDeclaredMethod("fromTag", CompoundTag.class, DatabaseScope.class);
            FROM_TAG.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    // ---------- normalizeScopedTab ----------

    @Test
    void normalizeScopedTabShouldReturnFallbackWhenNull() throws ReflectiveOperationException {
        DatabaseScopedTabRef fallback = DatabaseScopedTabRef.allTab(DatabaseScope.PERSONAL);
        assertEquals(fallback, NORMALIZE_SCOPED_TAB.invoke(null, null, fallback));
    }

    @Test
    void normalizeScopedTabShouldReturnInputWhenNonNull() throws ReflectiveOperationException {
        DatabaseScopedTabRef tab = DatabaseScopedTabRef.concreteTab(DatabaseScope.PUBLIC, "test_tab");
        DatabaseScopedTabRef fallback = DatabaseScopedTabRef.allTab(DatabaseScope.PERSONAL);
        assertEquals(tab, NORMALIZE_SCOPED_TAB.invoke(null, tab, fallback));
    }

    // ---------- normalizeVisibleTabs ----------

    @Test
    @SuppressWarnings("unchecked")
    void normalizeVisibleTabsShouldReturnFallbackWhenListIsEmpty() throws ReflectiveOperationException {
        DatabaseScopedTabRef fallback = DatabaseScopedTabRef.allTab(DatabaseScope.PERSONAL);
        List<DatabaseScopedTabRef> result = (List<DatabaseScopedTabRef>) NORMALIZE_VISIBLE_TABS.invoke(null, List.of(), fallback);
        assertEquals(1, result.size());
        assertEquals(fallback, result.getFirst());
    }

    @Test
    @SuppressWarnings("unchecked")
    void normalizeVisibleTabsShouldReturnFallbackWhenListIsNull() throws ReflectiveOperationException {
        DatabaseScopedTabRef fallback = DatabaseScopedTabRef.allTab(DatabaseScope.PERSONAL);
        List<DatabaseScopedTabRef> result = (List<DatabaseScopedTabRef>) NORMALIZE_VISIBLE_TABS.invoke(null, null, fallback);
        assertEquals(1, result.size());
        assertEquals(fallback, result.getFirst());
    }

    @Test
    @SuppressWarnings("unchecked")
    void normalizeVisibleTabsShouldDeduplicateEntries() throws ReflectiveOperationException {
        DatabaseScopedTabRef tab = DatabaseScopedTabRef.concreteTab(DatabaseScope.PERSONAL, "blocks");
        List<DatabaseScopedTabRef> input = List.of(tab, tab, tab);
        List<DatabaseScopedTabRef> result = (List<DatabaseScopedTabRef>) NORMALIZE_VISIBLE_TABS.invoke(null, input, tab);
        assertEquals(1, result.size());
        assertEquals(tab, result.getFirst());
    }

    @Test
    @SuppressWarnings("unchecked")
    void normalizeVisibleTabsShouldCapAtMaxVisibleTabCount() throws ReflectiveOperationException {
        DatabaseScopedTabRef fallback = DatabaseScopedTabRef.concreteTab(DatabaseScope.PERSONAL, "base");
        List<DatabaseScopedTabRef> input = new ArrayList<>();
        for (int i = 0; i < DatabaseTabs.MAX_VISIBLE_TAB_COUNT + 10; i++) {
            input.add(DatabaseScopedTabRef.concreteTab(DatabaseScope.PERSONAL, "tab" + i));
        }
        List<DatabaseScopedTabRef> result = (List<DatabaseScopedTabRef>) NORMALIZE_VISIBLE_TABS.invoke(null, input, fallback);
        assertTrue(result.size() <= DatabaseTabs.MAX_VISIBLE_TAB_COUNT);
    }

    // ---------- normalizeTabStates ----------

    @Test
    @SuppressWarnings("unchecked")
    void normalizeTabStatesShouldEnsureFocusedTabEntry() throws ReflectiveOperationException {
        DatabaseScopedTabRef focused = DatabaseScopedTabRef.concreteTab(DatabaseScope.PERSONAL, "focused");
        List<DatabaseScopedTabRef> visible = List.of(focused);
        Map<DatabaseScopedTabRef, DatabaseTabQueryState> result = (Map<DatabaseScopedTabRef, DatabaseTabQueryState>)
                NORMALIZE_TAB_STATES.invoke(null, Map.of(), visible, focused);
        assertTrue(result.containsKey(focused));
    }

    @Test
    @SuppressWarnings("unchecked")
    void normalizeTabStatesShouldPreserveExistingState() throws ReflectiveOperationException {
        DatabaseScopedTabRef tab = DatabaseScopedTabRef.concreteTab(DatabaseScope.PERSONAL, "blocks");
        DatabaseTabQueryState state = new DatabaseTabQueryState(
                DatabaseSortOption.NAME_ASC, "diamond", DatabaseSearchConfig.defaultConfig(), 3, 30);
        Map<DatabaseScopedTabRef, DatabaseTabQueryState> input = Map.of(tab, state);
        List<DatabaseScopedTabRef> visible = List.of(tab);
        Map<DatabaseScopedTabRef, DatabaseTabQueryState> result = (Map<DatabaseScopedTabRef, DatabaseTabQueryState>)
                NORMALIZE_TAB_STATES.invoke(null, input, visible, tab);
        assertEquals(state, result.get(tab));
    }

    @Test
    @SuppressWarnings("unchecked")
    void normalizeTabStatesShouldDefaultNullState() throws ReflectiveOperationException {
        DatabaseScopedTabRef tab = DatabaseScopedTabRef.concreteTab(DatabaseScope.PERSONAL, "blocks");
        Map<DatabaseScopedTabRef, DatabaseTabQueryState> input = new HashMap<>();
        input.put(tab, null);
        List<DatabaseScopedTabRef> visible = List.of(tab);
        Map<DatabaseScopedTabRef, DatabaseTabQueryState> result = (Map<DatabaseScopedTabRef, DatabaseTabQueryState>)
                NORMALIZE_TAB_STATES.invoke(null, input, visible, tab);
        assertEquals(DatabaseTabQueryState.defaultState(), result.get(tab));
    }

    // ---------- toScopedRefs ----------

    @Test
    @SuppressWarnings("unchecked")
    void toScopedRefsShouldReturnAllTabWhenListIsNull() throws ReflectiveOperationException {
        List<DatabaseScopedTabRef> result = (List<DatabaseScopedTabRef>) TO_SCOPED_REFS.invoke(null, DatabaseScope.PERSONAL, null);
        assertEquals(1, result.size());
        assertTrue(result.getFirst().isAllTab());
        assertEquals(DatabaseScope.PERSONAL, result.getFirst().scope());
    }

    @Test
    @SuppressWarnings("unchecked")
    void toScopedRefsShouldReturnAllTabWhenListIsEmpty() throws ReflectiveOperationException {
        List<DatabaseScopedTabRef> result = (List<DatabaseScopedTabRef>) TO_SCOPED_REFS.invoke(null, DatabaseScope.PUBLIC, List.of());
        assertEquals(1, result.size());
        assertTrue(result.getFirst().isAllTab());
        assertEquals(DatabaseScope.PUBLIC, result.getFirst().scope());
    }

    @Test
    @SuppressWarnings("unchecked")
    void toScopedRefsShouldConvertTabIdsToScopedRefs() throws ReflectiveOperationException {
        List<String> tabIds = List.of("blocks", "tools", "food");
        List<DatabaseScopedTabRef> result = (List<DatabaseScopedTabRef>) TO_SCOPED_REFS.invoke(null, DatabaseScope.PERSONAL, tabIds);
        assertEquals(3, result.size());
        assertEquals("blocks", result.get(0).tabId());
        assertEquals("tools", result.get(1).tabId());
        assertEquals("food", result.get(2).tabId());
        result.forEach(ref -> assertEquals(DatabaseScope.PERSONAL, ref.scope()));
    }

    // ---------- buildLegacyTabStates ----------

    @Test
    @SuppressWarnings("unchecked")
    void buildLegacyTabStatesShouldBuildStatesFromLegacyData() throws ReflectiveOperationException {
        Map<String, Integer> pageIndexes = Map.of("blocks", 2);
        Map<String, Integer> pageSizes = Map.of("blocks", 30);
        Map<DatabaseScopedTabRef, DatabaseTabQueryState> result = (Map<DatabaseScopedTabRef, DatabaseTabQueryState>)
                BUILD_LEGACY_TAB_STATES.invoke(null, DatabaseScope.PERSONAL, "blocks",
                        List.of("blocks"), pageIndexes, pageSizes,
                        DatabaseSortOption.NAME_ASC, "test", DatabaseSearchConfig.defaultConfig());
        assertFalse(result.isEmpty());
        DatabaseScopedTabRef blocksRef = DatabaseScopedTabRef.concreteTab(DatabaseScope.PERSONAL, "blocks");
        assertTrue(result.containsKey(blocksRef));
        assertEquals(2, result.get(blocksRef).pageIndex());
        assertEquals(30, result.get(blocksRef).pageSize());
    }

    // ---------- retargetScope ----------

    @Test
    void retargetScopeShouldSwitchAllRefsToNewScope() throws ReflectiveOperationException {
        DatabaseQuery original = new DatabaseQuery(
                DatabaseScopedTabRef.concreteTab(DatabaseScope.PERSONAL, "blocks"),
                List.of(DatabaseScopedTabRef.concreteTab(DatabaseScope.PERSONAL, "blocks")),
                Map.of(DatabaseScopedTabRef.concreteTab(DatabaseScope.PERSONAL, "blocks"), DatabaseTabQueryState.defaultState()),
                List.of(DatabaseScopedTabRef.concreteTab(DatabaseScope.PERSONAL, "hidden"))
        );
        DatabaseQuery retargeted = (DatabaseQuery) RETARGET_SCOPE.invoke(null, original, DatabaseScope.PUBLIC);
        assertEquals(DatabaseScope.PUBLIC, retargeted.focusedTab().scope());
        retargeted.visibleTabs().forEach(ref -> assertEquals(DatabaseScope.PUBLIC, ref.scope()));
        retargeted.tabStates().keySet().forEach(ref -> assertEquals(DatabaseScope.PUBLIC, ref.scope()));
        retargeted.hiddenTopTabs().forEach(ref -> assertEquals(DatabaseScope.PUBLIC, ref.scope()));
    }

    // ---------- queryForScope ----------

    @Test
    void queryForScopeShouldFilterToSpecificScope() throws ReflectiveOperationException {
        DatabaseQuery original = new DatabaseQuery(
                DatabaseScopedTabRef.concreteTab(DatabaseScope.PERSONAL, "blocks"),
                List.of(DatabaseScopedTabRef.concreteTab(DatabaseScope.PERSONAL, "blocks")),
                Map.of(
                        DatabaseScopedTabRef.concreteTab(DatabaseScope.PERSONAL, "blocks"),
                        DatabaseTabQueryState.defaultState(),
                        DatabaseScopedTabRef.concreteTab(DatabaseScope.PUBLIC, "blocks"),
                        DatabaseTabQueryState.defaultState()
                ),
                List.of()
        );
        DatabaseQuery personalQuery = (DatabaseQuery) QUERY_FOR_SCOPE.invoke(null, original, DatabaseScope.PERSONAL);
        assertEquals(DatabaseScope.PERSONAL, personalQuery.scope());
        personalQuery.visibleTabs().forEach(ref -> assertEquals(DatabaseScope.PERSONAL, ref.scope()));
        personalQuery.tabStates().keySet().forEach(ref -> assertEquals(DatabaseScope.PERSONAL, ref.scope()));
    }

    @Test
    void queryForScopeShouldFallbackToAllTabWhenNoVisibleTabs() throws ReflectiveOperationException {
        DatabaseQuery original = DatabaseQuery.defaultQuery(DatabaseScope.PERSONAL);
        DatabaseQuery publicQuery = (DatabaseQuery) QUERY_FOR_SCOPE.invoke(null, original, DatabaseScope.PUBLIC);
        assertEquals(DatabaseScope.PUBLIC, publicQuery.scope());
        assertFalse(publicQuery.visibleTabs().isEmpty());
    }

    // ---------- toTag / fromTag (NBT 序列化往返) ----------

    @Test
    void toTagAndFromTagShouldRoundTripCurrentFormat() throws ReflectiveOperationException {
        DatabaseQuery original = new DatabaseQuery(
                DatabaseScopedTabRef.concreteTab(DatabaseScope.PERSONAL, "blocks"),
                List.of(DatabaseScopedTabRef.concreteTab(DatabaseScope.PERSONAL, "blocks")),
                Map.of(DatabaseScopedTabRef.concreteTab(DatabaseScope.PERSONAL, "blocks"),
                        new DatabaseTabQueryState(DatabaseSortOption.COUNT_DESC, "diamond", DatabaseSearchConfig.defaultConfig(), 2, 30)),
                List.of()
        );
        CompoundTag tag = (CompoundTag) TO_TAG.invoke(null, original);
        DatabaseQuery restored = (DatabaseQuery) FROM_TAG.invoke(null, tag, DatabaseScope.PERSONAL);
        assertEquals(original.focusedTab(), restored.focusedTab());
        assertEquals(original.visibleTabs(), restored.visibleTabs());
        assertEquals(original.tabStates().keySet(), restored.tabStates().keySet());
    }

    @Test
    void fromTagShouldReturnDefaultQueryForNullTag() throws ReflectiveOperationException {
        DatabaseQuery result = (DatabaseQuery) FROM_TAG.invoke(null, null, DatabaseScope.PERSONAL);
        assertEquals(DatabaseScope.PERSONAL, result.scope());
        assertFalse(result.visibleTabs().isEmpty());
    }

    @Test
    void fromTagShouldReturnDefaultQueryForEmptyTag() throws ReflectiveOperationException {
        DatabaseQuery result = (DatabaseQuery) FROM_TAG.invoke(null, new CompoundTag(), DatabaseScope.PUBLIC);
        assertEquals(DatabaseScope.PUBLIC, result.scope());
    }

    @Test
    void fromTagShouldHandleLegacyFormatWithCategoryEnum() throws ReflectiveOperationException {
        CompoundTag legacyTag = new CompoundTag();
        legacyTag.putString("category", "BLOCKS");
        legacyTag.putString("sort_option", "NAME");
        legacyTag.putString("search_text", "stone");
        legacyTag.putInt("page_index", 1);
        legacyTag.putInt("page_size", 20);
        CompoundTag searchConfigTag = new CompoundTag();
        legacyTag.put("search_config", searchConfigTag);

        DatabaseQuery result = (DatabaseQuery) FROM_TAG.invoke(null, legacyTag, DatabaseScope.PERSONAL);
        assertEquals(DatabaseScope.PERSONAL, result.scope());
        assertEquals("stone", result.searchText());
    }

    @Test
    void fromTagShouldHandleLegacyFormatWithFocusedTabAndVisibleTabs() throws ReflectiveOperationException {
        CompoundTag legacyTag = new CompoundTag();
        legacyTag.putString("scope", "personal");
        legacyTag.putString("focused_tab_id", "ores");
        ListTag visibleTabsTag = new ListTag();
        CompoundTag visibleTabTag = new CompoundTag();
        visibleTabTag.putString("tab_id", "ores");
        visibleTabsTag.add(visibleTabTag);
        legacyTag.put("visible_tabs", visibleTabsTag);

        DatabaseQuery result = (DatabaseQuery) FROM_TAG.invoke(null, legacyTag, DatabaseScope.PERSONAL);
        assertEquals(DatabaseScope.PERSONAL, result.scope());
        assertEquals("ores", result.focusedTabId());
    }

    @Test
    void fromTagShouldHandleLegacyFormatWithTabStatesCompound() throws ReflectiveOperationException {
        CompoundTag legacyTag = new CompoundTag();
        legacyTag.putString("scope", "personal");
        legacyTag.putString("focused_tab_id", "ores");
        ListTag visibleTabsTag = new ListTag();
        CompoundTag visibleTabTag = new CompoundTag();
        visibleTabTag.putString("tab_id", "ores");
        visibleTabsTag.add(visibleTabTag);
        legacyTag.put("visible_tabs", visibleTabsTag);
        CompoundTag tabStatesTag = new CompoundTag();
        CompoundTag tabStateTag = new CompoundTag();
        tabStateTag.putString("sort_option", "AMOUNT");
        tabStateTag.putString("search_text", "");
        tabStateTag.putInt("page_index", 0);
        tabStateTag.putInt("page_size", 54);
        tabStatesTag.put("ores", tabStateTag);
        legacyTag.put("tab_states", tabStatesTag);

        DatabaseQuery result = (DatabaseQuery) FROM_TAG.invoke(null, legacyTag, DatabaseScope.PERSONAL);
        assertEquals(DatabaseScope.PERSONAL, result.scope());
        assertEquals("ores", result.focusedTabId());
    }

    @Test
    void fromTagShouldHandleLegacyFormatWithPageIndexesAndSizes() throws ReflectiveOperationException {
        CompoundTag legacyTag = new CompoundTag();
        legacyTag.putString("scope", "personal");
        legacyTag.putString("focused_tab_id", "blocks");
        ListTag visibleTabsTag = new ListTag();
        CompoundTag visibleTabTag = new CompoundTag();
        visibleTabTag.putString("tab_id", "blocks");
        visibleTabsTag.add(visibleTabTag);
        legacyTag.put("visible_tabs", visibleTabsTag);
        CompoundTag pageIndexesTag = new CompoundTag();
        pageIndexesTag.putInt("blocks", 3);
        legacyTag.put("page_indexes", pageIndexesTag);
        CompoundTag pageSizesTag = new CompoundTag();
        pageSizesTag.putInt("blocks", 40);
        legacyTag.put("page_sizes", pageSizesTag);
        legacyTag.putString("sort_option", "NAME");
        legacyTag.putString("search_text", "brick");

        DatabaseQuery result = (DatabaseQuery) FROM_TAG.invoke(null, legacyTag, DatabaseScope.PERSONAL);
        assertEquals(DatabaseScope.PERSONAL, result.scope());
        assertEquals("blocks", result.focusedTabId());
        assertEquals("brick", result.searchText());
    }

    @Test
    void fromTagShouldHandleLegacyVisibleTabWithoutScope() throws ReflectiveOperationException {
        CompoundTag tag = new CompoundTag();
        tag.putString("scope", "public");
        tag.putString("focused_tab_id", "items");
        ListTag visibleTabsTag = new ListTag();
        CompoundTag oldStyleTab = new CompoundTag();
        oldStyleTab.putString("tab_id", "items");
        visibleTabsTag.add(oldStyleTab);
        tag.put("visible_tabs", visibleTabsTag);
        CompoundTag tabStatesTag = new CompoundTag();
        tabStatesTag.put("items", new CompoundTag());
        tag.put("tab_states", tabStatesTag);

        DatabaseQuery result = (DatabaseQuery) FROM_TAG.invoke(null, tag, DatabaseScope.PUBLIC);
        assertEquals(DatabaseScope.PUBLIC, result.scope());
        assertEquals("items", result.focusedTabId());
    }

    // ---------- read / write (网络序列化往返) ----------

    @Test
    void readAndWriteShouldRoundTripThroughBuffer() {
        DatabaseQuery original = new DatabaseQuery(
                DatabaseScopedTabRef.concreteTab(DatabaseScope.PERSONAL, "blocks"),
                List.of(DatabaseScopedTabRef.concreteTab(DatabaseScope.PERSONAL, "blocks")),
                Map.of(DatabaseScopedTabRef.concreteTab(DatabaseScope.PERSONAL, "blocks"),
                        new DatabaseTabQueryState(DatabaseSortOption.NAME_ASC, "diamond", DatabaseSearchConfig.defaultConfig(), 2, 30)),
                List.of()
        );
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        DatabaseQuery.write(buffer, original);
        DatabaseQuery restored = DatabaseQuery.read(buffer);

        assertEquals(original.focusedTab(), restored.focusedTab());
        assertEquals(original.visibleTabs().size(), restored.visibleTabs().size());
        assertEquals(original.visibleTabs(), restored.visibleTabs());
        assertEquals(original.tabStates().keySet(), restored.tabStates().keySet());
        assertEquals(original.hiddenTopTabs(), restored.hiddenTopTabs());
    }

    @Test
    void readAndWriteShouldRoundTripWithHiddenTopTabs() {
        DatabaseQuery original = new DatabaseQuery(
                DatabaseScopedTabRef.allTab(DatabaseScope.PUBLIC),
                List.of(DatabaseScopedTabRef.allTab(DatabaseScope.PUBLIC)),
                Map.of(DatabaseScopedTabRef.allTab(DatabaseScope.PUBLIC), DatabaseTabQueryState.defaultState()),
                List.of(DatabaseScopedTabRef.concreteTab(DatabaseScope.PUBLIC, "hidden_1"),
                        DatabaseScopedTabRef.concreteTab(DatabaseScope.PUBLIC, "hidden_2"))
        );
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        DatabaseQuery.write(buffer, original);
        DatabaseQuery restored = DatabaseQuery.read(buffer);

        assertEquals(2, restored.hiddenTopTabs().size());
        assertEquals(original.hiddenTopTabs(), restored.hiddenTopTabs());
    }

    @Test
    void toTagAndFromTagShouldRoundTripWithHiddenTopTabs() throws ReflectiveOperationException {
        DatabaseQuery original = new DatabaseQuery(
                DatabaseScopedTabRef.allTab(DatabaseScope.PERSONAL),
                List.of(DatabaseScopedTabRef.allTab(DatabaseScope.PERSONAL)),
                Map.of(DatabaseScopedTabRef.allTab(DatabaseScope.PERSONAL), DatabaseTabQueryState.defaultState()),
                List.of(DatabaseScopedTabRef.concreteTab(DatabaseScope.PERSONAL, "hide_me"))
        );
        CompoundTag tag = (CompoundTag) TO_TAG.invoke(null, original);
        DatabaseQuery restored = (DatabaseQuery) FROM_TAG.invoke(null, tag, DatabaseScope.PERSONAL);

        assertEquals(1, restored.hiddenTopTabs().size());
        assertEquals("hide_me", restored.hiddenTopTabs().getFirst().tabId());
    }
}
