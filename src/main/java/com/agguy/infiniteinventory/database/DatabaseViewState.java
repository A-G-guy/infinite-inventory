package com.agguy.infiniteinventory.database;

import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;

public record DatabaseViewState(
        int containerId,
        long sessionId,
        DatabaseQuery query,
        DatabaseEnhancementConfig enhancementConfig,
        DatabaseAutoStoreTarget autoStoreTarget,
        List<DatabaseTab> personalTabs,
        List<DatabaseTab> publicTabs,
        List<DatabasePanelView> panels
) {
    public DatabaseViewState {
        query = query == null ? DatabaseQuery.defaultQuery() : query;
        enhancementConfig = enhancementConfig == null ? DatabaseEnhancementConfig.defaultConfig() : enhancementConfig;
        autoStoreTarget = autoStoreTarget == null ? DatabaseAutoStoreTarget.defaultTarget() : autoStoreTarget;
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
                DatabaseEnhancementConfig.defaultConfig(),
                DatabaseAutoStoreTarget.defaultTarget(),
                List.of(DatabaseTabs.allTab(), DatabaseTabs.defaultConcreteTab()),
                List.of(DatabaseTabs.allTab(), DatabaseTabs.defaultConcreteTab()),
                List.of()
        );
    }

    public List<DatabaseTab> tabsForScope(DatabaseScope scope) {
        return DatabaseScope.normalize(scope) == DatabaseScope.PUBLIC ? this.publicTabs : this.personalTabs;
    }

    public DatabaseQuery queryForScope(DatabaseScope scope) {
        return DatabaseQuery.normalizeForScope(scope, this.query);
    }

    public DatabaseQuery personalQuery() {
        return this.queryForScope(DatabaseScope.PERSONAL);
    }

    public DatabaseQuery publicQuery() {
        return this.queryForScope(DatabaseScope.PUBLIC);
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
            if (panel.scopedTab().equals(this.query.focusedTab())) {
                return panel;
            }
        }
        return this.panels.isEmpty() ? null : this.panels.getFirst();
    }

    public static DatabaseViewState read(RegistryFriendlyByteBuf buffer) {
        int containerId = buffer.readVarInt();
        long sessionId = buffer.readVarLong();
        DatabaseQuery query = DatabaseQuery.read(buffer);
        DatabaseEnhancementConfig enhancementConfig = DatabaseEnhancementConfig.read(buffer);
        DatabaseAutoStoreTarget autoStoreTarget = DatabaseAutoStoreTarget.read(buffer);
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
                enhancementConfig,
                autoStoreTarget,
                personalTabs,
                publicTabs,
                panels
        );
    }

    public void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(this.containerId);
        buffer.writeVarLong(this.sessionId);
        DatabaseQuery.write(buffer, this.query);
        DatabaseEnhancementConfig.write(buffer, this.enhancementConfig);
        DatabaseAutoStoreTarget.write(buffer, this.autoStoreTarget);
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
