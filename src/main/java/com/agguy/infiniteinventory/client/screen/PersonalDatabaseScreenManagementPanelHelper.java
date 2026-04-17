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

        renderManagementColumn(screen, guiGraphics, mouseX, mouseY, DatabaseScope.PERSONAL, selectedScopedTab);
        renderManagementColumn(screen, guiGraphics, mouseX, mouseY, DatabaseScope.PUBLIC, selectedScopedTab);

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
                PersonalDatabaseScreenGeometry.truncateToWidth(
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
        PersonalDatabaseScreenManagementLogic.renderManagementActionButton(
                screen,
                guiGraphics,
                PersonalDatabaseScreenManagementGeometry.managementActionButtonRect(screen, 0, 1),
                mouseX,
                mouseY,
                Component.translatable("screen.infiniteinventory.management.pick_icon"),
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

        if (handleManagementColumnClick(screen, mouseX, mouseY, DatabaseScope.PERSONAL)) {
            return true;
        }
        if (handleManagementColumnClick(screen, mouseX, mouseY, DatabaseScope.PUBLIC)) {
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
            PersonalDatabaseScreenManagementHelper.openIconPicker(screen);
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

    private static void renderManagementColumn(
            PersonalDatabaseScreen screen,
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            DatabaseScope scope,
            DatabaseScopedTabRef selectedScopedTab
    ) {
        boolean personalColumn = scope == DatabaseScope.PERSONAL;
        PersonalDatabaseLayout.Rect headerRect = PersonalDatabaseScreenManagementGeometry.managementScopeHeaderRect(screen, personalColumn);
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
        for (int index = 0; index < tabs.size(); index++) {
            DatabaseTab tab = tabs.get(index);
            PersonalDatabaseLayout.Rect rowRect = PersonalDatabaseScreenManagementGeometry.managementListRowRect(
                    screen,
                    personalColumn,
                    index
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
    }

    private static boolean handleManagementColumnClick(
            PersonalDatabaseScreen screen,
            double mouseX,
            double mouseY,
            DatabaseScope scope
    ) {
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
        for (int index = 0; index < tabs.size(); index++) {
            PersonalDatabaseLayout.Rect rowRect = PersonalDatabaseScreenManagementGeometry.managementListRowRect(
                    screen,
                    personalColumn,
                    index
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
}
