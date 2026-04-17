package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.DatabaseScopedTabRef;
import com.agguy.infiniteinventory.database.DatabaseTab;
import com.agguy.infiniteinventory.database.DatabaseTabs;
import com.agguy.infiniteinventory.menu.PersonalDatabaseLayout;
import com.agguy.infiniteinventory.network.DatabaseTabMutationAction;
import com.agguy.infiniteinventory.network.DatabaseTabMutationPayload;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

final class PersonalDatabaseScreenManagementHelper {
    private static final int MANAGEMENT_NAME_TEXT_LEFT_PADDING = PersonalDatabaseScreen.TEXT_FIELD_LEFT_PADDING;
    private static final int MANAGEMENT_NAME_TEXT_RIGHT_PADDING = PersonalDatabaseScreen.TEXT_FIELD_RIGHT_PADDING;

    private PersonalDatabaseScreenManagementHelper() {
    }

    static void ensureManagementWidgets(PersonalDatabaseScreen screen) {
        if (screen.managementNameBox == null) {
            screen.managementNameBox = screen.addScreenEditBox(new EditBox(
                    screen.screenFont(),
                    0,
                    0,
                    10,
                    12,
                    Component.translatable("screen.infiniteinventory.management.name")
            ));
            screen.managementNameBox.setBordered(false);
            screen.managementNameBox.setMaxLength(DatabaseTabs.MAX_TAB_NAME_LENGTH);
            screen.managementNameBox.setTextColor(PersonalDatabaseScreen.TEXT_FIELD_TEXT_COLOR);
            screen.managementNameBox.setTextColorUneditable(PersonalDatabaseScreen.TEXT_FIELD_MUTED_TEXT_COLOR);
            screen.managementNameBox.visible = false;
        }
        if (screen.iconSearchBox == null) {
            screen.iconSearchBox = screen.addScreenEditBox(new EditBox(
                    screen.screenFont(),
                    0,
                    0,
                    10,
                    12,
                    Component.translatable("screen.infiniteinventory.icon_picker.search")
            ));
            screen.iconSearchBox.setBordered(false);
            screen.iconSearchBox.setMaxLength(64);
            screen.iconSearchBox.setTextColor(PersonalDatabaseScreen.TEXT_FIELD_TEXT_COLOR);
            screen.iconSearchBox.setTextColorUneditable(PersonalDatabaseScreen.TEXT_FIELD_MUTED_TEXT_COLOR);
            screen.iconSearchBox.setResponder(value -> screen.iconPickerPageIndex = 0);
            screen.iconSearchBox.visible = false;
        }
    }

    static void syncManagementWidgets(PersonalDatabaseScreen screen) {
        ensureManagementWidgets(screen);
        if (screen.managementNameBox == null || screen.iconSearchBox == null) {
            return;
        }
        boolean childOverlayExpanded = screen.iconPickerExpanded || screen.targetSelectorExpanded;
        DatabaseScopedTabRef selectedScopedTab = PersonalDatabaseScreenManagementPanelHelper.selectedManagementScopedTab(screen);
        boolean selectedTabPresent = PersonalDatabaseScreenManagementPanelHelper.hasScopedTab(screen, selectedScopedTab);
        DatabaseScopedTabRef resolvedSelection = selectedTabPresent ? selectedScopedTab : preferredManagementTab(screen);
        DatabaseTab selectedTab = PersonalDatabaseScreenCommonHelper.findTab(screen, resolvedSelection);
        if (!selectedTabPresent) {
            loadManagementDrafts(screen, resolvedSelection);
        } else if (PersonalDatabaseScreenManagementLogic.shouldReloadManagementDrafts(screen, selectedTab)) {
            loadManagementDrafts(screen, resolvedSelection);
        }

        PersonalDatabaseLayout.Rect nameFieldRect = PersonalDatabaseScreenManagementGeometry.managementNameFieldRect(screen);
        screen.managementNameBox.setX(nameFieldRect.x() + MANAGEMENT_NAME_TEXT_LEFT_PADDING);
        screen.managementNameBox.setY(nameFieldRect.y() + 4);
        screen.managementNameBox.setWidth(Math.max(
                1,
                nameFieldRect.width() - MANAGEMENT_NAME_TEXT_LEFT_PADDING - MANAGEMENT_NAME_TEXT_RIGHT_PADDING
        ));
        screen.managementNameBox.setHeight(12);
        screen.managementNameBox.visible = screen.tabManagementExpanded && !childOverlayExpanded;
        screen.managementNameBox.active = selectedTab.canRename() && !childOverlayExpanded;
        if (!screen.tabManagementExpanded || childOverlayExpanded) {
            screen.managementNameBox.setFocused(false);
        }

        PersonalDatabaseLayout.Rect iconSearchRect = PersonalDatabaseScreenIconPickerGeometry.iconPickerSearchFieldRect(screen);
        screen.iconSearchBox.setX(iconSearchRect.x() + PersonalDatabaseScreen.TEXT_FIELD_LEFT_PADDING);
        screen.iconSearchBox.setY(iconSearchRect.y() + 4);
        screen.iconSearchBox.setWidth(Math.max(
                1,
                iconSearchRect.width() - PersonalDatabaseScreen.TEXT_FIELD_LEFT_PADDING - PersonalDatabaseScreen.TEXT_FIELD_RIGHT_PADDING
        ));
        screen.iconSearchBox.setHeight(12);
        screen.iconSearchBox.visible = screen.iconPickerExpanded;
        screen.iconSearchBox.active = screen.iconPickerExpanded;
        if (!screen.iconPickerExpanded) {
            screen.iconSearchBox.setFocused(false);
        }
    }

    static void loadManagementDrafts(PersonalDatabaseScreen screen, DatabaseScopedTabRef scopedTab) {
        DatabaseScopedTabRef resolvedScopedTab = scopedTab == null ? preferredManagementTab(screen) : scopedTab;
        DatabaseTab resolvedTab = PersonalDatabaseScreenCommonHelper.findTab(screen, resolvedScopedTab);
        screen.managementSelectedScope = resolvedScopedTab.scope();
        screen.managementSelectedTabId = resolvedTab.id();
        screen.pendingIconItemId = resolvedTab.iconItemId();
        screen.managementSnapshotScope = resolvedScopedTab.scope();
        screen.managementSnapshotTabId = resolvedTab.id();
        screen.managementSnapshotName = PersonalDatabaseScreenCommonHelper.tabEditableName(screen, resolvedTab);
        screen.managementSnapshotIconItemId = resolvedTab.iconItemId();
        if (screen.managementNameBox != null) {
            screen.managementNameBox.setValue(screen.managementSnapshotName);
        }
    }

    static DatabaseScopedTabRef preferredManagementTab(PersonalDatabaseScreen screen) {
        DatabaseScopedTabRef focusedTab = screen.databaseMenu.viewState().query().focusedTab();
        if (PersonalDatabaseScreenManagementPanelHelper.hasScopedTab(screen, focusedTab)) {
            DatabaseTab focused = PersonalDatabaseScreenCommonHelper.findTab(screen, focusedTab);
            if (focused.isConcreteTab()) {
                return focusedTab;
            }
        }
        DatabaseScopedTabRef concreteInFocusedScope = PersonalDatabaseScreenManagementPanelHelper.firstConcreteTabForScope(
                screen,
                focusedTab.scope()
        );
        if (concreteInFocusedScope != null) {
            return concreteInFocusedScope;
        }
        DatabaseScopedTabRef personalConcrete = PersonalDatabaseScreenManagementPanelHelper.firstConcreteTabForScope(
                screen,
                DatabaseScope.PERSONAL
        );
        if (personalConcrete != null) {
            return personalConcrete;
        }
        DatabaseScopedTabRef publicConcrete = PersonalDatabaseScreenManagementPanelHelper.firstConcreteTabForScope(
                screen,
                DatabaseScope.PUBLIC
        );
        if (publicConcrete != null) {
            return publicConcrete;
        }
        return DatabaseScopedTabRef.allTab(focusedTab.scope());
    }

    static void openIconPicker(PersonalDatabaseScreen screen) {
        ensureManagementWidgets(screen);
        screen.iconPickerExpanded = true;
        screen.iconPickerCategory = com.agguy.infiniteinventory.database.DatabaseCategory.ALL;
        screen.iconPickerPageIndex = 0;
        screen.iconPickerOriginalItemId = screen.pendingIconItemId;
        if (screen.iconSearchBox != null) {
            screen.iconSearchBox.setValue("");
            screen.iconSearchBox.setFocused(true);
        }
    }

    static void closeIconPicker(PersonalDatabaseScreen screen) {
        PersonalDatabaseScreenManagementLogic.closeIconPicker(screen, false);
    }

    static boolean applyIconPickerSelection(PersonalDatabaseScreen screen) {
        DatabaseTab selectedTab = PersonalDatabaseScreenManagementPanelHelper.selectedManagementTab(screen);
        boolean changed = PersonalDatabaseScreenManagementLogic.applySelectedTabIcon(screen, selectedTab);
        screen.iconPickerOriginalItemId = screen.pendingIconItemId;
        PersonalDatabaseScreenManagementLogic.closeIconPicker(screen, true);
        return changed;
    }

    static void closeTabManagementOverlays(PersonalDatabaseScreen screen) {
        screen.tabManagementExpanded = false;
        if (screen.managementNameBox != null) {
            screen.managementNameBox.setFocused(false);
        }
        closeIconPicker(screen);
    }

    static void sendTabMutation(
            PersonalDatabaseScreen screen,
            DatabaseScope scope,
            DatabaseTabMutationAction action,
            String tabId,
            String targetTabId,
            String name,
            String iconItemId
    ) {
        sendTabMutation(screen, scope, action, tabId, null, targetTabId, name, iconItemId);
    }

    static void sendTabMutation(
            PersonalDatabaseScreen screen,
            DatabaseScope scope,
            DatabaseTabMutationAction action,
            String tabId,
            DatabaseScope targetScope,
            String targetTabId,
            String name,
            String iconItemId
    ) {
        PacketDistributor.sendToServer(new DatabaseTabMutationPayload(
                screen.databaseMenu.containerId,
                screen.databaseMenu.viewState().sessionId(),
                DatabaseScope.normalize(scope),
                targetScope,
                action,
                tabId == null ? "" : tabId,
                targetTabId == null ? "" : targetTabId,
                name == null ? "" : name,
                iconItemId == null ? "" : iconItemId
        ));
    }

    static void renderTabManagementPanel(PersonalDatabaseScreen screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
        PersonalDatabaseScreenManagementPanelHelper.renderTabManagementPanel(screen, guiGraphics, mouseX, mouseY);
    }

    static boolean handleTabManagementClick(PersonalDatabaseScreen screen, double mouseX, double mouseY) {
        return PersonalDatabaseScreenManagementPanelHelper.handleTabManagementClick(screen, mouseX, mouseY);
    }

    static void renderIconPicker(PersonalDatabaseScreen screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
        PersonalDatabaseScreenIconPickerHelper.renderIconPicker(screen, guiGraphics, mouseX, mouseY);
    }

    static boolean handleIconPickerClick(PersonalDatabaseScreen screen, double mouseX, double mouseY) {
        return PersonalDatabaseScreenIconPickerHelper.handleIconPickerClick(screen, mouseX, mouseY);
    }

    static boolean handleManagementEnter(PersonalDatabaseScreen screen) {
        if (!screen.tabManagementExpanded) {
            return false;
        }
        return PersonalDatabaseScreenManagementLogic.saveSelectedTab(
                screen,
                PersonalDatabaseScreenManagementPanelHelper.selectedManagementTab(screen)
        );
    }
}
