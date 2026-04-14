package com.agguy.infiniteinventory.database;

import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;

public record DatabaseViewState(
        int containerId,
        long sessionId,
        DatabaseQuery query,
        DatabaseQuery personalQuery,
        DatabaseQuery publicQuery,
        DatabaseEnhancementConfig enhancementConfig,
        String autoStoreTargetTabId,
        List<DatabaseTab> personalTabs,
        List<DatabaseTab> publicTabs,
        List<DatabasePanelView> panels
) {
    public DatabaseViewState {
        query = query == null ? DatabaseQuery.defaultQuery() : query;
        personalQuery = DatabaseQuery.normalizeForScope(DatabaseScope.PERSONAL, personalQuery);
        publicQuery = DatabaseQuery.normalizeForScope(DatabaseScope.PUBLIC, publicQuery);
        if (query.scope() == DatabaseScope.PUBLIC) {
            publicQuery = query;
        } else {
            personalQuery = query;
        }
        enhancementConfig = enhancementConfig == null ? DatabaseEnhancementConfig.defaultConfig() : enhancementConfig;
        autoStoreTargetTabId = DatabaseTabs.normalizeConcreteTarget(autoStoreTargetTabId);
        sessionId = Math.max(0L, sessionId);
        personalTabs = copyTabs(personalTabs);
        publicTabs = copyTabs(publicTabs);
        panels = List.copyOf(panels);
    }

    public static DatabaseViewState empty(int containerId) {
        return empty(containerId, 0L, DatabaseQuery.defaultQuery());
    }

    public static DatabaseViewState empty(int containerId, DatabaseQuery query) {
        return empty(containerId, 0L, query);
    }

    public static DatabaseViewState empty(int containerId, long sessionId, DatabaseQuery query) {
        return new DatabaseViewState(
                containerId,
                sessionId,
                query,
                DatabaseQuery.defaultQuery(DatabaseScope.PERSONAL),
                DatabaseQuery.defaultQuery(DatabaseScope.PUBLIC),
                DatabaseEnhancementConfig.defaultConfig(),
                DatabaseTabs.DEFAULT_TAB_ID,
                List.of(DatabaseTabs.allTab(), DatabaseTabs.defaultConcreteTab()),
                List.of(DatabaseTabs.allTab(), DatabaseTabs.defaultConcreteTab()),
                List.of()
        );
    }

    public DatabaseQuery queryForScope(DatabaseScope scope) {
        return DatabaseScope.normalize(scope) == DatabaseScope.PUBLIC ? this.publicQuery : this.personalQuery;
    }

    public List<DatabaseTab> tabsForScope(DatabaseScope scope) {
        return DatabaseScope.normalize(scope) == DatabaseScope.PUBLIC ? this.publicTabs : this.personalTabs;
    }

    public List<VisibleDatabaseEntry> entries() {
        DatabasePanelView focusedPanel = this.focusedPanel();
        return focusedPanel == null ? List.of() : focusedPanel.entries();
    }

    public int totalEntries() {
        DatabasePanelView focusedPanel = this.focusedPanel();
        return focusedPanel == null ? 0 : focusedPanel.totalEntries();
    }

    public int totalPages() {
        DatabasePanelView focusedPanel = this.focusedPanel();
        return focusedPanel == null ? 1 : focusedPanel.totalPages();
    }

    public long totalItems() {
        DatabasePanelView focusedPanel = this.focusedPanel();
        return focusedPanel == null ? 0L : focusedPanel.totalItems();
    }

    public DatabasePanelView focusedPanel() {
        for (DatabasePanelView panel : this.panels) {
            if (panel.tab().id().equals(this.query.focusedTabId())) {
                return panel;
            }
        }
        return this.panels.isEmpty() ? null : this.panels.getFirst();
    }

    public static DatabaseViewState read(RegistryFriendlyByteBuf buffer) {
        int containerId = buffer.readVarInt();
        long sessionId = buffer.readVarLong();
        DatabaseQuery query = DatabaseQuery.read(buffer);
        DatabaseQuery personalQuery = DatabaseQuery.read(buffer);
        DatabaseQuery publicQuery = DatabaseQuery.read(buffer);
        DatabaseEnhancementConfig enhancementConfig = DatabaseEnhancementConfig.read(buffer);
        String autoStoreTargetTabId = buffer.readUtf(DatabaseQuery.MAX_TAB_ID_LENGTH);
        List<DatabaseTab> personalTabs = readTabs(buffer);
        List<DatabaseTab> publicTabs = readTabs(buffer);
        int panelCount = buffer.readVarInt();
        java.util.ArrayList<DatabasePanelView> panels = new java.util.ArrayList<>(panelCount);
        for (int index = 0; index < panelCount; index++) {
            panels.add(DatabasePanelView.read(buffer));
        }
        return new DatabaseViewState(
                containerId,
                sessionId,
                query,
                personalQuery,
                publicQuery,
                enhancementConfig,
                autoStoreTargetTabId,
                personalTabs,
                publicTabs,
                panels
        );
    }

    public void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(this.containerId);
        buffer.writeVarLong(this.sessionId);
        DatabaseQuery.write(buffer, this.query);
        DatabaseQuery.write(buffer, this.personalQuery);
        DatabaseQuery.write(buffer, this.publicQuery);
        DatabaseEnhancementConfig.write(buffer, this.enhancementConfig);
        buffer.writeUtf(this.autoStoreTargetTabId, DatabaseQuery.MAX_TAB_ID_LENGTH);
        writeTabs(buffer, this.personalTabs);
        writeTabs(buffer, this.publicTabs);
        buffer.writeVarInt(this.panels.size());
        for (DatabasePanelView panel : this.panels) {
            panel.write(buffer);
        }
    }

    private static List<DatabaseTab> copyTabs(List<DatabaseTab> tabs) {
        if (tabs == null || tabs.isEmpty()) {
            return List.of(DatabaseTabs.allTab(), DatabaseTabs.defaultConcreteTab());
        }
        java.util.ArrayList<DatabaseTab> copiedTabs = new java.util.ArrayList<>(tabs.size());
        for (DatabaseTab tab : tabs) {
            copiedTabs.add(tab == null ? DatabaseTabs.defaultConcreteTab() : tab);
        }
        return List.copyOf(copiedTabs);
    }

    private static List<DatabaseTab> readTabs(RegistryFriendlyByteBuf buffer) {
        int tabCount = buffer.readVarInt();
        java.util.ArrayList<DatabaseTab> tabs = new java.util.ArrayList<>(tabCount);
        for (int index = 0; index < tabCount; index++) {
            tabs.add(DatabaseTab.read(buffer));
        }
        return tabs;
    }

    private static void writeTabs(RegistryFriendlyByteBuf buffer, List<DatabaseTab> tabs) {
        buffer.writeVarInt(tabs.size());
        for (DatabaseTab tab : tabs) {
            tab.write(buffer);
        }
    }
}
