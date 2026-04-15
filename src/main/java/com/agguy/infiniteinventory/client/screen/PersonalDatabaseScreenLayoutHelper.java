package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.database.DatabaseQuery;
import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.DatabaseTabs;
import com.agguy.infiniteinventory.menu.PersonalDatabaseLayout;
import com.agguy.infiniteinventory.network.DatabaseClickAction;
import com.agguy.infiniteinventory.network.DatabaseClickPayload;
import com.agguy.infiniteinventory.network.DatabaseQueryPayload;
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
        Map<String, Integer> nextPageIndexes = new java.util.LinkedHashMap<>(currentQuery.pageIndexes());
        Map<String, Integer> nextPageSizes = new java.util.LinkedHashMap<>(currentQuery.pageSizes());
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

    static void onSearchChanged(PersonalDatabaseScreen screen, String value) {
        if (screen.syncingSearchBox) {
            return;
        }
        DatabaseQuery currentQuery = screen.databaseMenu.viewState().query();
        if (currentQuery.searchText().equals(value)) {
            return;
        }
        sendQuery(screen, currentQuery.withSearchText(value));
    }

    static void changePage(PersonalDatabaseScreen screen, int delta) {
        var viewState = screen.databaseMenu.viewState();
        int nextPage = Math.max(0, Math.min(viewState.totalPages() - 1, viewState.query().pageIndex() + delta));
        if (nextPage == viewState.query().pageIndex()) {
            return;
        }
        sendQuery(screen, viewState.query().withPageIndex(nextPage));
    }

    static void sendQuery(PersonalDatabaseScreen screen, DatabaseQuery query) {
        if (query.equals(screen.databaseMenu.viewState().query())) {
            return;
        }
        screen.pendingLayoutQuery = null;
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
}
