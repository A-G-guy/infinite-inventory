package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.database.DatabaseAutoStoreTarget;
import com.agguy.infiniteinventory.database.DatabaseScopedTabRef;
import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.DatabaseTab;
import com.agguy.infiniteinventory.menu.PersonalDatabaseLayout;
import com.agguy.infiniteinventory.network.DatabaseClickAction;
import com.agguy.infiniteinventory.network.DatabaseSelectionAction;
import com.agguy.infiniteinventory.network.DatabaseEnhancementPayload;
import com.agguy.infiniteinventory.network.DatabaseQuickDepositPayload;
import com.agguy.infiniteinventory.network.DatabaseTabMutationAction;
import com.agguy.infiniteinventory.network.DepositAllPayload;
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
        openTargetSelector(
                screen,
                mode,
                panelIndex,
                slotIndex,
                screen.databaseMenu.viewState().query().focusedTab().scope(),
                sourceTabId
        );
    }

    static void openTargetSelector(
            PersonalDatabaseScreen screen,
            PersonalDatabaseScreen.TargetSelectorMode mode,
            int panelIndex,
            int slotIndex,
            DatabaseScope sourceScope,
            String sourceTabId
    ) {
        PersonalDatabaseScreenContextHelper.closeContextMenu(screen);
        PersonalDatabaseScreenCustomExtractOverlayHelper.closeOverlay(screen);
        screen.sortDropdownExpanded = false;
        screen.pagePickerExpanded = false;
        screen.moreTabsExpanded = false;
        screen.viewSelectorExpanded = false;
        screen.targetSelectorMode = mode == null ? PersonalDatabaseScreen.TargetSelectorMode.NONE : mode;
        screen.pendingTargetPanelIndex = panelIndex;
        screen.pendingQuickDepositSlotIndex = slotIndex;
        screen.pendingTargetSourceTabId = sourceTabId == null ? "" : sourceTabId;
        screen.pendingTargetSourceScope = DatabaseScope.normalize(sourceScope);
        screen.targetSelectorScrollIndex = 0;
        screen.targetSelectorExpanded = true;
    }

    static void closeTargetSelector(PersonalDatabaseScreen screen) {
        screen.targetSelectorExpanded = false;
        screen.targetSelectorMode = PersonalDatabaseScreen.TargetSelectorMode.NONE;
        screen.pendingTargetPanelIndex = -1;
        screen.pendingQuickDepositSlotIndex = -1;
        screen.pendingTargetSourceTabId = "";
        screen.pendingTargetSourceScope = screen.databaseMenu.viewState().query().focusedTab().scope();
        screen.pendingTargetStoresSingle = false;
        screen.targetSelectorScrollIndex = 0;
    }

    static void closeTransientOverlays(PersonalDatabaseScreen screen) {
        PersonalDatabaseScreenContextHelper.closeContextMenu(screen);
        PersonalDatabaseScreenCustomExtractOverlayHelper.closeOverlay(screen);
        screen.sortDropdownExpanded = false;
        screen.activeSortPanelIndex = -1;
        screen.pagePickerExpanded = false;
        screen.activePagePickerPanelIndex = -1;
        screen.enhancementPanelExpanded = false;
        screen.viewSelectorExpanded = false;
        screen.moreTabsExpanded = false;
        PersonalDatabaseScreenTabHelper.closeTopTabPrompt(screen);
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
        guiGraphics.fill(
                panelRect.x() + 8,
                panelRect.y() + 8 + PersonalDatabaseScreen.OVERLAY_SECTION_TITLE_HEIGHT,
                panelRect.right() - 8,
                panelRect.y() + 9 + PersonalDatabaseScreen.OVERLAY_SECTION_TITLE_HEIGHT,
                0x70A89E8C
        );
        PersonalDatabaseScreenOverlayRenderHelper.renderOverlayCloseButton(screen, guiGraphics, panelRect, mouseX, mouseY);

        List<PersonalDatabaseTargetSelectorModel.Row> candidateRows = targetSelectorRows(screen);
        List<PersonalDatabaseTargetSelectorModel.Row> visibleRows = visibleTargetSelectorRows(screen, candidateRows);
        if (candidateRows.isEmpty()) {
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

        DatabaseAutoStoreTarget selectedTarget = screen.targetSelectorMode == PersonalDatabaseScreen.TargetSelectorMode.AUTO_STORE_TARGET
                ? screen.databaseMenu.viewState().autoStoreTarget()
                : null;
        PersonalDatabaseLayout.Rect bodyRect = selectorBodyRect(panelRect);
        PersonalDatabaseScreenListHelper.VisibleRange visibleRange = PersonalDatabaseScreenListHelper.visibleRange(
                candidateRows.size(),
                screen.targetSelectorScrollIndex,
                PersonalDatabaseScreenListHelper.maxVisibleRows(bodyRect, PersonalDatabaseScreen.TARGET_SELECTOR_ROW_HEIGHT)
        );
        screen.targetSelectorScrollIndex = visibleRange.scrollIndex();
        PersonalDatabaseScreenListHelper.enableScissor(guiGraphics, bodyRect);
        for (int index = 0; index < visibleRows.size(); index++) {
            PersonalDatabaseTargetSelectorModel.Row row = visibleRows.get(index);
            PersonalDatabaseLayout.Rect rowRect = PersonalDatabaseScreenGeometry.selectorRowRect(
                    panelRect,
                    index,
                    PersonalDatabaseScreen.TARGET_SELECTOR_ROW_HEIGHT
            );
            if (row.type() == PersonalDatabaseTargetSelectorModel.RowType.HEADER) {
                renderTargetSelectorGroupHeader(screen, guiGraphics, rowRect, row);
                continue;
            }
            renderTargetSelectorTargetRow(
                    screen,
                    guiGraphics,
                    rowRect,
                    row,
                    rowRect.contains(mouseX, mouseY),
                    isSelectedAutoStoreTarget(selectedTarget, row)
            );
        }
        guiGraphics.disableScissor();
        PersonalDatabaseScreenListHelper.renderScrollIndicators(screen, guiGraphics, bodyRect, visibleRange);
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
        List<PersonalDatabaseTargetSelectorModel.Row> candidateRows = targetSelectorRows(screen);
        List<PersonalDatabaseTargetSelectorModel.Row> visibleRows = visibleTargetSelectorRows(screen, candidateRows);
        for (int index = 0; index < visibleRows.size(); index++) {
            PersonalDatabaseLayout.Rect rowRect = PersonalDatabaseScreenGeometry.selectorRowRect(
                    panelRect,
                    index,
                    PersonalDatabaseScreen.TARGET_SELECTOR_ROW_HEIGHT
            );
            if (!rowRect.contains(mouseX, mouseY)) {
                continue;
            }
            PersonalDatabaseTargetSelectorModel.TargetSelection targetSelection = visibleRows.get(index).targetSelection();
            if (targetSelection != null) {
                applyTargetSelection(screen, targetSelection);
            }
            return true;
        }
        return true;
    }

    static boolean scrollTargetSelector(PersonalDatabaseScreen screen, int deltaRows) {
        if (!screen.targetSelectorExpanded || deltaRows == 0) {
            return false;
        }
        List<PersonalDatabaseTargetSelectorModel.Row> candidateTabs = targetSelectorRows(screen);
        int maxVisibleRows = maxVisibleRows(screen);
        int maxScrollIndex = Math.max(0, candidateTabs.size() - maxVisibleRows);
        int nextScrollIndex = Math.max(0, Math.min(maxScrollIndex, screen.targetSelectorScrollIndex + deltaRows));
        if (nextScrollIndex == screen.targetSelectorScrollIndex) {
            return false;
        }
        screen.targetSelectorScrollIndex = nextScrollIndex;
        return true;
    }

    static List<PersonalDatabaseTargetSelectorModel.Row> targetSelectorRows(PersonalDatabaseScreen screen) {
        return PersonalDatabaseTargetSelectorModel.buildRows(
                screen.targetSelectorMode,
                screen.databaseMenu.viewState(),
                screen.pendingTargetSourceScope,
                screen.pendingTargetSourceTabId,
                PersonalDatabaseScreenCommonHelper.currentPanels(screen)
        );
    }

    static Component targetSelectorTitle(PersonalDatabaseScreen screen) {
        return switch (screen.targetSelectorMode) {
            case DEPOSIT_ALL -> Component.translatable("screen.infiniteinventory.target_selector.deposit_all");
            case CARRIED_STORE -> Component.translatable("screen.infiniteinventory.target_selector.store");
            case QUICK_DEPOSIT -> Component.translatable("screen.infiniteinventory.target_selector.quick_deposit");
            case TRANSFER_TAB -> Component.translatable("screen.infiniteinventory.target_selector.transfer");
            case TRANSFER_SELECTION -> Component.translatable("screen.infiniteinventory.target_selector.selection_transfer");
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
        DatabaseScopedTabRef directTarget = PersonalDatabaseScreenCommonHelper.resolveSingleStoreTarget(screen);
        if (directTarget != null) {
            PacketDistributor.sendToServer(new DatabaseQuickDepositPayload(
                    screen.databaseMenu.containerId,
                    screen.databaseMenu.viewState().sessionId(),
                    slotIndex,
                    directTarget.scope(),
                    directTarget.tabId()
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

    private static void applyTargetSelection(
            PersonalDatabaseScreen screen,
            PersonalDatabaseTargetSelectorModel.TargetSelection targetSelection
    ) {
        String targetTabId = targetSelection.tabId();
        switch (screen.targetSelectorMode) {
            case DEPOSIT_ALL -> PacketDistributor.sendToServer(new DepositAllPayload(
                    screen.databaseMenu.containerId,
                    screen.databaseMenu.viewState().sessionId(),
                    targetSelection.scope(),
                    targetTabId
            ));
            case CARRIED_STORE -> PersonalDatabaseScreenLayoutHelper.sendDatabaseClick(
                    screen,
                    Math.max(0, screen.pendingTargetPanelIndex),
                    0,
                    screen.pendingTargetStoresSingle ? DatabaseClickAction.STORE_SINGLE : DatabaseClickAction.STORE_STACK,
                    targetSelection.scope(),
                    targetTabId
            );
            case QUICK_DEPOSIT -> PacketDistributor.sendToServer(new DatabaseQuickDepositPayload(
                    screen.databaseMenu.containerId,
                    screen.databaseMenu.viewState().sessionId(),
                    screen.pendingQuickDepositSlotIndex,
                    targetSelection.scope(),
                    targetTabId
            ));
            case TRANSFER_TAB -> PersonalDatabaseScreenManagementHelper.sendTabMutation(
                    screen,
                    screen.pendingTargetSourceScope,
                    DatabaseTabMutationAction.TRANSFER,
                    screen.pendingTargetSourceTabId,
                    targetSelection.scope(),
                    targetTabId,
                    "",
                    ""
            );
            case TRANSFER_SELECTION -> PersonalDatabaseScreenSelectionHelper.sendSelectionAction(
                    screen,
                    DatabaseSelectionAction.TRANSFER_TO_TAB,
                    targetSelection.scope(),
                    targetTabId,
                    0L
            );
            case DELETE_TAB -> PersonalDatabaseScreenManagementHelper.sendTabMutation(
                    screen,
                    screen.pendingTargetSourceScope,
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
                    new DatabaseAutoStoreTarget(targetSelection.scope(), targetTabId)
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

    private static void renderTargetSelectorGroupHeader(
            PersonalDatabaseScreen screen,
            GuiGraphics guiGraphics,
            PersonalDatabaseLayout.Rect rowRect,
            PersonalDatabaseTargetSelectorModel.Row row
    ) {
        Component label = screen.targetSelectorMode == PersonalDatabaseScreen.TargetSelectorMode.AUTO_STORE_TARGET
                || screen.targetSelectorMode == PersonalDatabaseScreen.TargetSelectorMode.DEPOSIT_ALL
                || screen.targetSelectorMode == PersonalDatabaseScreen.TargetSelectorMode.CARRIED_STORE
                || screen.targetSelectorMode == PersonalDatabaseScreen.TargetSelectorMode.QUICK_DEPOSIT
                ? Component.translatable(
                        "screen.infiniteinventory.target_selector.group.target_scope",
                        Component.translatable(row.scope().translationKey())
                )
                : screen.targetSelectorMode == PersonalDatabaseScreen.TargetSelectorMode.TRANSFER_TAB
                        || screen.targetSelectorMode == PersonalDatabaseScreen.TargetSelectorMode.TRANSFER_SELECTION
                ? row.sourceScopeGroup()
                        ? Component.translatable(
                                "screen.infiniteinventory.target_selector.group.source_scope",
                                Component.translatable(row.scope().translationKey())
                        )
                        : Component.translatable(
                                "screen.infiniteinventory.target_selector.group.other_scope",
                                Component.translatable(row.scope().translationKey())
                        )
                : Component.translatable(
                        "screen.infiniteinventory.target_selector.group.target_scope",
                        Component.translatable(row.scope().translationKey())
                );
        int textX = rowRect.x() + 2;
        int textY = rowRect.y() + 6;
        guiGraphics.drawString(
                screen.screenFont(),
                label,
                textX,
                textY,
                PersonalDatabaseScreen.OVERLAY_MUTED_TEXT_COLOR,
                false
        );
        int dividerX = Math.min(rowRect.right() - 2, textX + screen.screenFont().width(label) + 6);
        if (dividerX < rowRect.right() - 2) {
            guiGraphics.fill(dividerX, rowRect.y() + 10, rowRect.right() - 2, rowRect.y() + 11, 0x70A89E8C);
        }
    }

    private static void renderTargetSelectorTargetRow(
            PersonalDatabaseScreen screen,
            GuiGraphics guiGraphics,
            PersonalDatabaseLayout.Rect rowRect,
            PersonalDatabaseTargetSelectorModel.Row row,
            boolean hovered,
            boolean selected
    ) {
        VanillaWidgetRenderer.renderOverlayRow(guiGraphics, rowRect, hovered, selected);
        DatabaseTab tab = row.tab();
        guiGraphics.renderItem(PersonalDatabaseScreenCommonHelper.tabIcon(screen, tab), rowRect.x() + 3, rowRect.y() + 2);
        int textRight = rowRect.right() - 4;
        if (usesGroupedTransferRows(screen)) {
            Component scopeLabel = Component.translatable(row.scope().translationKey());
            int scopeLabelWidth = screen.screenFont().width(scopeLabel);
            int scopeLabelX = Math.max(rowRect.x() + 32, rowRect.right() - 6 - scopeLabelWidth);
            guiGraphics.drawString(
                    screen.screenFont(),
                    scopeLabel,
                    scopeLabelX,
                    rowRect.y() + 6,
                    PersonalDatabaseScreen.OVERLAY_MUTED_TEXT_COLOR,
                    false
            );
            textRight = Math.max(rowRect.x() + 24, scopeLabelX - 8);
        }
        guiGraphics.drawString(
                screen.screenFont(),
                PersonalDatabaseScreenGeometry.truncateToWidth(
                        screen,
                        PersonalDatabaseScreenCommonHelper.tabLabel(screen, tab).getString(),
                        Math.max(0, textRight - (rowRect.x() + 24))
                ),
                rowRect.x() + 24,
                rowRect.y() + 6,
                selected ? PersonalDatabaseScreen.OVERLAY_ACCENT_TEXT_COLOR : PersonalDatabaseScreen.OVERLAY_TEXT_COLOR,
                false
        );
    }

    private static boolean usesGroupedTransferRows(PersonalDatabaseScreen screen) {
        return screen.targetSelectorMode == PersonalDatabaseScreen.TargetSelectorMode.TRANSFER_TAB
                || screen.targetSelectorMode == PersonalDatabaseScreen.TargetSelectorMode.TRANSFER_SELECTION;
    }

    private static boolean isSelectedAutoStoreTarget(
            DatabaseAutoStoreTarget selectedTarget,
            PersonalDatabaseTargetSelectorModel.Row row
    ) {
        return selectedTarget != null
                && row.scope() != null
                && row.tab() != null
                && selectedTarget.scope() == row.scope()
                && selectedTarget.tabId().equals(row.tab().id());
    }

    private static List<PersonalDatabaseTargetSelectorModel.Row> visibleTargetSelectorRows(
            PersonalDatabaseScreen screen,
            List<PersonalDatabaseTargetSelectorModel.Row> candidateTabs
    ) {
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
        return PersonalDatabaseScreenListHelper.maxVisibleRows(
                selectorBodyRect(panelRect),
                PersonalDatabaseScreen.TARGET_SELECTOR_ROW_HEIGHT
        );
    }

    private static PersonalDatabaseLayout.Rect selectorBodyRect(PersonalDatabaseLayout.Rect panelRect) {
        int contentTop = panelRect.y()
                + PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING
                + PersonalDatabaseScreen.OVERLAY_SECTION_TITLE_HEIGHT
                + 8;
        int contentBottom = panelRect.bottom() - PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING;
        return new PersonalDatabaseLayout.Rect(
                panelRect.x() + PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING,
                contentTop,
                Math.max(0, panelRect.width() - PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING * 2),
                Math.max(0, contentBottom - contentTop)
        );
    }
}
