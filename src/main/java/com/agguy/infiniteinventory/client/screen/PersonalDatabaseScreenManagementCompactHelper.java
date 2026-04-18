package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.DatabaseScopedTabRef;
import com.agguy.infiniteinventory.menu.PersonalDatabaseLayout;
import net.minecraft.client.gui.GuiGraphics;

final class PersonalDatabaseScreenManagementCompactHelper {
    private PersonalDatabaseScreenManagementCompactHelper() {
    }

    static void renderCompactScopeSwitchers(PersonalDatabaseScreen screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (!PersonalDatabaseScreenCommonHelper.fitProfile(screen).usesCompactManagementLayout()) {
            return;
        }
        renderCompactScopeButton(screen, guiGraphics, mouseX, mouseY, DatabaseScope.PERSONAL);
        renderCompactScopeButton(screen, guiGraphics, mouseX, mouseY, DatabaseScope.PUBLIC);
    }

    static boolean handleCompactScopeClick(PersonalDatabaseScreen screen, double mouseX, double mouseY, DatabaseScope scope) {
        if (!PersonalDatabaseScreenCommonHelper.fitProfile(screen).usesCompactManagementLayout()) {
            return false;
        }
        PersonalDatabaseLayout.Rect scopeButtonRect = PersonalDatabaseScreenManagementGeometry.compactScopeButtonRowRect(
                screen,
                scope == DatabaseScope.PERSONAL
        );
        if (!scopeButtonRect.contains(mouseX, mouseY)) {
            return false;
        }
        switchManagementScope(screen, scope);
        return true;
    }

    private static void renderCompactScopeButton(
            PersonalDatabaseScreen screen,
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            DatabaseScope scope
    ) {
        PersonalDatabaseLayout.Rect rect = PersonalDatabaseScreenManagementGeometry.compactScopeButtonRowRect(
                screen,
                scope == DatabaseScope.PERSONAL
        );
        boolean selected = screen.managementSelectedScope == scope;
        VanillaWidgetRenderer.renderOverlayChip(guiGraphics, rect, rect.contains(mouseX, mouseY), selected, true);
        PersonalDatabaseScreenCommonHelper.drawCenteredShadow(
                screen,
                guiGraphics,
                PersonalDatabaseScreenCommonHelper.scopeLabel(scope),
                rect.x() + 2,
                rect.right() - 2,
                rect.y() + 6,
                selected ? PersonalDatabaseScreen.OVERLAY_ACCENT_TEXT_COLOR : PersonalDatabaseScreen.OVERLAY_TEXT_COLOR
        );
    }

    private static void switchManagementScope(PersonalDatabaseScreen screen, DatabaseScope scope) {
        DatabaseScope normalizedScope = DatabaseScope.normalize(scope);
        if (screen.managementSelectedScope == normalizedScope) {
            return;
        }
        DatabaseScopedTabRef nextScopedTab = PersonalDatabaseScreenManagementPanelHelper.hasScopedTab(
                screen,
                DatabaseScopedTabRef.concreteTab(normalizedScope, screen.managementSelectedTabId)
        ) ? DatabaseScopedTabRef.concreteTab(normalizedScope, screen.managementSelectedTabId)
                : PersonalDatabaseScreenManagementPanelHelper.firstConcreteTabForScope(screen, normalizedScope);
        if (nextScopedTab == null) {
            nextScopedTab = DatabaseScopedTabRef.allTab(normalizedScope);
        }
        PersonalDatabaseScreenManagementHelper.loadManagementDrafts(screen, nextScopedTab);
    }
}
