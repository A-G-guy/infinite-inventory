package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.database.DatabaseQuery;
import com.agguy.infiniteinventory.database.DatabaseScopedTabRef;
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
                Math.min(PersonalDatabaseScreenCommonHelper.maxVisiblePanels(screen), screen.databaseMenu.viewState().query().visibleTabs().size())
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
        if (enforceScreenConstraints(screen)) {
            return;
        }
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
            for (DatabaseScopedTabRef visibleTab : currentQuery.visibleTabs()) {
                if (currentQuery.pageSizeFor(visibleTab) != screen.pendingLayoutQuery.pageSizeFor(visibleTab)) {
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
        Map<DatabaseScopedTabRef, Integer> nextPageIndexes = new java.util.LinkedHashMap<>();
        Map<DatabaseScopedTabRef, Integer> nextPageSizes = new java.util.LinkedHashMap<>();
        for (Map.Entry<DatabaseScopedTabRef, com.agguy.infiniteinventory.database.DatabaseTabQueryState> entry : currentQuery.tabStates().entrySet()) {
            nextPageIndexes.put(entry.getKey(), entry.getValue().pageIndex());
            nextPageSizes.put(entry.getKey(), entry.getValue().pageSize());
        }
        boolean changed = false;
        for (int panelIndex = 0; panelIndex < currentQuery.visibleTabs().size(); panelIndex++) {
            DatabaseScopedTabRef visibleTab = currentQuery.visibleTabs().get(panelIndex);
            int targetPageSize = Math.max(1, screen.layout.visibleDatabaseSlotCount(panelIndex));
            int currentPageSize = currentQuery.pageSizeFor(visibleTab);
            if (currentPageSize == targetPageSize) {
                continue;
            }
            long firstVisibleEntryIndex = (long) currentQuery.pageIndexFor(visibleTab) * Math.max(1, currentPageSize);
            int adjustedPageIndex = (int) Math.min(Integer.MAX_VALUE, firstVisibleEntryIndex / targetPageSize);
            nextPageIndexes.put(visibleTab, adjustedPageIndex);
            nextPageSizes.put(visibleTab, targetPageSize);
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
        DatabaseScopedTabRef scopedTab = PersonalDatabaseScreenCommonHelper.currentPanels(screen).get(panelIndex).scopedTab();
        DatabaseQuery currentQuery = screen.databaseMenu.viewState().query();
        if (currentQuery.searchTextFor(scopedTab).equals(value)) {
            return;
        }
        sendQuery(screen, currentQuery.withSearchText(scopedTab, value).withFocusedTab(scopedTab));
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
                screen.databaseMenu.viewState().query().withPageIndex(panel.scopedTab(), nextPage).withFocusedTab(panel.scopedTab())
        );
    }

    static void sendQuery(PersonalDatabaseScreen screen, DatabaseQuery query) {
        sendQuery(screen, query, false);
    }

    static void sendQuery(PersonalDatabaseScreen screen, DatabaseQuery query, boolean keepSortDropdownExpanded) {
        if (query.equals(screen.databaseMenu.viewState().query())) {
            return;
        }
        screen.pendingLayoutQuery = null;
        PersonalDatabaseScreenSelectionHelper.clearSelection(screen);
        prepareForServerQuery(screen, keepSortDropdownExpanded);
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
            DatabaseScope targetScope,
            String targetTabId
    ) {
        PacketDistributor.sendToServer(new DatabaseClickPayload(
                screen.databaseMenu.containerId,
                screen.databaseMenu.viewState().sessionId(),
                panelIndex,
                slotIndex,
                action,
                targetScope,
                targetTabId == null ? "" : targetTabId
        ));
    }

    static void sendDatabaseSelection(
            PersonalDatabaseScreen screen,
            DatabaseSelectionAction action,
            String targetTabId,
            List<DatabaseSelectionEntry> selectedEntries
    ) {
        sendDatabaseSelection(screen, action, null, targetTabId, 0L, selectedEntries);
    }

    static void sendDatabaseSelection(
            PersonalDatabaseScreen screen,
            DatabaseSelectionAction action,
            String targetTabId,
            long requestedAmount,
            List<DatabaseSelectionEntry> selectedEntries
    ) {
        sendDatabaseSelection(screen, action, null, targetTabId, requestedAmount, selectedEntries);
    }

    static void sendDatabaseSelection(
            PersonalDatabaseScreen screen,
            DatabaseSelectionAction action,
            DatabaseScope targetScope,
            String targetTabId,
            long requestedAmount,
            List<DatabaseSelectionEntry> selectedEntries
    ) {
        PacketDistributor.sendToServer(new DatabaseSelectionPayload(
                screen.databaseMenu.containerId,
                screen.databaseMenu.viewState().sessionId(),
                action,
                targetScope,
                targetTabId == null ? "" : targetTabId,
                requestedAmount,
                selectedEntries
        ));
    }

    static void prepareForServerQuery(PersonalDatabaseScreen screen) {
        prepareForServerQuery(screen, false);
    }

    static void prepareForServerQuery(PersonalDatabaseScreen screen, boolean keepSortDropdownExpanded) {
        if (keepSortDropdownExpanded) {
            PersonalDatabaseScreenContextHelper.closeContextMenu(screen);
            PersonalDatabaseScreenCustomExtractOverlayHelper.closeOverlay(screen);
            screen.pagePickerExpanded = false;
            screen.activePagePickerPanelIndex = -1;
            screen.enhancementPanelExpanded = false;
            screen.viewSelectorExpanded = false;
            screen.moreTabsExpanded = false;
            PersonalDatabaseScreenTabHelper.closeTopTabPrompt(screen);
            PersonalDatabaseScreenTargetHelper.closeTargetSelector(screen);
            PersonalDatabaseScreenManagementHelper.closeTabManagementOverlays(screen);
            return;
        }
        PersonalDatabaseScreenTargetHelper.closeTransientOverlays(screen);
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

    private static boolean enforceScreenConstraints(PersonalDatabaseScreen screen) {
        DatabaseQuery currentQuery = screen.databaseMenu.viewState().query();
        java.util.List<DatabaseScopedTabRef> clampedVisibleTabs = PersonalDatabaseScreenCommonHelper.clampVisibleTabsToScreen(
                screen,
                currentQuery.visibleTabs(),
                currentQuery.focusedTab()
        );
        if (clampedVisibleTabs.equals(currentQuery.visibleTabs())) {
            return false;
        }
        DatabaseScopedTabRef nextFocusedTab = clampedVisibleTabs.contains(currentQuery.focusedTab())
                ? currentQuery.focusedTab()
                : clampedVisibleTabs.getFirst();
        sendQuery(
                screen,
                currentQuery.withVisibleTabs(clampedVisibleTabs).withFocusedTab(nextFocusedTab)
        );
        return true;
    }

    private static String uiSignature(PersonalDatabaseScreen screen) {
        DatabaseQuery query = screen.databaseMenu.viewState().query();
        List<String> signatureParts = new ArrayList<>();
        signatureParts.add(Integer.toString(screen.screenWidthValue()));
        signatureParts.add(Integer.toString(screen.screenHeightValue()));
        signatureParts.add(Boolean.toString(screen.accessoriesExpanded));
        signatureParts.add(query.focusedTab().scope().name() + ":" + query.focusedTab().tabId());
        for (DatabaseScopedTabRef visibleTab : query.visibleTabs()) {
            signatureParts.add(visibleTab.scope().name() + ":" + visibleTab.tabId());
        }
        for (var panel : screen.databaseMenu.viewState().panels()) {
            signatureParts.add(panel.scopedTab().scope().name() + ":" + panel.scopedTab().tabId());
        }
        return String.join("|", signatureParts);
    }
}
