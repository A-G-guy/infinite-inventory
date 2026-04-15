package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.database.DatabaseQuery;
import com.agguy.infiniteinventory.database.DatabaseSelectionEntry;
import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.DatabaseTabs;
import com.agguy.infiniteinventory.database.DatabaseTab;
import com.agguy.infiniteinventory.menu.PersonalDatabaseLayout;
import com.agguy.infiniteinventory.network.DatabaseClickAction;
import com.agguy.infiniteinventory.network.DatabaseClickPayload;
import com.agguy.infiniteinventory.network.DatabaseQueryPayload;
import com.agguy.infiniteinventory.network.DatabaseSelectionAction;
import com.agguy.infiniteinventory.network.DatabaseSelectionPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;

final class PersonalDatabaseScreenLayoutHelper {
    private PersonalDatabaseScreenLayoutHelper() {
    }

    static boolean hasAccessorySlots(PersonalDatabaseScreen screen) {
        return !screen.databaseMenu.accessorySlotGroups().isEmpty();
    }

    static void rebuildLayout(PersonalDatabaseScreen screen) {
        int visiblePanelCount = Math.max(
                1,
                Math.min(DatabaseTabs.MAX_VISIBLE_TAB_COUNT, screen.databaseMenu.viewState().query().visibleTabIds().size())
        );
        screen.layout = PersonalDatabaseLayout.create(
                screen.screenWidthValue(),
                screen.screenHeightValue(),
                screen.inventoryPaneProvider.equipmentPanelWidth(),
                screen.inventoryPaneProvider.equipmentPanelHeight(),
                screen.inventoryPaneProvider.bottomInventoryWidth(),
                screen.inventoryPaneProvider.bottomInventoryHeight(),
                screen.databaseMenu.accessorySlotGroups(),
                visiblePanelCount,
                screen.accessoriesExpanded,
                screen.accessoryScrollRow
        );
        screen.accessoryScrollRow = screen.layout.accessoryScrollRow();
        screen.databaseMenu.applySlotLayout(screen.layout);
    }

    static void refreshUiStructureIfNeeded(PersonalDatabaseScreen screen) {
        String nextSignature = uiSignature(screen);
        if (nextSignature.equals(screen.lastUiSignature)) {
            return;
        }
        screen.lastUiSignature = nextSignature;
        rebuildLayout(screen);
        rebuildWidgets(screen);
    }

    static void ensureLayoutQuerySynced(PersonalDatabaseScreen screen) {
        if (screen.layout == null) {
            return;
        }
        DatabaseQuery currentQuery = screen.databaseMenu.viewState().query();
        if (screen.pendingLayoutQuery != null) {
            boolean allSynced = true;
            for (String visibleTabId : currentQuery.visibleTabIds()) {
                if (currentQuery.pageSizeFor(visibleTabId) != screen.pendingLayoutQuery.pageSizeFor(visibleTabId)) {
                    allSynced = false;
                    break;
                }
            }
            if (allSynced) {
                screen.pendingLayoutQuery = null;
            } else {
                return;
            }
        }
        Map<String, Integer> nextPageIndexes = new java.util.LinkedHashMap<>();
        Map<String, Integer> nextPageSizes = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, com.agguy.infiniteinventory.database.DatabaseTabQueryState> entry : currentQuery.tabStates().entrySet()) {
            nextPageIndexes.put(entry.getKey(), entry.getValue().pageIndex());
            nextPageSizes.put(entry.getKey(), entry.getValue().pageSize());
        }
        boolean changed = false;
        for (int panelIndex = 0; panelIndex < currentQuery.visibleTabIds().size(); panelIndex++) {
            String visibleTabId = currentQuery.visibleTabIds().get(panelIndex);
            int targetPageSize = Math.max(1, screen.layout.visibleDatabaseSlotCount(panelIndex));
            int currentPageSize = currentQuery.pageSizeFor(visibleTabId);
            if (currentPageSize == targetPageSize) {
                continue;
            }
            long firstVisibleEntryIndex = (long) currentQuery.pageIndexFor(visibleTabId) * Math.max(1, currentPageSize);
            int adjustedPageIndex = (int) Math.min(Integer.MAX_VALUE, firstVisibleEntryIndex / targetPageSize);
            nextPageIndexes.put(visibleTabId, adjustedPageIndex);
            nextPageSizes.put(visibleTabId, targetPageSize);
            changed = true;
        }
        if (!changed) {
            return;
        }
        DatabaseQuery adjustedQuery = currentQuery.withPanelLayout(nextPageIndexes, nextPageSizes);
        screen.pendingLayoutQuery = adjustedQuery;
        prepareForServerQuery(screen);
        dispatchQuery(screen, adjustedQuery);
    }

    static void onPanelSearchChanged(PersonalDatabaseScreen screen, int panelIndex, String value) {
        if (screen.syncingSearchBox) {
            return;
        }
        if (panelIndex < 0 || panelIndex >= PersonalDatabaseScreenCommonHelper.currentPanels(screen).size()) {
            return;
        }
        String tabId = PersonalDatabaseScreenCommonHelper.currentPanels(screen).get(panelIndex).tab().id();
        DatabaseQuery currentQuery = screen.databaseMenu.viewState().query();
        if (currentQuery.searchTextFor(tabId).equals(value)) {
            return;
        }
        sendQuery(screen, currentQuery.withSearchText(tabId, value).withFocusedTabId(tabId));
    }

    static void changePanelPage(PersonalDatabaseScreen screen, int panelIndex, int delta) {
        if (panelIndex < 0 || panelIndex >= PersonalDatabaseScreenCommonHelper.currentPanels(screen).size()) {
            return;
        }
        var panel = PersonalDatabaseScreenCommonHelper.currentPanels(screen).get(panelIndex);
        int nextPage = Math.max(0, Math.min(panel.totalPages() - 1, panel.pageIndex() + delta));
        if (nextPage == panel.pageIndex()) {
            return;
        }
        sendQuery(
                screen,
                screen.databaseMenu.viewState().query().withPageIndex(panel.tab().id(), nextPage).withFocusedTabId(panel.tab().id())
        );
    }

    static void sendQuery(PersonalDatabaseScreen screen, DatabaseQuery query) {
        if (query.equals(screen.databaseMenu.viewState().query())) {
            return;
        }
        screen.pendingLayoutQuery = null;
        PersonalDatabaseScreenSelectionHelper.clearSelection(screen);
        prepareForServerQuery(screen);
        dispatchQuery(screen, query);
    }

    static void dispatchQuery(PersonalDatabaseScreen screen, DatabaseQuery query) {
        PacketDistributor.sendToServer(new DatabaseQueryPayload(
                screen.databaseMenu.containerId,
                screen.databaseMenu.viewState().sessionId(),
                query
        ));
    }

    static void sendDatabaseClick(
            PersonalDatabaseScreen screen,
            int panelIndex,
            int slotIndex,
            DatabaseClickAction action,
            String targetTabId
    ) {
        PacketDistributor.sendToServer(new DatabaseClickPayload(
                screen.databaseMenu.containerId,
                screen.databaseMenu.viewState().sessionId(),
                panelIndex,
                slotIndex,
                action,
                targetTabId == null ? "" : targetTabId
        ));
    }

    static void sendDatabaseSelection(
            PersonalDatabaseScreen screen,
            DatabaseSelectionAction action,
            String targetTabId,
            List<DatabaseSelectionEntry> selectedEntries
    ) {
        PacketDistributor.sendToServer(new DatabaseSelectionPayload(
                screen.databaseMenu.containerId,
                screen.databaseMenu.viewState().sessionId(),
                action,
                targetTabId == null ? "" : targetTabId,
                selectedEntries
        ));
    }

    static void prepareForServerQuery(PersonalDatabaseScreen screen) {
        PersonalDatabaseScreenTargetHelper.closeTransientOverlays(screen);
    }

    static void switchScope(PersonalDatabaseScreen screen, DatabaseScope scope) {
        DatabaseScope normalizedScope = DatabaseScope.normalize(scope);
        if (normalizedScope == screen.databaseMenu.viewState().query().scope()) {
            return;
        }
        sendQuery(screen, screen.databaseMenu.viewState().queryForScope(normalizedScope));
    }

    static void toggleAccessoriesPanel(PersonalDatabaseScreen screen) {
        if (!hasAccessorySlots(screen)) {
            return;
        }
        screen.accessoriesExpanded = !screen.accessoriesExpanded;
        if (!screen.accessoriesExpanded) {
            screen.accessoryScrollRow = 0;
        }
        PersonalDatabaseScreenTargetHelper.closeTransientOverlays(screen);
        rebuildLayout(screen);
    }

    static boolean scrollAccessories(PersonalDatabaseScreen screen, int deltaRows) {
        if (deltaRows == 0 || screen.layout == null || !screen.accessoriesExpanded) {
            return false;
        }
        int nextScrollRow = Mth.clamp(screen.accessoryScrollRow + deltaRows, 0, screen.layout.accessoryMaxScrollRow());
        if (nextScrollRow == screen.accessoryScrollRow) {
            return false;
        }
        screen.accessoryScrollRow = nextScrollRow;
        rebuildLayout(screen);
        return true;
    }

    private static void rebuildWidgets(PersonalDatabaseScreen screen) {
        screen.clearScreenWidgets();
        screen.panelSearchBoxes.clear();
        screen.panelSortButtons.clear();
        screen.panelPreviousPageButtons.clear();
        screen.panelPageButtons.clear();
        screen.panelNextPageButtons.clear();
        screen.advancedSearchToggleButtons.clear();
        screen.advancedSearchWeightButtons.clear();
        screen.enhancementToggleButtons.clear();
        screen.managementNameBox = null;
        screen.iconSearchBox = null;
        PersonalDatabaseScreenWidgetHelper.buildWidgets(screen);
        PersonalDatabaseScreenWidgetHelper.syncWidgetsFromState(screen);
    }

    private static String uiSignature(PersonalDatabaseScreen screen) {
        DatabaseQuery query = screen.databaseMenu.viewState().query();
        List<String> signatureParts = new ArrayList<>();
        signatureParts.add(Integer.toString(screen.screenWidthValue()));
        signatureParts.add(Integer.toString(screen.screenHeightValue()));
        signatureParts.add(Boolean.toString(screen.accessoriesExpanded));
        signatureParts.add(query.scope().name());
        signatureParts.add(query.focusedTabId());
        signatureParts.addAll(query.visibleTabIds());
        for (DatabaseTab tab : screen.databaseMenu.viewState().panels().stream().map(com.agguy.infiniteinventory.database.DatabasePanelView::tab).toList()) {
            signatureParts.add(tab.id());
        }
        return String.join("|", signatureParts);
    }
}
