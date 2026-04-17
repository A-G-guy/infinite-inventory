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
            @Nullable DatabaseScope sourceScope,
            @Nullable String sourceTabId,
            List<DatabasePanelView> currentPanels
    ) {
        if (mode == null || viewState == null) {
            return List.of();
        }
        return switch (mode) {
            case TRANSFER_SELECTION, TRANSFER_TAB -> buildTransferRows(mode, viewState, sourceScope, sourceTabId);
            case AUTO_STORE_TARGET -> buildAutoStoreTargetRows(viewState);
            case DEPOSIT_ALL, CARRIED_STORE, QUICK_DEPOSIT -> buildDepositTargetRows(viewState, currentPanels);
            case DELETE_TAB -> buildFlatRows(
                    DatabaseScope.normalize(sourceScope),
                    candidateTabs(mode, viewState, DatabaseScope.normalize(sourceScope), sourceTabId, currentPanels)
            );
            case NONE -> List.of();
        };
    }

    private static List<Row> buildTransferRows(
            PersonalDatabaseScreen.TargetSelectorMode mode,
            DatabaseViewState viewState,
            @Nullable DatabaseScope sourceScope,
            @Nullable String sourceTabId
    ) {
        DatabaseScope normalizedSourceScope = DatabaseScope.normalize(sourceScope);
        DatabaseScope otherScope = normalizedSourceScope == DatabaseScope.PUBLIC ? DatabaseScope.PERSONAL : DatabaseScope.PUBLIC;
        List<Row> rows = new ArrayList<>();
        appendTransferGroup(
                rows,
                normalizedSourceScope,
                true,
                transferTabsForScope(mode, viewState, normalizedSourceScope, normalizedSourceScope, sourceTabId)
        );
        appendTransferGroup(
                rows,
                otherScope,
                false,
                transferTabsForScope(mode, viewState, otherScope, normalizedSourceScope, sourceTabId)
        );
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

    private static List<Row> buildDepositTargetRows(DatabaseViewState viewState, List<DatabasePanelView> currentPanels) {
        List<DatabaseTab> personalTabs = candidateTabs(
                PersonalDatabaseScreen.TargetSelectorMode.DEPOSIT_ALL,
                viewState,
                DatabaseScope.PERSONAL,
                "",
                currentPanels
        );
        List<DatabaseTab> publicTabs = candidateTabs(
                PersonalDatabaseScreen.TargetSelectorMode.DEPOSIT_ALL,
                viewState,
                DatabaseScope.PUBLIC,
                "",
                currentPanels
        );
        boolean hasPersonal = !personalTabs.isEmpty();
        boolean hasPublic = !publicTabs.isEmpty();
        if (!hasPersonal && !hasPublic) {
            return List.of();
        }
        if (!hasPersonal) {
            return buildFlatRows(DatabaseScope.PUBLIC, publicTabs);
        }
        if (!hasPublic) {
            return buildFlatRows(DatabaseScope.PERSONAL, personalTabs);
        }
        List<Row> rows = new ArrayList<>();
        appendTransferGroup(rows, DatabaseScope.PERSONAL, false, personalTabs);
        appendTransferGroup(rows, DatabaseScope.PUBLIC, false, publicTabs);
        return List.copyOf(rows);
    }

    private static List<DatabaseTab> transferTabsForScope(
            PersonalDatabaseScreen.TargetSelectorMode mode,
            DatabaseViewState viewState,
            DatabaseScope scope,
            DatabaseScope sourceScope,
            @Nullable String sourceTabId
    ) {
        String normalizedSourceTabId = sourceTabId == null || sourceTabId.isBlank()
                ? null
                : DatabaseTabs.normalizeConcreteTarget(sourceTabId);
        return viewState.tabsForScope(scope).stream()
                .filter(DatabaseTab::isConcreteTab)
                .filter(tab -> mode != PersonalDatabaseScreen.TargetSelectorMode.TRANSFER_TAB
                        || scope != DatabaseScope.normalize(sourceScope)
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
            DatabaseScope targetScope,
            @Nullable String sourceTabId,
            List<DatabasePanelView> currentPanels
    ) {
        return switch (mode) {
            case DEPOSIT_ALL, CARRIED_STORE, QUICK_DEPOSIT -> visibleConcreteTabs(viewState, targetScope, currentPanels);
            case DELETE_TAB -> viewState.tabsForScope(targetScope).stream()
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

    private static List<DatabaseTab> visibleConcreteTabs(
            DatabaseViewState viewState,
            DatabaseScope scope,
            List<DatabasePanelView> currentPanels
    ) {
        LinkedHashSet<String> visibleConcreteTabIds = new LinkedHashSet<>();
        for (DatabasePanelView panel : currentPanels) {
            if (panel.scopedTab().scope() == DatabaseScope.normalize(scope) && panel.tab().isConcreteTab()) {
                visibleConcreteTabIds.add(panel.tab().id());
            }
        }
        List<DatabaseTab> scopeTabs = viewState.tabsForScope(scope);
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
