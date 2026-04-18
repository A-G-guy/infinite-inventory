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
import org.jetbrains.annotations.Nullable;

final class PersonalDatabaseScreenLayoutHelper {
    private static final int SEARCH_SYNC_DELAY_TICKS = 6;

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
        prepareForServerQuery(screen, false, screen.viewSelectorExpanded);
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
        String normalizedValue = value == null ? "" : value;
        screen.activeSearchTab = scopedTab;
        screen.pendingSearchTexts.put(scopedTab, normalizedValue);
        screen.dispatchedSearchTexts.remove(scopedTab);
        screen.searchSyncCooldownTicks = SEARCH_SYNC_DELAY_TICKS;
    }

    static void tickSearchSync(PersonalDatabaseScreen screen) {
        reconcilePendingSearchState(screen);
        if (screen.pendingSearchTexts.isEmpty()) {
            screen.searchSyncCooldownTicks = 0;
            return;
        }
        if (screen.searchSyncCooldownTicks > 0) {
            screen.searchSyncCooldownTicks--;
            return;
        }
        DatabaseQuery currentQuery = screen.databaseMenu.viewState().query();
        DatabaseQuery nextQuery = currentQuery;
        boolean changed = false;
        for (Map.Entry<DatabaseScopedTabRef, String> entry : screen.pendingSearchTexts.entrySet()) {
            DatabaseScopedTabRef scopedTab = entry.getKey();
            String pendingText = entry.getValue();
            if (currentQuery.searchTextFor(scopedTab).equals(pendingText)
                    || pendingText.equals(screen.dispatchedSearchTexts.get(scopedTab))) {
                continue;
            }
            nextQuery = nextQuery.withSearchText(scopedTab, pendingText);
            screen.dispatchedSearchTexts.put(scopedTab, pendingText);
            changed = true;
        }
        if (!changed) {
            return;
        }
        DatabaseScopedTabRef searchFocusTab = resolveActiveSearchTab(screen);
        if (searchFocusTab != null) {
            nextQuery = nextQuery.withFocusedTab(searchFocusTab);
        }
        sendQuery(screen, nextQuery);
    }

    static void updateSearchFocusFromClick(PersonalDatabaseScreen screen, double mouseX, double mouseY) {
        if (screen.layout == null) {
            screen.activeSearchTab = null;
            return;
        }
        for (int panelIndex = 0; panelIndex < PersonalDatabaseScreenCommonHelper.currentPanels(screen).size(); panelIndex++) {
            if (!PersonalDatabaseScreenGeometry.panelSearchFieldRect(screen, panelIndex).contains(mouseX, mouseY)) {
                continue;
            }
            screen.activeSearchTab = PersonalDatabaseScreenCommonHelper.currentPanels(screen).get(panelIndex).scopedTab();
            return;
        }
        screen.activeSearchTab = null;
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
        sendQuery(screen, query, false, screen.viewSelectorExpanded);
    }

    static void sendQuery(PersonalDatabaseScreen screen, DatabaseQuery query, boolean keepSortDropdownExpanded) {
        sendQuery(screen, query, keepSortDropdownExpanded, screen.viewSelectorExpanded);
    }

    static void sendQueryKeepingViewSelector(PersonalDatabaseScreen screen, DatabaseQuery query) {
        sendQuery(screen, query, false, true);
    }

    private static void sendQuery(
            PersonalDatabaseScreen screen,
            DatabaseQuery query,
            boolean keepSortDropdownExpanded,
            boolean keepViewSelectorExpanded
    ) {
        if (query.equals(screen.databaseMenu.viewState().query())) {
            return;
        }
        screen.pendingLayoutQuery = null;
        PersonalDatabaseScreenSelectionHelper.clearSelection(screen);
        prepareForServerQuery(screen, keepSortDropdownExpanded, keepViewSelectorExpanded);
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
        prepareForServerQuery(screen, false, false);
    }

    static void prepareForServerQuery(PersonalDatabaseScreen screen, boolean keepSortDropdownExpanded) {
        prepareForServerQuery(screen, keepSortDropdownExpanded, false);
    }

    private static void prepareForServerQuery(
            PersonalDatabaseScreen screen,
            boolean keepSortDropdownExpanded,
            boolean keepViewSelectorExpanded
    ) {
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
        if (keepViewSelectorExpanded) {
            PersonalDatabaseScreenContextHelper.closeContextMenu(screen);
            PersonalDatabaseScreenCustomExtractOverlayHelper.closeOverlay(screen);
            screen.sortDropdownExpanded = false;
            screen.activeSortPanelIndex = -1;
            screen.pagePickerExpanded = false;
            screen.activePagePickerPanelIndex = -1;
            screen.advancedSearchExpanded = false;
            screen.enhancementPanelExpanded = false;
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
        DatabaseScopedTabRef focusedSearchTab = screen.activeSearchTab != null
                ? screen.activeSearchTab
                : focusedSearchTab(screen);
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
        restoreFocusedSearchBox(screen, focusedSearchTab);
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
        for (DatabaseScopedTabRef visibleTab : query.visibleTabs()) {
            signatureParts.add(visibleTab.scope().name() + ":" + visibleTab.tabId());
        }
        for (var panel : screen.databaseMenu.viewState().panels()) {
            signatureParts.add(panel.scopedTab().scope().name() + ":" + panel.scopedTab().tabId());
        }
        return String.join("|", signatureParts);
    }

    private static DatabaseScopedTabRef focusedSearchTab(PersonalDatabaseScreen screen) {
        int panelCount = Math.min(screen.panelSearchBoxes.size(), PersonalDatabaseScreenCommonHelper.currentPanels(screen).size());
        for (int panelIndex = 0; panelIndex < panelCount; panelIndex++) {
            if (screen.panelSearchBoxes.get(panelIndex).isFocused()) {
                return PersonalDatabaseScreenCommonHelper.currentPanels(screen).get(panelIndex).scopedTab();
            }
        }
        return null;
    }

    private static void restoreFocusedSearchBox(PersonalDatabaseScreen screen, DatabaseScopedTabRef focusedSearchTab) {
        if (focusedSearchTab == null) {
            return;
        }
        int panelCount = Math.min(screen.panelSearchBoxes.size(), PersonalDatabaseScreenCommonHelper.currentPanels(screen).size());
        for (int panelIndex = 0; panelIndex < panelCount; panelIndex++) {
            if (!PersonalDatabaseScreenCommonHelper.currentPanels(screen).get(panelIndex).scopedTab().equals(focusedSearchTab)) {
                continue;
            }
            var searchBox = screen.panelSearchBoxes.get(panelIndex);
            screen.focusScreen(searchBox);
            searchBox.setFocused(true);
            return;
        }
    }

    private static void reconcilePendingSearchState(PersonalDatabaseScreen screen) {
        DatabaseQuery currentQuery = screen.databaseMenu.viewState().query();
        java.util.Iterator<Map.Entry<DatabaseScopedTabRef, String>> iterator = screen.pendingSearchTexts.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<DatabaseScopedTabRef, String> entry = iterator.next();
            if (!currentQuery.searchTextFor(entry.getKey()).equals(entry.getValue())) {
                continue;
            }
            iterator.remove();
            screen.dispatchedSearchTexts.remove(entry.getKey());
        }
    }

    @Nullable
    private static DatabaseScopedTabRef resolveActiveSearchTab(PersonalDatabaseScreen screen) {
        if (screen.activeSearchTab != null) {
            return screen.activeSearchTab;
        }
        return focusedSearchTab(screen);
    }
}
