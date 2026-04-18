package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.DatabaseScopedTabRef;
import com.agguy.infiniteinventory.database.DatabaseTab;
import com.agguy.infiniteinventory.database.DatabaseTabs;
import com.agguy.infiniteinventory.menu.PersonalDatabaseLayout;
import com.agguy.infiniteinventory.network.DatabaseTabMutationAction;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

final class PersonalDatabaseScreenManagementListHelper {
    private PersonalDatabaseScreenManagementListHelper() {
    }

    static boolean scrollManagementList(PersonalDatabaseScreen screen, double mouseX, double mouseY, int deltaRows) {
        if (!screen.tabManagementExpanded || screen.iconPickerExpanded || screen.targetSelectorExpanded || deltaRows == 0) {
            return false;
        }
        if (adjustManagementScroll(screen, mouseX, mouseY, DatabaseScope.PERSONAL, deltaRows)) {
            return true;
        }
        return adjustManagementScroll(screen, mouseX, mouseY, DatabaseScope.PUBLIC, deltaRows);
    }

    static void renderManagementColumn(
            PersonalDatabaseScreen screen,
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            DatabaseScope scope,
            DatabaseScopedTabRef selectedScopedTab
    ) {
        boolean personalColumn = scope == DatabaseScope.PERSONAL;
        PersonalDatabaseLayout.Rect headerRect = PersonalDatabaseScreenManagementGeometry.managementScopeHeaderRect(screen, personalColumn);
        if (headerRect.width() <= 0 || headerRect.height() <= 0) {
            return;
        }
        PersonalDatabaseLayout.Rect addRect = PersonalDatabaseScreenManagementGeometry.managementScopeAddButtonRect(screen, personalColumn);
        guiGraphics.drawString(
                screen.screenFont(),
                PersonalDatabaseScreenCommonHelper.scopeLabel(scope),
                headerRect.x(),
                headerRect.y() + 6,
                PersonalDatabaseScreen.OVERLAY_MUTED_TEXT_COLOR,
                false
        );
        PersonalDatabaseScreenManagementLogic.renderManagementActionButton(
                screen,
                guiGraphics,
                addRect,
                mouseX,
                mouseY,
                Component.translatable("screen.infiniteinventory.management.add"),
                true
        );

        List<DatabaseTab> tabs = PersonalDatabaseScreenCommonHelper.tabsForScope(screen, scope);
        ManagementListViewport viewport = listViewport(screen, scope);
        PersonalDatabaseScreenListHelper.enableScissor(guiGraphics, viewport.bodyRect());
        for (int index = viewport.visibleRange().fromIndex(); index < viewport.visibleRange().toIndex(); index++) {
            DatabaseTab tab = tabs.get(index);
            int rowIndex = index - viewport.visibleRange().fromIndex();
            PersonalDatabaseLayout.Rect rowRect = new PersonalDatabaseLayout.Rect(
                    viewport.bodyRect().x(),
                    viewport.bodyRect().y() + rowIndex * PersonalDatabaseScreen.MANAGEMENT_ROW_HEIGHT,
                    viewport.bodyRect().width(),
                    PersonalDatabaseScreen.MANAGEMENT_ROW_HEIGHT - 1
            );
            boolean hovered = rowRect.contains(mouseX, mouseY);
            boolean selected = selectedScopedTab.scope() == scope && selectedScopedTab.tabId().equals(tab.id());
            VanillaWidgetRenderer.renderOverlayRow(guiGraphics, rowRect, hovered, selected);
            guiGraphics.renderItem(PersonalDatabaseScreenCommonHelper.tabIcon(screen, tab), rowRect.x() + 3, rowRect.y() + 2);
            guiGraphics.drawString(
                    screen.screenFont(),
                    PersonalDatabaseScreenGeometry.truncateToWidth(
                            screen,
                            PersonalDatabaseScreenCommonHelper.tabLabel(screen, tab).getString(),
                            Math.max(0, rowRect.width() - 28)
                    ),
                    rowRect.x() + 24,
                    rowRect.y() + 6,
                    selected ? PersonalDatabaseScreen.OVERLAY_ACCENT_TEXT_COLOR : PersonalDatabaseScreen.OVERLAY_TEXT_COLOR,
                    false
            );
        }
        guiGraphics.disableScissor();
        PersonalDatabaseScreenListHelper.renderScrollIndicators(screen, guiGraphics, viewport.bodyRect(), viewport.visibleRange());
    }

    static boolean handleManagementColumnClick(PersonalDatabaseScreen screen, double mouseX, double mouseY, DatabaseScope scope) {
        if (PersonalDatabaseScreenManagementCompactHelper.handleCompactScopeClick(screen, mouseX, mouseY, scope)) {
            return true;
        }
        boolean personalColumn = scope == DatabaseScope.PERSONAL;
        if (PersonalDatabaseScreenManagementGeometry.managementScopeAddButtonRect(screen, personalColumn).contains(mouseX, mouseY)) {
            PersonalDatabaseScreenManagementHelper.sendTabMutation(
                    screen,
                    scope,
                    DatabaseTabMutationAction.ADD,
                    "",
                    "",
                    "",
                    DatabaseTabs.DEFAULT_CONCRETE_ICON_ITEM_ID
            );
            return true;
        }
        List<DatabaseTab> tabs = PersonalDatabaseScreenCommonHelper.tabsForScope(screen, scope);
        ManagementListViewport viewport = listViewport(screen, scope);
        for (int index = viewport.visibleRange().fromIndex(); index < viewport.visibleRange().toIndex(); index++) {
            int rowIndex = index - viewport.visibleRange().fromIndex();
            PersonalDatabaseLayout.Rect rowRect = new PersonalDatabaseLayout.Rect(
                    viewport.bodyRect().x(),
                    viewport.bodyRect().y() + rowIndex * PersonalDatabaseScreen.MANAGEMENT_ROW_HEIGHT,
                    viewport.bodyRect().width(),
                    PersonalDatabaseScreen.MANAGEMENT_ROW_HEIGHT - 1
            );
            if (!rowRect.contains(mouseX, mouseY)) {
                continue;
            }
            PersonalDatabaseScreenManagementHelper.loadManagementDrafts(
                    screen,
                    DatabaseScopedTabRef.concreteTab(scope, tabs.get(index).id())
            );
            return true;
        }
        return false;
    }

    private static boolean adjustManagementScroll(
            PersonalDatabaseScreen screen,
            double mouseX,
            double mouseY,
            DatabaseScope scope,
            int deltaRows
    ) {
        ManagementListViewport viewport = listViewport(screen, scope);
        if (!viewport.bodyRect().contains(mouseX, mouseY)) {
            return false;
        }
        int currentScrollIndex = scope == DatabaseScope.PERSONAL
                ? screen.managementPersonalScrollIndex
                : screen.managementPublicScrollIndex;
        int nextScrollIndex = PersonalDatabaseScreenListHelper.clampScrollIndex(viewport.visibleRange(), deltaRows);
        if (currentScrollIndex == nextScrollIndex) {
            return false;
        }
        if (scope == DatabaseScope.PERSONAL) {
            screen.managementPersonalScrollIndex = nextScrollIndex;
        } else {
            screen.managementPublicScrollIndex = nextScrollIndex;
        }
        return true;
    }

    private static ManagementListViewport listViewport(PersonalDatabaseScreen screen, DatabaseScope scope) {
        boolean personalColumn = scope == DatabaseScope.PERSONAL;
        PersonalDatabaseLayout.Rect columnRect = PersonalDatabaseScreenManagementGeometry.managementScopeColumnRect(screen, personalColumn);
        PersonalDatabaseLayout.Rect headerRect = PersonalDatabaseScreenManagementGeometry.managementScopeHeaderRect(screen, personalColumn);
        PersonalDatabaseLayout.Rect bodyRect = new PersonalDatabaseLayout.Rect(
                columnRect.x(),
                headerRect.bottom() + 6,
                columnRect.width(),
                Math.max(0, columnRect.bottom() - (headerRect.bottom() + 6))
        );
        int requestedScrollIndex = scope == DatabaseScope.PERSONAL
                ? screen.managementPersonalScrollIndex
                : screen.managementPublicScrollIndex;
        PersonalDatabaseScreenListHelper.VisibleRange visibleRange = PersonalDatabaseScreenListHelper.visibleRange(
                PersonalDatabaseScreenCommonHelper.tabsForScope(screen, scope).size(),
                requestedScrollIndex,
                PersonalDatabaseScreenListHelper.maxVisibleRows(bodyRect, PersonalDatabaseScreen.MANAGEMENT_ROW_HEIGHT)
        );
        if (scope == DatabaseScope.PERSONAL) {
            screen.managementPersonalScrollIndex = visibleRange.scrollIndex();
        } else {
            screen.managementPublicScrollIndex = visibleRange.scrollIndex();
        }
        return new ManagementListViewport(bodyRect, visibleRange);
    }

    private record ManagementListViewport(
            PersonalDatabaseLayout.Rect bodyRect,
            PersonalDatabaseScreenListHelper.VisibleRange visibleRange
    ) {
    }
}
