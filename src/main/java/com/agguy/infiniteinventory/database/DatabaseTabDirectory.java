package com.agguy.infiniteinventory.database;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

public final class DatabaseTabDirectory {
    private static final String TABS_KEY = "tabs";

    private final List<DatabaseTab> concreteTabs = new ArrayList<>();

    public DatabaseTabDirectory() {
        this.ensureDefaultConcreteTab();
    }

    public List<DatabaseTab> orderedTabs() {
        List<DatabaseTab> orderedTabs = new ArrayList<>(this.concreteTabs.size() + 1);
        orderedTabs.add(DatabaseTabs.allTab());
        orderedTabs.addAll(this.concreteTabs);
        return List.copyOf(orderedTabs);
    }

    public List<DatabaseTab> concreteTabs() {
        return List.copyOf(this.concreteTabs);
    }

    public DatabaseTab defaultConcreteTab() {
        return this.find(DatabaseTabs.DEFAULT_TAB_ID).orElseGet(DatabaseTabs::defaultConcreteTab);
    }

    public boolean contains(String tabId) {
        if (DatabaseTabs.isAllTabId(tabId)) {
            return true;
        }
        return this.concreteTabs.stream().anyMatch(tab -> tab.id().equals(tabId));
    }

    public boolean containsConcreteTab(String tabId) {
        return DatabaseTabs.isConcreteTabId(tabId) && this.concreteTabs.stream().anyMatch(tab -> tab.id().equals(tabId));
    }

    public java.util.Optional<DatabaseTab> find(String tabId) {
        if (DatabaseTabs.isAllTabId(tabId)) {
            return java.util.Optional.of(DatabaseTabs.allTab());
        }
        return this.concreteTabs.stream().filter(tab -> tab.id().equals(tabId)).findFirst();
    }

    public DatabaseTab resolve(String tabId) {
        return this.find(tabId).orElseGet(this::defaultConcreteTab);
    }

    public String resolveVisibleTabId(String requestedTabId) {
        if (requestedTabId == null || requestedTabId.isBlank()) {
            return DatabaseTabs.ALL_TAB_ID;
        }
        String normalizedTabId = requestedTabId.trim();
        if (DatabaseTabs.isAllTabId(normalizedTabId)) {
            return DatabaseTabs.ALL_TAB_ID;
        }
        if (this.containsConcreteTab(normalizedTabId)) {
            return normalizedTabId;
        }
        return null;
    }

    public String sanitizeConcreteTarget(String requestedTabId) {
        String normalizedTabId = DatabaseTabs.normalizeConcreteTarget(requestedTabId);
        if (this.containsConcreteTab(normalizedTabId)) {
            return normalizedTabId;
        }
        return this.defaultConcreteTab().id();
    }

    public DatabaseQuery sanitizeQuery(DatabaseQuery query) {
        DatabaseQuery normalizedQuery = query == null ? DatabaseQuery.defaultQuery() : query;
        String focusedTabId = this.resolveVisibleTabId(normalizedQuery.focusedTabId());
        if (focusedTabId == null) {
            focusedTabId = DatabaseTabs.ALL_TAB_ID;
        }
        List<String> visibleTabIds = DatabaseTabs.normalizeVisibleTabIds(normalizedQuery.visibleTabIds(), this, focusedTabId);
        if (!visibleTabIds.contains(focusedTabId)) {
            focusedTabId = visibleTabIds.getFirst();
        }

        Map<String, DatabaseTabQueryState> tabStates = new LinkedHashMap<>();
        for (DatabaseTab tab : this.orderedTabs()) {
            tabStates.put(tab.id(), normalizedQuery.tabStateFor(tab.id()));
        }
        return new DatabaseQuery(normalizedQuery.scope(), focusedTabId, visibleTabIds, tabStates);
    }

    public DatabaseTab addCustomTab(String name, String iconItemId) {
        String normalizedName = DatabaseTabs.normalizeTabName(name, "");
        DatabaseTab newTab;
        if (normalizedName.isBlank()) {
            newTab = new DatabaseTab(
                    DatabaseTabs.newCustomTabId(),
                    "",
                    DatabaseTabs.NEW_CUSTOM_TAB_TRANSLATION_KEY,
                    iconItemId,
                    false,
                    false
            );
        } else {
            newTab = new DatabaseTab(
                    DatabaseTabs.newCustomTabId(),
                    normalizedName,
                    "",
                    iconItemId,
                    false,
                    false
            );
        }
        this.concreteTabs.add(newTab);
        return newTab;
    }

    public boolean renameTab(String tabId, String name) {
        int index = this.indexOf(tabId);
        if (index < 0) {
            return false;
        }
        DatabaseTab renamedTab = this.concreteTabs.get(index).withName(name);
        if (renamedTab.equals(this.concreteTabs.get(index))) {
            return false;
        }
        this.concreteTabs.set(index, renamedTab);
        return true;
    }

    public boolean updateTabIcon(String tabId, String iconItemId) {
        int index = this.indexOf(tabId);
        if (index < 0) {
            return false;
        }
        DatabaseTab updatedTab = this.concreteTabs.get(index).withIconItemId(iconItemId);
        if (updatedTab.equals(this.concreteTabs.get(index))) {
            return false;
        }
        this.concreteTabs.set(index, updatedTab);
        return true;
    }

    public boolean moveTab(String tabId, int direction) {
        int index = this.indexOf(tabId);
        if (index < 0) {
            return false;
        }
        int nextIndex = Math.max(0, Math.min(this.concreteTabs.size() - 1, index + direction));
        if (nextIndex == index) {
            return false;
        }
        DatabaseTab movedTab = this.concreteTabs.remove(index);
        this.concreteTabs.add(nextIndex, movedTab);
        return true;
    }

    public boolean deleteTab(String tabId) {
        int index = this.indexOf(tabId);
        if (index < 0) {
            return false;
        }
        if (!this.concreteTabs.get(index).canDelete()) {
            return false;
        }
        this.concreteTabs.remove(index);
        this.ensureDefaultConcreteTab();
        return true;
    }

    public CompoundTag toTag() {
        CompoundTag root = new CompoundTag();
        ListTag tabs = new ListTag();
        for (DatabaseTab tab : this.concreteTabs) {
            tabs.add(tab.toTag());
        }
        root.put(TABS_KEY, tabs);
        return root;
    }

    public static DatabaseTabDirectory fromTag(CompoundTag tag) {
        DatabaseTabDirectory directory = new DatabaseTabDirectory();
        directory.concreteTabs.clear();
        if (tag != null) {
            for (Tag entry : tag.getList(TABS_KEY, Tag.TAG_COMPOUND)) {
                if (!(entry instanceof CompoundTag tabTag)) {
                    continue;
                }
                DatabaseTab tab = DatabaseTab.fromTag(tabTag);
                if (!tab.isConcreteTab()) {
                    continue;
                }
                directory.concreteTabs.add(tab);
            }
        }
        directory.ensureDefaultConcreteTab();
        directory.deduplicateConcreteTabs();
        return directory;
    }

    private int indexOf(String tabId) {
        for (int index = 0; index < this.concreteTabs.size(); index++) {
            if (Objects.equals(this.concreteTabs.get(index).id(), tabId)) {
                return index;
            }
        }
        return -1;
    }

    private void ensureDefaultConcreteTab() {
        if (this.concreteTabs.stream().noneMatch(tab -> DatabaseTabs.DEFAULT_TAB_ID.equals(tab.id()))) {
            this.concreteTabs.add(0, DatabaseTabs.defaultConcreteTab());
            return;
        }
        int defaultIndex = this.indexOf(DatabaseTabs.DEFAULT_TAB_ID);
        if (defaultIndex > 0) {
            DatabaseTab defaultTab = this.concreteTabs.remove(defaultIndex);
            this.concreteTabs.add(0, defaultTab);
        }
    }

    private void deduplicateConcreteTabs() {
        Map<String, DatabaseTab> tabsById = new LinkedHashMap<>();
        for (DatabaseTab tab : this.concreteTabs) {
            tabsById.putIfAbsent(tab.id(), tab);
        }
        this.concreteTabs.clear();
        this.concreteTabs.addAll(tabsById.values());
        this.ensureDefaultConcreteTab();
    }
}
