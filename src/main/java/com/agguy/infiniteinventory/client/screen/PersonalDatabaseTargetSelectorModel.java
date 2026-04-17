package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.database.DatabasePanelView;
import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.DatabaseTab;
import com.agguy.infiniteinventory.database.DatabaseTabs;
import com.agguy.infiniteinventory.database.DatabaseViewState;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import org.jetbrains.annotations.Nullable;

final class PersonalDatabaseTargetSelectorModel {
    private PersonalDatabaseTargetSelectorModel() {
    }

    static List<Row> buildRows(
            PersonalDatabaseScreen.TargetSelectorMode mode,
            @Nullable DatabaseViewState viewState,
            @Nullable String sourceTabId,
            List<DatabasePanelView> currentPanels
    ) {
        if (mode == null || viewState == null) {
            return List.of();
        }
        return switch (mode) {
            case TRANSFER_SELECTION, TRANSFER_TAB -> buildTransferRows(mode, viewState, sourceTabId);
            case AUTO_STORE_TARGET -> buildAutoStoreTargetRows(viewState);
            case DEPOSIT_ALL, CARRIED_STORE, QUICK_DEPOSIT, DELETE_TAB -> buildFlatRows(
                    viewState.query().scope(),
                    candidateTabs(mode, viewState, sourceTabId, currentPanels)
            );
            case NONE -> List.of();
        };
    }

    private static List<Row> buildTransferRows(
            PersonalDatabaseScreen.TargetSelectorMode mode,
            DatabaseViewState viewState,
            @Nullable String sourceTabId
    ) {
        DatabaseScope sourceScope = viewState.query().scope();
        DatabaseScope otherScope = sourceScope == DatabaseScope.PUBLIC ? DatabaseScope.PERSONAL : DatabaseScope.PUBLIC;
        List<Row> rows = new ArrayList<>();
        appendTransferGroup(rows, sourceScope, true, transferTabsForScope(mode, viewState, sourceScope, sourceTabId));
        appendTransferGroup(rows, otherScope, false, transferTabsForScope(mode, viewState, otherScope, sourceTabId));
        return List.copyOf(rows);
    }

    private static void appendTransferGroup(
            List<Row> rows,
            DatabaseScope scope,
            boolean sourceScopeGroup,
            List<DatabaseTab> tabs
    ) {
        if (tabs.isEmpty()) {
            return;
        }
        rows.add(Row.header(scope, sourceScopeGroup));
        for (DatabaseTab tab : tabs) {
            rows.add(Row.target(scope, tab));
        }
    }

    private static List<Row> buildAutoStoreTargetRows(DatabaseViewState viewState) {
        List<Row> rows = new ArrayList<>();
        appendTransferGroup(rows, DatabaseScope.PERSONAL, false, concreteTabsForScope(viewState, DatabaseScope.PERSONAL));
        appendTransferGroup(rows, DatabaseScope.PUBLIC, false, concreteTabsForScope(viewState, DatabaseScope.PUBLIC));
        return List.copyOf(rows);
    }

    private static List<DatabaseTab> transferTabsForScope(
            PersonalDatabaseScreen.TargetSelectorMode mode,
            DatabaseViewState viewState,
            DatabaseScope scope,
            @Nullable String sourceTabId
    ) {
        String normalizedSourceTabId = sourceTabId == null || sourceTabId.isBlank()
                ? null
                : DatabaseTabs.normalizeConcreteTarget(sourceTabId);
        return viewState.tabsForScope(scope).stream()
                .filter(DatabaseTab::isConcreteTab)
                .filter(tab -> mode != PersonalDatabaseScreen.TargetSelectorMode.TRANSFER_TAB
                        || scope != viewState.query().scope()
                        || normalizedSourceTabId == null
                        || !tab.id().equals(normalizedSourceTabId))
                .toList();
    }

    private static List<Row> buildFlatRows(DatabaseScope targetScope, List<DatabaseTab> tabs) {
        if (tabs.isEmpty()) {
            return List.of();
        }
        List<Row> rows = new ArrayList<>(tabs.size());
        for (DatabaseTab tab : tabs) {
            rows.add(Row.target(targetScope, tab));
        }
        return List.copyOf(rows);
    }

    private static List<DatabaseTab> candidateTabs(
            PersonalDatabaseScreen.TargetSelectorMode mode,
            DatabaseViewState viewState,
            @Nullable String sourceTabId,
            List<DatabasePanelView> currentPanels
    ) {
        return switch (mode) {
            case DEPOSIT_ALL, CARRIED_STORE, QUICK_DEPOSIT -> visibleConcreteTabs(viewState, currentPanels);
            case DELETE_TAB -> viewState.tabsForScope(viewState.query().scope()).stream()
                    .filter(DatabaseTab::isConcreteTab)
                    .filter(tab -> !tab.id().equals(sourceTabId == null ? "" : sourceTabId))
                    .toList();
            case NONE, TRANSFER_SELECTION, TRANSFER_TAB, AUTO_STORE_TARGET -> List.of();
        };
    }

    private static List<DatabaseTab> concreteTabsForScope(DatabaseViewState viewState, DatabaseScope scope) {
        return viewState.tabsForScope(scope).stream()
                .filter(DatabaseTab::isConcreteTab)
                .toList();
    }

    private static List<DatabaseTab> visibleConcreteTabs(DatabaseViewState viewState, List<DatabasePanelView> currentPanels) {
        LinkedHashSet<String> visibleConcreteTabIds = new LinkedHashSet<>();
        for (DatabasePanelView panel : currentPanels) {
            if (panel.tab().isConcreteTab()) {
                visibleConcreteTabIds.add(panel.tab().id());
            }
        }
        List<DatabaseTab> scopeTabs = viewState.tabsForScope(viewState.query().scope());
        if (visibleConcreteTabIds.isEmpty()) {
            for (DatabaseTab tab : scopeTabs) {
                if (tab.isConcreteTab()) {
                    visibleConcreteTabIds.add(tab.id());
                }
            }
        }
        List<DatabaseTab> candidateTabs = new ArrayList<>();
        for (DatabaseTab tab : scopeTabs) {
            if (visibleConcreteTabIds.contains(tab.id())) {
                candidateTabs.add(tab);
            }
        }
        return List.copyOf(candidateTabs);
    }

    record TargetSelection(DatabaseScope scope, String tabId) {
        TargetSelection {
            scope = DatabaseScope.normalize(scope);
            tabId = DatabaseTabs.normalizeConcreteTarget(tabId);
        }
    }

    record Row(RowType type, @Nullable DatabaseScope scope, @Nullable DatabaseTab tab, boolean sourceScopeGroup) {
        static Row header(DatabaseScope scope, boolean sourceScopeGroup) {
            return new Row(RowType.HEADER, DatabaseScope.normalize(scope), null, sourceScopeGroup);
        }

        static Row target(DatabaseScope scope, DatabaseTab tab) {
            return new Row(RowType.TARGET, DatabaseScope.normalize(scope), tab, false);
        }

        boolean isClickable() {
            return this.type == RowType.TARGET && this.scope != null && this.tab != null;
        }

        @Nullable
        TargetSelection targetSelection() {
            if (!this.isClickable()) {
                return null;
            }
            return new TargetSelection(this.scope, this.tab.id());
        }
    }

    enum RowType {
        HEADER,
        TARGET
    }
}
