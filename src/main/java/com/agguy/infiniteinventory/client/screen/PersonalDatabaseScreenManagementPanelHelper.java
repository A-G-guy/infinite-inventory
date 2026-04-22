package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.DatabaseScopedTabRef;
import com.agguy.infiniteinventory.database.DatabaseTab;
import com.agguy.infiniteinventory.menu.PersonalDatabaseLayout;
import com.agguy.infiniteinventory.network.DatabaseTabMutationAction;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

final class PersonalDatabaseScreenManagementPanelHelper {
    private PersonalDatabaseScreenManagementPanelHelper() {
    }

    static void renderTabManagementPanel(PersonalDatabaseScreen screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
        PersonalDatabaseLayout.Rect panelRect = PersonalDatabaseScreenManagementGeometry.tabManagementPanelRect(screen);
        if (panelRect.width() <= 0 || panelRect.height() <= 0) {
            return;
        }
        PersonalDatabaseScreenManagementHelper.ensureManagementWidgets(screen);
        DatabaseScopedTabRef selectedScopedTab = selectedManagementScopedTab(screen);
        DatabaseTab selectedTab = selectedManagementTab(screen);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 255.0F);
        VanillaWidgetRenderer.renderOverlayPanel(guiGraphics, panelRect);
        guiGraphics.drawString(
                screen.screenFont(),
                Component.translatable("screen.infiniteinventory.management.title"),
                panelRect.x() + PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING,
                panelRect.y() + PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING,
                PersonalDatabaseScreen.OVERLAY_TEXT_COLOR,
                false
        );
        guiGraphics.fill(
                panelRect.x() + PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING,
                panelRect.y() + PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING + PersonalDatabaseScreen.OVERLAY_SECTION_TITLE_HEIGHT,
                panelRect.right() - PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING,
                panelRect.y() + PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING + PersonalDatabaseScreen.OVERLAY_SECTION_TITLE_HEIGHT + 1,
                0x70A89E8C
        );
        PersonalDatabaseScreenOverlayRenderHelper.renderOverlayCloseButton(screen, guiGraphics, panelRect, mouseX, mouseY);

        PersonalDatabaseScreenManagementCompactHelper.renderCompactScopeSwitchers(screen, guiGraphics, mouseX, mouseY);
        PersonalDatabaseScreenManagementListHelper.renderManagementColumn(
                screen,
                guiGraphics,
                mouseX,
                mouseY,
                DatabaseScope.PERSONAL,
                selectedScopedTab
        );
        PersonalDatabaseScreenManagementListHelper.renderManagementColumn(
                screen,
                guiGraphics,
                mouseX,
                mouseY,
                DatabaseScope.PUBLIC,
                selectedScopedTab
        );

        int editorTop = PersonalDatabaseScreenManagementGeometry.managementEditorTop(screen);
        guiGraphics.drawString(
                screen.screenFont(),
                PersonalDatabaseScreenCommonHelper.scopedTabLabel(screen, selectedScopedTab),
                panelRect.x() + PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING,
                editorTop,
                PersonalDatabaseScreen.OVERLAY_MUTED_TEXT_COLOR,
                false
        );

        PersonalDatabaseLayout.Rect nameFieldRect = PersonalDatabaseScreenManagementGeometry.managementNameFieldRect(screen);
        guiGraphics.drawString(
                screen.screenFont(),
                Component.translatable("screen.infiniteinventory.management.name"),
                nameFieldRect.x(),
                nameFieldRect.y() - 12,
                PersonalDatabaseScreen.OVERLAY_TEXT_COLOR,
                false
        );
        VanillaWidgetRenderer.renderTextField(
                guiGraphics,
                nameFieldRect,
                screen.managementNameBox != null && screen.managementNameBox.isFocused()
        );
        if (screen.managementNameBox != null) {
            screen.managementNameBox.render(guiGraphics, mouseX, mouseY, 0.0F);
        }

        PersonalDatabaseLayout.Rect iconFieldRect = PersonalDatabaseScreenManagementGeometry.managementIconFieldRect(screen);
        guiGraphics.drawString(
                screen.screenFont(),
                Component.translatable("screen.infiniteinventory.management.icon"),
                iconFieldRect.x(),
                iconFieldRect.y() - 12,
                PersonalDatabaseScreen.OVERLAY_TEXT_COLOR,
                false
        );
        VanillaWidgetRenderer.renderOverlayRow(guiGraphics, iconFieldRect, iconFieldRect.contains(mouseX, mouseY), false);
        guiGraphics.renderItem(
                PersonalDatabaseScreenCommonHelper.resolveIconStack(screen, screen.pendingIconItemId, selectedTab.isAllTab()),
                iconFieldRect.x() + 2,
                iconFieldRect.y() + 2
        );
        guiGraphics.drawString(
                screen.screenFont(),
                PersonalDatabaseScreenCommonHelper.truncateToWidth(
                        screen,
                        Component.translatable("screen.infiniteinventory.management.pick_icon").getString() + " / " + screen.pendingIconItemId,
                        Math.max(0, iconFieldRect.width() - 26)
                ),
                iconFieldRect.x() + 24,
                iconFieldRect.y() + 6,
                PersonalDatabaseScreen.OVERLAY_TEXT_COLOR,
                false
        );

        int concreteCount = PersonalDatabaseScreenCommonHelper.concreteTabsForScope(screen, selectedScopedTab.scope()).size();
        PersonalDatabaseScreenManagementLogic.renderManagementActionButton(
                screen,
                guiGraphics,
                PersonalDatabaseScreenManagementGeometry.managementActionButtonRect(screen, 0, 0),
                mouseX,
                mouseY,
                PersonalDatabaseScreenManagementLogic.managementPrimaryActionLabel(screen, selectedTab),
                PersonalDatabaseScreenManagementLogic.canSaveSelectedTab(screen, selectedTab)
        );
        boolean isHiddenInTop = screen.databaseMenu.viewState().query().isTopTabHidden(selectedScopedTab);
        PersonalDatabaseScreenManagementLogic.renderManagementActionButton(
                screen,
                guiGraphics,
                PersonalDatabaseScreenManagementGeometry.managementActionButtonRect(screen, 0, 1),
                mouseX,
                mouseY,
                Component.translatable(isHiddenInTop ? "screen.infiniteinventory.management.show_in_top" : "screen.infiniteinventory.management.hide_in_top"),
                true
        );
        PersonalDatabaseScreenManagementLogic.renderManagementActionButton(
                screen,
                guiGraphics,
                PersonalDatabaseScreenManagementGeometry.managementActionButtonRect(screen, 1, 0),
                mouseX,
                mouseY,
                Component.translatable("screen.infiniteinventory.management.transfer"),
                concreteCount > 1 && selectedTab.isConcreteTab()
        );
        PersonalDatabaseScreenManagementLogic.renderManagementActionButton(
                screen,
                guiGraphics,
                PersonalDatabaseScreenManagementGeometry.managementActionButtonRect(screen, 1, 1),
                mouseX,
                mouseY,
                Component.translatable("screen.infiniteinventory.management.move_left"),
                PersonalDatabaseScreenManagementLogic.canMoveManagementTab(screen, selectedScopedTab.scope(), selectedTab, -1)
        );
        PersonalDatabaseScreenManagementLogic.renderManagementActionButton(
                screen,
                guiGraphics,
                PersonalDatabaseScreenManagementGeometry.managementActionButtonRect(screen, 2, 0),
                mouseX,
                mouseY,
                Component.translatable("screen.infiniteinventory.management.move_right"),
                PersonalDatabaseScreenManagementLogic.canMoveManagementTab(screen, selectedScopedTab.scope(), selectedTab, 1)
        );
        PersonalDatabaseScreenManagementLogic.renderManagementActionButton(
                screen,
                guiGraphics,
                PersonalDatabaseScreenManagementGeometry.managementActionButtonRect(screen, 2, 1),
                mouseX,
                mouseY,
                Component.translatable("screen.infiniteinventory.management.delete"),
                concreteCount > 1 && selectedTab.canDelete()
        );
        guiGraphics.pose().popPose();
    }

    static boolean handleTabManagementClick(PersonalDatabaseScreen screen, double mouseX, double mouseY) {
        if (!screen.tabManagementExpanded) {
            return false;
        }
        PersonalDatabaseScreenManagementHelper.ensureManagementWidgets(screen);
        PersonalDatabaseLayout.Rect panelRect = PersonalDatabaseScreenManagementGeometry.tabManagementPanelRect(screen);
        if (PersonalDatabaseScreenOverlayRenderHelper.isOverlayCloseClicked(panelRect, mouseX, mouseY)) {
            PersonalDatabaseScreenManagementHelper.closeTabManagementOverlays(screen);
            return true;
        }
        if (!panelRect.contains(mouseX, mouseY)) {
            PersonalDatabaseScreenManagementHelper.closeTabManagementOverlays(screen);
            return true;
        }

        if (PersonalDatabaseScreenManagementListHelper.handleManagementColumnClick(
                screen,
                mouseX,
                mouseY,
                DatabaseScope.PERSONAL
        )) {
            return true;
        }
        if (PersonalDatabaseScreenManagementListHelper.handleManagementColumnClick(
                screen,
                mouseX,
                mouseY,
                DatabaseScope.PUBLIC
        )) {
            return true;
        }

        if (screen.managementNameBox != null
                && PersonalDatabaseScreenManagementGeometry.managementNameFieldRect(screen).contains(mouseX, mouseY)) {
            screen.focusScreen(screen.managementNameBox);
            screen.managementNameBox.setFocused(screen.managementNameBox.active);
            screen.managementNameBox.mouseClicked(mouseX, mouseY, 0);
            return true;
        }
        if (PersonalDatabaseScreenManagementGeometry.managementIconFieldRect(screen).contains(mouseX, mouseY)) {
            PersonalDatabaseScreenManagementHelper.openIconPicker(screen);
            return true;
        }

        DatabaseScopedTabRef selectedScopedTab = selectedManagementScopedTab(screen);
        DatabaseTab selectedTab = selectedManagementTab(screen);
        int concreteCount = PersonalDatabaseScreenCommonHelper.concreteTabsForScope(screen, selectedScopedTab.scope()).size();
        if (PersonalDatabaseScreenManagementGeometry.managementActionButtonRect(screen, 0, 0).contains(mouseX, mouseY)) {
            return PersonalDatabaseScreenManagementLogic.saveSelectedTab(screen, selectedTab);
        }
        if (PersonalDatabaseScreenManagementGeometry.managementActionButtonRect(screen, 0, 1).contains(mouseX, mouseY)) {
            PersonalDatabaseScreenManagementHelper.sendTabMutation(
                    screen,
                    selectedScopedTab.scope(),
                    DatabaseTabMutationAction.TOGGLE_TOP_VISIBILITY,
                    selectedTab.id(),
                    "",
                    "",
                    ""
            );
            return true;
        }
        if (PersonalDatabaseScreenManagementGeometry.managementActionButtonRect(screen, 1, 0).contains(mouseX, mouseY)
                && concreteCount > 1
                && selectedTab.isConcreteTab()) {
            PersonalDatabaseScreenTargetHelper.openTargetSelector(
                    screen,
                    PersonalDatabaseScreen.TargetSelectorMode.TRANSFER_TAB,
                    -1,
                    -1,
                    selectedScopedTab.scope(),
                    selectedTab.id()
            );
            return true;
        }
        if (PersonalDatabaseScreenManagementGeometry.managementActionButtonRect(screen, 1, 1).contains(mouseX, mouseY)
                && PersonalDatabaseScreenManagementLogic.canMoveManagementTab(screen, selectedScopedTab.scope(), selectedTab, -1)) {
            PersonalDatabaseScreenManagementHelper.sendTabMutation(
                    screen,
                    selectedScopedTab.scope(),
                    DatabaseTabMutationAction.MOVE_LEFT,
                    selectedTab.id(),
                    "",
                    "",
                    ""
            );
            return true;
        }
        if (PersonalDatabaseScreenManagementGeometry.managementActionButtonRect(screen, 2, 0).contains(mouseX, mouseY)
                && PersonalDatabaseScreenManagementLogic.canMoveManagementTab(screen, selectedScopedTab.scope(), selectedTab, 1)) {
            PersonalDatabaseScreenManagementHelper.sendTabMutation(
                    screen,
                    selectedScopedTab.scope(),
                    DatabaseTabMutationAction.MOVE_RIGHT,
                    selectedTab.id(),
                    "",
                    "",
                    ""
            );
            return true;
        }
        if (PersonalDatabaseScreenManagementGeometry.managementActionButtonRect(screen, 2, 1).contains(mouseX, mouseY)
                && concreteCount > 1
                && selectedTab.canDelete()) {
            PersonalDatabaseScreenTargetHelper.openTargetSelector(
                    screen,
                    PersonalDatabaseScreen.TargetSelectorMode.DELETE_TAB,
                    -1,
                    -1,
                    selectedScopedTab.scope(),
                    selectedTab.id()
            );
            return true;
        }
        return true;
    }

    static DatabaseTab selectedManagementTab(PersonalDatabaseScreen screen) {
        return PersonalDatabaseScreenCommonHelper.findTab(screen, screen.managementSelectedScope, screen.managementSelectedTabId);
    }

    static DatabaseScopedTabRef selectedManagementScopedTab(PersonalDatabaseScreen screen) {
        return DatabaseScopedTabRef.concreteTab(screen.managementSelectedScope, screen.managementSelectedTabId);
    }

    static boolean hasScopedTab(PersonalDatabaseScreen screen, DatabaseScopedTabRef scopedTab) {
        return PersonalDatabaseScreenCommonHelper.tabsForScope(screen, scopedTab.scope()).stream()
                .anyMatch(tab -> tab.id().equals(scopedTab.tabId()));
    }

    static DatabaseScopedTabRef firstConcreteTabForScope(PersonalDatabaseScreen screen, DatabaseScope scope) {
        List<DatabaseTab> concreteTabs = PersonalDatabaseScreenCommonHelper.concreteTabsForScope(screen, scope);
        if (concreteTabs.isEmpty()) {
            return null;
        }
        return DatabaseScopedTabRef.concreteTab(scope, concreteTabs.getFirst().id());
    }

    static boolean scrollManagementList(PersonalDatabaseScreen screen, double mouseX, double mouseY, int deltaRows) {
        return PersonalDatabaseScreenManagementListHelper.scrollManagementList(screen, mouseX, mouseY, deltaRows);
    }
}
