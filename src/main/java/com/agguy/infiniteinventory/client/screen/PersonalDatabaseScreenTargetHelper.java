package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.database.DatabasePanelView;
import com.agguy.infiniteinventory.database.DatabaseTab;
import com.agguy.infiniteinventory.database.DatabaseTabs;
import com.agguy.infiniteinventory.menu.PersonalDatabaseLayout;
import com.agguy.infiniteinventory.network.DatabaseClickAction;
import com.agguy.infiniteinventory.network.DatabaseEnhancementPayload;
import com.agguy.infiniteinventory.network.DatabaseQuickDepositPayload;
import com.agguy.infiniteinventory.network.DatabaseTabMutationAction;
import com.agguy.infiniteinventory.network.DepositAllPayload;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.network.PacketDistributor;

final class PersonalDatabaseScreenTargetHelper {
    private PersonalDatabaseScreenTargetHelper() {
    }

    static void openTargetSelector(
            PersonalDatabaseScreen screen,
            PersonalDatabaseScreen.TargetSelectorMode mode,
            int panelIndex,
            int slotIndex,
            String sourceTabId
    ) {
        PersonalDatabaseScreenContextHelper.closeContextMenu(screen);
        screen.sortDropdownExpanded = false;
        screen.pagePickerExpanded = false;
        screen.moreTabsExpanded = false;
        screen.viewSelectorExpanded = false;
        screen.targetSelectorMode = mode == null ? PersonalDatabaseScreen.TargetSelectorMode.NONE : mode;
        screen.pendingTargetPanelIndex = panelIndex;
        screen.pendingQuickDepositSlotIndex = slotIndex;
        screen.pendingTargetSourceTabId = sourceTabId == null ? "" : sourceTabId;
        screen.targetSelectorScrollIndex = 0;
        screen.targetSelectorExpanded = true;
    }

    static void closeTargetSelector(PersonalDatabaseScreen screen) {
        screen.targetSelectorExpanded = false;
        screen.targetSelectorMode = PersonalDatabaseScreen.TargetSelectorMode.NONE;
        screen.pendingTargetPanelIndex = -1;
        screen.pendingQuickDepositSlotIndex = -1;
        screen.pendingTargetSourceTabId = "";
        screen.pendingTargetStoresSingle = false;
        screen.targetSelectorScrollIndex = 0;
    }

    static void closeTransientOverlays(PersonalDatabaseScreen screen) {
        PersonalDatabaseScreenContextHelper.closeContextMenu(screen);
        screen.sortDropdownExpanded = false;
        screen.activeSortPanelIndex = -1;
        screen.pagePickerExpanded = false;
        screen.activePagePickerPanelIndex = -1;
        screen.enhancementPanelExpanded = false;
        screen.viewSelectorExpanded = false;
        screen.moreTabsExpanded = false;
        closeTargetSelector(screen);
        PersonalDatabaseScreenManagementHelper.closeTabManagementOverlays(screen);
    }

    static void renderTargetSelector(PersonalDatabaseScreen screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
        PersonalDatabaseLayout.Rect panelRect = PersonalDatabaseScreenGeometry.targetSelectorRect(screen);
        if (panelRect.width() <= 0 || panelRect.height() <= 0) {
            return;
        }
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 254.0F);
        VanillaWidgetRenderer.renderOverlayPanel(guiGraphics, panelRect);
        guiGraphics.drawString(
                screen.screenFont(),
                targetSelectorTitle(screen),
                panelRect.x() + 8,
                panelRect.y() + 8,
                PersonalDatabaseScreen.OVERLAY_TEXT_COLOR,
                false
        );
        guiGraphics.fill(panelRect.x() + 8, panelRect.y() + 20, panelRect.right() - 8, panelRect.y() + 21, 0x70A89E8C);
        PersonalDatabaseScreenOverlayRenderHelper.renderOverlayCloseButton(screen, guiGraphics, panelRect, mouseX, mouseY);

        List<DatabaseTab> candidateTabs = targetSelectorTabs(screen);
        List<DatabaseTab> visibleTabs = visibleTargetSelectorTabs(screen, candidateTabs);
        if (candidateTabs.isEmpty()) {
            PersonalDatabaseScreenCommonHelper.drawCenteredShadow(
                    screen,
                    guiGraphics,
                    Component.translatable("screen.infiniteinventory.target_selector.none"),
                    panelRect.x() + 8,
                    panelRect.right() - 8,
                    panelRect.y() + 36,
                    PersonalDatabaseScreen.OVERLAY_MUTED_TEXT_COLOR
            );
            guiGraphics.pose().popPose();
            return;
        }

        String selectedTargetTabId = screen.targetSelectorMode == PersonalDatabaseScreen.TargetSelectorMode.AUTO_STORE_TARGET
                ? screen.databaseMenu.viewState().autoStoreTargetTabId()
                : "";
        for (int index = 0; index < visibleTabs.size(); index++) {
            DatabaseTab tab = visibleTabs.get(index);
            PersonalDatabaseLayout.Rect rowRect = PersonalDatabaseScreenGeometry.selectorRowRect(
                    panelRect,
                    index,
                    PersonalDatabaseScreen.TARGET_SELECTOR_ROW_HEIGHT
            );
            boolean hovered = rowRect.contains(mouseX, mouseY);
            boolean selected = selectedTargetTabId.equals(tab.id());
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
        guiGraphics.pose().popPose();
    }

    static boolean handleTargetSelectorClick(PersonalDatabaseScreen screen, double mouseX, double mouseY) {
        if (!screen.targetSelectorExpanded) {
            return false;
        }
        PersonalDatabaseLayout.Rect panelRect = PersonalDatabaseScreenGeometry.targetSelectorRect(screen);
        if (PersonalDatabaseScreenOverlayRenderHelper.isOverlayCloseClicked(panelRect, mouseX, mouseY)) {
            closeTargetSelector(screen);
            return true;
        }
        if (!panelRect.contains(mouseX, mouseY)) {
            closeTargetSelector(screen);
            return true;
        }
        List<DatabaseTab> candidateTabs = targetSelectorTabs(screen);
        List<DatabaseTab> visibleTabs = visibleTargetSelectorTabs(screen, candidateTabs);
        for (int index = 0; index < visibleTabs.size(); index++) {
            PersonalDatabaseLayout.Rect rowRect = PersonalDatabaseScreenGeometry.selectorRowRect(
                    panelRect,
                    index,
                    PersonalDatabaseScreen.TARGET_SELECTOR_ROW_HEIGHT
            );
            if (!rowRect.contains(mouseX, mouseY)) {
                continue;
            }
            applyTargetSelection(screen, visibleTabs.get(index).id());
            return true;
        }
        return true;
    }

    static boolean scrollTargetSelector(PersonalDatabaseScreen screen, int deltaRows) {
        if (!screen.targetSelectorExpanded || deltaRows == 0) {
            return false;
        }
        List<DatabaseTab> candidateTabs = targetSelectorTabs(screen);
        int maxVisibleRows = maxVisibleRows(screen);
        int maxScrollIndex = Math.max(0, candidateTabs.size() - maxVisibleRows);
        int nextScrollIndex = Math.max(0, Math.min(maxScrollIndex, screen.targetSelectorScrollIndex + deltaRows));
        if (nextScrollIndex == screen.targetSelectorScrollIndex) {
            return false;
        }
        screen.targetSelectorScrollIndex = nextScrollIndex;
        return true;
    }

    static List<DatabaseTab> targetSelectorTabs(PersonalDatabaseScreen screen) {
        return switch (screen.targetSelectorMode) {
            case DEPOSIT_ALL, CARRIED_STORE, QUICK_DEPOSIT -> {
                LinkedHashSet<String> visibleConcreteTabIds = new LinkedHashSet<>();
                for (DatabasePanelView panel : PersonalDatabaseScreenCommonHelper.currentPanels(screen)) {
                    if (panel.tab().isConcreteTab()) {
                        visibleConcreteTabIds.add(panel.tab().id());
                    }
                }
                if (visibleConcreteTabIds.isEmpty()) {
                    for (DatabaseTab tab : PersonalDatabaseScreenCommonHelper.currentConcreteTabs(screen)) {
                        visibleConcreteTabIds.add(tab.id());
                    }
                }
                List<DatabaseTab> candidateTabs = new ArrayList<>();
                for (DatabaseTab tab : PersonalDatabaseScreenCommonHelper.currentTabs(screen)) {
                    if (visibleConcreteTabIds.contains(tab.id())) {
                        candidateTabs.add(tab);
                    }
                }
                yield List.copyOf(candidateTabs);
            }
            case TRANSFER_TAB, DELETE_TAB -> PersonalDatabaseScreenCommonHelper.currentConcreteTabs(screen).stream()
                    .filter(tab -> !tab.id().equals(screen.pendingTargetSourceTabId))
                    .toList();
            case AUTO_STORE_TARGET -> screen.databaseMenu.viewState().personalTabs().stream()
                    .filter(DatabaseTab::isConcreteTab)
                    .toList();
            case NONE -> List.of();
        };
    }

    static Component targetSelectorTitle(PersonalDatabaseScreen screen) {
        return switch (screen.targetSelectorMode) {
            case DEPOSIT_ALL -> Component.translatable("screen.infiniteinventory.target_selector.deposit_all");
            case CARRIED_STORE -> Component.translatable("screen.infiniteinventory.target_selector.store");
            case QUICK_DEPOSIT -> Component.translatable("screen.infiniteinventory.target_selector.quick_deposit");
            case TRANSFER_TAB -> Component.translatable("screen.infiniteinventory.target_selector.transfer");
            case DELETE_TAB -> Component.translatable("screen.infiniteinventory.target_selector.delete");
            case AUTO_STORE_TARGET -> Component.translatable("screen.infiniteinventory.target_selector.auto_store");
            case NONE -> Component.translatable("screen.infiniteinventory.target_selector.title");
        };
    }

    static boolean handleQuickDepositClick(PersonalDatabaseScreen screen, double mouseX, double mouseY, int button) {
        if (button != 0 || !net.minecraft.client.gui.screens.Screen.hasShiftDown()
                || screen.hoveredSlotRef() == null
                || !screen.databaseMenu.getCarried().isEmpty()) {
            return false;
        }
        Slot hoveredSlot = screen.hoveredSlotRef();
        int slotIndex = screen.databaseMenu.slots.indexOf(hoveredSlot);
        if (slotIndex < 0 || !hoveredSlot.hasItem() || !isQuickDepositSlot(screen, slotIndex, hoveredSlot)) {
            return false;
        }
        String directTargetTabId = PersonalDatabaseScreenCommonHelper.resolveSingleStoreTargetTabId(screen);
        if (directTargetTabId != null) {
            PacketDistributor.sendToServer(new DatabaseQuickDepositPayload(
                    screen.databaseMenu.containerId,
                    screen.databaseMenu.viewState().sessionId(),
                    slotIndex,
                    directTargetTabId
            ));
            return true;
        }
        openTargetSelector(screen, PersonalDatabaseScreen.TargetSelectorMode.QUICK_DEPOSIT, -1, slotIndex, "");
        return true;
    }

    static boolean handleEnhancementPanelClick(PersonalDatabaseScreen screen, double mouseX, double mouseY) {
        PersonalDatabaseLayout.Rect autoStoreRowRect = PersonalDatabaseScreenGeometry.enhancementAutoStoreRowRect(screen);
        if (autoStoreRowRect.contains(mouseX, mouseY)) {
            openTargetSelector(screen, PersonalDatabaseScreen.TargetSelectorMode.AUTO_STORE_TARGET, -1, -1, "");
            return true;
        }
        return false;
    }

    private static void applyTargetSelection(PersonalDatabaseScreen screen, String targetTabId) {
        switch (screen.targetSelectorMode) {
            case DEPOSIT_ALL -> PacketDistributor.sendToServer(new DepositAllPayload(
                    screen.databaseMenu.containerId,
                    screen.databaseMenu.viewState().sessionId(),
                    targetTabId
            ));
            case CARRIED_STORE -> PersonalDatabaseScreenLayoutHelper.sendDatabaseClick(
                    screen,
                    Math.max(0, screen.pendingTargetPanelIndex),
                    0,
                    screen.pendingTargetStoresSingle ? DatabaseClickAction.STORE_SINGLE : DatabaseClickAction.STORE_STACK,
                    targetTabId
            );
            case QUICK_DEPOSIT -> PacketDistributor.sendToServer(new DatabaseQuickDepositPayload(
                    screen.databaseMenu.containerId,
                    screen.databaseMenu.viewState().sessionId(),
                    screen.pendingQuickDepositSlotIndex,
                    targetTabId
            ));
            case TRANSFER_TAB -> PersonalDatabaseScreenManagementHelper.sendTabMutation(
                    screen,
                    DatabaseTabMutationAction.TRANSFER,
                    screen.pendingTargetSourceTabId,
                    targetTabId,
                    "",
                    ""
            );
            case DELETE_TAB -> PersonalDatabaseScreenManagementHelper.sendTabMutation(
                    screen,
                    DatabaseTabMutationAction.DELETE,
                    screen.pendingTargetSourceTabId,
                    targetTabId,
                    "",
                    ""
            );
            case AUTO_STORE_TARGET -> PacketDistributor.sendToServer(new DatabaseEnhancementPayload(
                    screen.databaseMenu.containerId,
                    screen.databaseMenu.viewState().sessionId(),
                    screen.databaseMenu.viewState().enhancementConfig(),
                    targetTabId
            ));
            case NONE -> {
            }
        }
        closeTargetSelector(screen);
    }

    private static boolean isQuickDepositSlot(PersonalDatabaseScreen screen, int slotIndex, Slot slot) {
        return screen.databaseMenu.isAccessorySlotIndex(slotIndex)
                || (slot.container instanceof Inventory && slot.getContainerSlot() >= 0 && slot.getContainerSlot() < 36);
    }

    private static List<DatabaseTab> visibleTargetSelectorTabs(PersonalDatabaseScreen screen, List<DatabaseTab> candidateTabs) {
        if (candidateTabs.isEmpty()) {
            return List.of();
        }
        int maxVisibleRows = maxVisibleRows(screen);
        int maxScrollIndex = Math.max(0, candidateTabs.size() - maxVisibleRows);
        screen.targetSelectorScrollIndex = Math.max(0, Math.min(maxScrollIndex, screen.targetSelectorScrollIndex));
        int fromIndex = Math.min(candidateTabs.size(), screen.targetSelectorScrollIndex);
        int toIndex = Math.min(candidateTabs.size(), fromIndex + maxVisibleRows);
        return candidateTabs.subList(fromIndex, toIndex);
    }

    private static int maxVisibleRows(PersonalDatabaseScreen screen) {
        PersonalDatabaseLayout.Rect panelRect = PersonalDatabaseScreenGeometry.targetSelectorRect(screen);
        int contentTop = panelRect.y()
                + PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING
                + PersonalDatabaseScreen.OVERLAY_SECTION_TITLE_HEIGHT
                + 8;
        int contentBottom = panelRect.bottom() - PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING;
        int availableHeight = Math.max(0, contentBottom - contentTop);
        return Math.max(1, availableHeight / PersonalDatabaseScreen.TARGET_SELECTOR_ROW_HEIGHT);
    }
}
