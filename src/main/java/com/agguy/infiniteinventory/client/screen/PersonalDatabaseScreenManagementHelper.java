package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.database.DatabaseTab;
import com.agguy.infiniteinventory.database.DatabaseTabs;
import com.agguy.infiniteinventory.database.DatabaseScope;
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
        boolean selectedTabPresent = PersonalDatabaseScreenCommonHelper.currentTabs(screen).stream()
                .anyMatch(tab -> tab.id().equals(screen.managementSelectedTabId));
        DatabaseTab selectedTab = selectedTabPresent
                ? PersonalDatabaseScreenCommonHelper.findTab(screen, screen.managementSelectedTabId)
                : preferredManagementTab(screen);
        if (!selectedTabPresent) {
            loadManagementDrafts(screen, selectedTab);
        } else if (PersonalDatabaseScreenManagementLogic.shouldReloadManagementDrafts(screen, selectedTab)) {
            loadManagementDrafts(screen, selectedTab);
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

    static void loadManagementDrafts(PersonalDatabaseScreen screen, DatabaseTab tab) {
        DatabaseTab resolvedTab = tab == null ? DatabaseTabs.defaultConcreteTab() : tab;
        screen.managementSelectedTabId = resolvedTab.id();
        screen.pendingIconItemId = resolvedTab.iconItemId();
        screen.managementSnapshotScope = screen.databaseMenu.viewState().query().scope();
        screen.managementSnapshotTabId = resolvedTab.id();
        screen.managementSnapshotName = PersonalDatabaseScreenCommonHelper.tabEditableName(screen, resolvedTab);
        screen.managementSnapshotIconItemId = resolvedTab.iconItemId();
        if (screen.managementNameBox != null) {
            screen.managementNameBox.setValue(screen.managementSnapshotName);
        }
    }

    static DatabaseTab preferredManagementTab(PersonalDatabaseScreen screen) {
        DatabaseTab focusedTab = PersonalDatabaseScreenCommonHelper.findTab(
                screen,
                screen.databaseMenu.viewState().query().focusedTabId()
        );
        if (focusedTab.isConcreteTab() || PersonalDatabaseScreenCommonHelper.currentConcreteTabs(screen).isEmpty()) {
            return focusedTab;
        }
        return PersonalDatabaseScreenCommonHelper.currentConcreteTabs(screen).getFirst();
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
        DatabaseTab selectedTab = PersonalDatabaseScreenCommonHelper.findTab(screen, screen.managementSelectedTabId);
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
            DatabaseTabMutationAction action,
            String tabId,
            String targetTabId,
            String name,
            String iconItemId
    ) {
        sendTabMutation(screen, action, tabId, null, targetTabId, name, iconItemId);
    }

    static void sendTabMutation(
            PersonalDatabaseScreen screen,
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
                screen.databaseMenu.viewState().query().scope(),
                targetScope,
                action,
                tabId == null ? "" : tabId,
                targetTabId == null ? "" : targetTabId,
                name == null ? "" : name,
                iconItemId == null ? "" : iconItemId
        ));
    }

    static void renderTabManagementPanel(PersonalDatabaseScreen screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
        PersonalDatabaseLayout.Rect panelRect = PersonalDatabaseScreenManagementGeometry.tabManagementPanelRect(screen);
        if (panelRect.width() <= 0 || panelRect.height() <= 0) {
            return;
        }
        ensureManagementWidgets(screen);
        DatabaseTab selectedTab = PersonalDatabaseScreenCommonHelper.findTab(screen, screen.managementSelectedTabId);
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
        PersonalDatabaseScreenManagementLogic.renderManagementActionButton(
                screen,
                guiGraphics,
                PersonalDatabaseScreenManagementGeometry.managementTopLevelAddButtonRect(screen),
                mouseX,
                mouseY,
                Component.translatable("screen.infiniteinventory.management.add"),
                true
        );
        PersonalDatabaseScreenOverlayRenderHelper.renderOverlayCloseButton(screen, guiGraphics, panelRect, mouseX, mouseY);

        List<DatabaseTab> tabs = PersonalDatabaseScreenCommonHelper.currentTabs(screen);
        for (int index = 0; index < tabs.size(); index++) {
            PersonalDatabaseLayout.Rect rowRect = PersonalDatabaseScreenManagementGeometry.managementListRowRect(screen, index);
            if (rowRect.bottom() > panelRect.bottom() - PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING) {
                break;
            }
            DatabaseTab tab = tabs.get(index);
            boolean hovered = rowRect.contains(mouseX, mouseY);
            boolean selected = tab.id().equals(selectedTab.id());
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
                PersonalDatabaseScreenCommonHelper.currentConcreteTabs(screen).size() > 1 && selectedTab.isConcreteTab()
        );
        PersonalDatabaseScreenManagementLogic.renderManagementActionButton(
                screen,
                guiGraphics,
                PersonalDatabaseScreenManagementGeometry.managementActionButtonRect(screen, 1, 1),
                mouseX,
                mouseY,
                Component.translatable("screen.infiniteinventory.management.move_left"),
                PersonalDatabaseScreenManagementLogic.canMoveManagementTab(screen, selectedTab, -1)
        );
        PersonalDatabaseScreenManagementLogic.renderManagementActionButton(
                screen,
                guiGraphics,
                PersonalDatabaseScreenManagementGeometry.managementActionButtonRect(screen, 2, 0),
                mouseX,
                mouseY,
                Component.translatable("screen.infiniteinventory.management.move_right"),
                PersonalDatabaseScreenManagementLogic.canMoveManagementTab(screen, selectedTab, 1)
        );
        PersonalDatabaseScreenManagementLogic.renderManagementActionButton(
                screen,
                guiGraphics,
                PersonalDatabaseScreenManagementGeometry.managementActionButtonRect(screen, 2, 1),
                mouseX,
                mouseY,
                Component.translatable("screen.infiniteinventory.management.delete"),
                PersonalDatabaseScreenCommonHelper.currentConcreteTabs(screen).size() > 1 && selectedTab.canDelete()
        );
        guiGraphics.pose().popPose();
    }

    static boolean handleTabManagementClick(PersonalDatabaseScreen screen, double mouseX, double mouseY) {
        if (!screen.tabManagementExpanded) {
            return false;
        }
        ensureManagementWidgets(screen);
        PersonalDatabaseLayout.Rect panelRect = PersonalDatabaseScreenManagementGeometry.tabManagementPanelRect(screen);
        if (PersonalDatabaseScreenOverlayRenderHelper.isOverlayCloseClicked(panelRect, mouseX, mouseY)) {
            closeTabManagementOverlays(screen);
            return true;
        }
        if (PersonalDatabaseScreenManagementGeometry.managementTopLevelAddButtonRect(screen).contains(mouseX, mouseY)) {
            sendTabMutation(
                    screen,
                    DatabaseTabMutationAction.ADD,
                    "",
                    "",
                    "",
                    DatabaseTabs.DEFAULT_CONCRETE_ICON_ITEM_ID
            );
            return true;
        }
        if (!panelRect.contains(mouseX, mouseY)) {
            closeTabManagementOverlays(screen);
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
            openIconPicker(screen);
            return true;
        }

        List<DatabaseTab> tabs = PersonalDatabaseScreenCommonHelper.currentTabs(screen);
        for (int index = 0; index < tabs.size(); index++) {
            PersonalDatabaseLayout.Rect rowRect = PersonalDatabaseScreenManagementGeometry.managementListRowRect(screen, index);
            if (rowRect.bottom() > panelRect.bottom() - PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING) {
                break;
            }
            if (rowRect.contains(mouseX, mouseY)) {
                loadManagementDrafts(screen, tabs.get(index));
                return true;
            }
        }

        DatabaseTab selectedTab = PersonalDatabaseScreenCommonHelper.findTab(screen, screen.managementSelectedTabId);
        if (PersonalDatabaseScreenManagementGeometry.managementActionButtonRect(screen, 0, 0).contains(mouseX, mouseY)) {
            return PersonalDatabaseScreenManagementLogic.saveSelectedTab(screen, selectedTab);
        }
        if (PersonalDatabaseScreenManagementGeometry.managementActionButtonRect(screen, 0, 1).contains(mouseX, mouseY)) {
            openIconPicker(screen);
            return true;
        }
        if (PersonalDatabaseScreenManagementGeometry.managementActionButtonRect(screen, 1, 0).contains(mouseX, mouseY)
                && PersonalDatabaseScreenCommonHelper.currentConcreteTabs(screen).size() > 1
                && selectedTab.isConcreteTab()) {
            PersonalDatabaseScreenTargetHelper.openTargetSelector(
                    screen,
                    PersonalDatabaseScreen.TargetSelectorMode.TRANSFER_TAB,
                    -1,
                    -1,
                    selectedTab.id()
            );
            return true;
        }
        if (PersonalDatabaseScreenManagementGeometry.managementActionButtonRect(screen, 1, 1).contains(mouseX, mouseY)
                && PersonalDatabaseScreenManagementLogic.canMoveManagementTab(screen, selectedTab, -1)) {
            sendTabMutation(screen, DatabaseTabMutationAction.MOVE_LEFT, selectedTab.id(), "", "", "");
            return true;
        }
        if (PersonalDatabaseScreenManagementGeometry.managementActionButtonRect(screen, 2, 0).contains(mouseX, mouseY)
                && PersonalDatabaseScreenManagementLogic.canMoveManagementTab(screen, selectedTab, 1)) {
            sendTabMutation(screen, DatabaseTabMutationAction.MOVE_RIGHT, selectedTab.id(), "", "", "");
            return true;
        }
        if (PersonalDatabaseScreenManagementGeometry.managementActionButtonRect(screen, 2, 1).contains(mouseX, mouseY)
                && PersonalDatabaseScreenCommonHelper.currentConcreteTabs(screen).size() > 1
                && selectedTab.canDelete()) {
            PersonalDatabaseScreenTargetHelper.openTargetSelector(
                    screen,
                    PersonalDatabaseScreen.TargetSelectorMode.DELETE_TAB,
                    -1,
                    -1,
                    selectedTab.id()
            );
            return true;
        }
        return true;
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
                PersonalDatabaseScreenCommonHelper.findTab(screen, screen.managementSelectedTabId)
        );
    }
}
