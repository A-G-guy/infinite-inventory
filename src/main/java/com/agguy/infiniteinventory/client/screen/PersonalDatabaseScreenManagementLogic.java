package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.database.DatabaseTab;
import com.agguy.infiniteinventory.menu.PersonalDatabaseLayout;
import com.agguy.infiniteinventory.network.DatabaseTabMutationAction;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

final class PersonalDatabaseScreenManagementLogic {
    private PersonalDatabaseScreenManagementLogic() {
    }

    static boolean shouldReloadManagementDrafts(PersonalDatabaseScreen screen, DatabaseTab selectedTab) {
        if (selectedTab == null) {
            return false;
        }
        boolean snapshotChanged = screen.managementSnapshotScope != screen.databaseMenu.viewState().query().scope()
                || !Objects.equals(screen.managementSnapshotTabId, selectedTab.id())
                || !Objects.equals(screen.managementSnapshotName, PersonalDatabaseScreenCommonHelper.tabEditableName(screen, selectedTab))
                || !Objects.equals(screen.managementSnapshotIconItemId, selectedTab.iconItemId());
        if (!snapshotChanged) {
            return false;
        }
        return (screen.managementNameBox == null || !screen.managementNameBox.isFocused()) && !screen.iconPickerExpanded;
    }

    static void renderManagementActionButton(
            PersonalDatabaseScreen screen,
            GuiGraphics guiGraphics,
            PersonalDatabaseLayout.Rect rect,
            int mouseX,
            int mouseY,
            Component label,
            boolean enabled
    ) {
        VanillaWidgetRenderer.renderOverlayChip(guiGraphics, rect, rect.contains(mouseX, mouseY), false, enabled);
        PersonalDatabaseScreenCommonHelper.drawCenteredShadow(
                screen,
                guiGraphics,
                label,
                rect.x() + 2,
                rect.right() - 2,
                rect.y() + 6,
                enabled ? PersonalDatabaseScreen.OVERLAY_TEXT_COLOR : PersonalDatabaseScreen.OVERLAY_MUTED_TEXT_COLOR
        );
    }

    static boolean canMoveManagementTab(PersonalDatabaseScreen screen, DatabaseTab selectedTab, int direction) {
        if (selectedTab == null || !selectedTab.isConcreteTab()) {
            return false;
        }
        List<DatabaseTab> concreteTabs = PersonalDatabaseScreenCommonHelper.currentConcreteTabs(screen);
        int selectedIndex = -1;
        for (int index = 0; index < concreteTabs.size(); index++) {
            if (concreteTabs.get(index).id().equals(selectedTab.id())) {
                selectedIndex = index;
                break;
            }
        }
        if (selectedIndex < 0) {
            return false;
        }
        int nextIndex = selectedIndex + direction;
        return nextIndex >= 0 && nextIndex < concreteTabs.size();
    }

    static boolean canSaveSelectedTab(PersonalDatabaseScreen screen, DatabaseTab selectedTab) {
        if (selectedTab == null) {
            return false;
        }
        if (selectedTab.canRename() && !managementDraftName(screen).equals(PersonalDatabaseScreenCommonHelper.tabEditableName(screen, selectedTab))) {
            return true;
        }
        return !Objects.equals(screen.pendingIconItemId, selectedTab.iconItemId());
    }

    static boolean saveSelectedTab(PersonalDatabaseScreen screen, DatabaseTab selectedTab) {
        if (selectedTab == null) {
            return false;
        }
        boolean changed = false;
        String draftName = managementDraftName(screen);
        if (selectedTab.canRename() && !draftName.equals(PersonalDatabaseScreenCommonHelper.tabEditableName(screen, selectedTab))) {
            PersonalDatabaseScreenManagementHelper.sendTabMutation(
                    screen,
                    DatabaseTabMutationAction.RENAME,
                    selectedTab.id(),
                    "",
                    draftName,
                    ""
            );
            changed = true;
        }
        if (!Objects.equals(screen.pendingIconItemId, selectedTab.iconItemId())) {
            PersonalDatabaseScreenManagementHelper.sendTabMutation(
                    screen,
                    DatabaseTabMutationAction.CHANGE_ICON,
                    selectedTab.id(),
                    "",
                    "",
                    screen.pendingIconItemId
            );
            changed = true;
        }
        return changed;
    }

    static void closeIconPicker(PersonalDatabaseScreen screen, boolean applySelection) {
        if (!applySelection) {
            screen.pendingIconItemId = screen.iconPickerOriginalItemId;
        }
        screen.iconPickerExpanded = false;
        if (screen.iconSearchBox != null) {
            screen.iconSearchBox.setFocused(false);
        }
    }

    static String managementDraftName(PersonalDatabaseScreen screen) {
        return screen.managementNameBox == null ? "" : screen.managementNameBox.getValue().trim();
    }
}
