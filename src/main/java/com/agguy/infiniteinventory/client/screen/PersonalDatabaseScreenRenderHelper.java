package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.database.DatabasePanelView;
import com.agguy.infiniteinventory.database.DatabaseTab;
import com.agguy.infiniteinventory.database.VisibleDatabaseEntry;
import com.agguy.infiniteinventory.menu.PersonalDatabaseLayout;
import com.agguy.infiniteinventory.util.CompactNumberFormatter;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

final class PersonalDatabaseScreenRenderHelper {
    private PersonalDatabaseScreenRenderHelper() {
    }

    static void renderBg(PersonalDatabaseScreen screen, GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        if (screen.layout == null) {
            return;
        }
        VanillaWidgetRenderer.renderPanel(guiGraphics, screen.layout.frameRect());
        PersonalDatabaseScreenTabHelper.renderTabs(screen, guiGraphics, mouseX, mouseY);
        VanillaWidgetRenderer.renderTextField(
                guiGraphics,
                screen.layout.searchFieldRect(),
                screen.searchBox != null && screen.searchBox.isFocused()
        );
        renderDatabaseScaffold(screen, guiGraphics);

        if (screen.minecraftClient() != null && screen.minecraftClient().player != null) {
            screen.inventoryPaneProvider.renderEquipmentPanel(
                    guiGraphics,
                    screen.minecraftClient().player,
                    screen.layout.equipmentPanelRect().x(),
                    screen.layout.equipmentPanelRect().y(),
                    mouseX,
                    mouseY
            );
        }
        screen.inventoryPaneProvider.renderBottomInventory(
                guiGraphics,
                screen.layout.bottomInventoryRect().x(),
                screen.layout.bottomInventoryRect().y()
        );
        if (screen.accessoriesExpanded) {
            renderAccessoriesPanel(screen, guiGraphics, mouseX, mouseY);
        }

        renderDatabaseSlots(screen, guiGraphics);
        renderDatabaseEntries(screen, guiGraphics, mouseX, mouseY);
        renderEmptyState(screen, guiGraphics);
        renderFrameText(screen, guiGraphics);
    }

    static void renderSlot(PersonalDatabaseScreen screen, GuiGraphics guiGraphics, Slot slot) {
        PersonalDatabaseLayout.AccessorySlotLayout accessorySlotLayout = PersonalDatabaseScreenGeometry.resolveAccessorySlotLayout(
                screen,
                slot
        );
        if (accessorySlotLayout != null && accessorySlotLayout.visible()) {
            PersonalDatabaseLayout.Rect slotRect = accessorySlotLayout.slotRect();
            VanillaWidgetRenderer.renderMenuSlot(guiGraphics, slotRect.x(), slotRect.y());
        }
    }

    static boolean shouldSkipSlotHighlight(PersonalDatabaseScreen screen, Slot slot) {
        return PersonalDatabaseScreenGeometry.resolveAccessorySlotLayout(screen, slot) != null;
    }

    static void renderAccessoriesPanel(PersonalDatabaseScreen screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (screen.layout == null || screen.layout.accessoriesPanelRect().height() <= 0) {
            return;
        }
        PersonalDatabaseLayout.Rect panelRect = screen.layout.accessoriesPanelRect();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 220.0F);
        VanillaWidgetRenderer.renderOverlayPanel(guiGraphics, panelRect);
        guiGraphics.drawString(
                screen.screenFont(),
                Component.translatable("screen.infiniteinventory.accessories_panel"),
                panelRect.x() + PersonalDatabaseLayout.ACCESSORY_DRAWER_PADDING,
                panelRect.y() + PersonalDatabaseLayout.ACCESSORY_DRAWER_PADDING,
                PersonalDatabaseScreen.OVERLAY_TEXT_COLOR,
                true
        );
        guiGraphics.fill(
                panelRect.x() + PersonalDatabaseLayout.ACCESSORY_DRAWER_PADDING,
                panelRect.y() + PersonalDatabaseLayout.ACCESSORY_DRAWER_PADDING + PersonalDatabaseLayout.ACCESSORY_DRAWER_TITLE_HEIGHT + 1,
                panelRect.right() - PersonalDatabaseLayout.ACCESSORY_DRAWER_PADDING,
                panelRect.y() + PersonalDatabaseLayout.ACCESSORY_DRAWER_PADDING + PersonalDatabaseLayout.ACCESSORY_DRAWER_TITLE_HEIGHT + 2,
                0x70A89E8C
        );
        guiGraphics.pose().popPose();
    }

    static void renderAccessorySlotHover(PersonalDatabaseScreen screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
        PersonalDatabaseLayout.AccessorySlotLayout hoveredSlot = PersonalDatabaseScreenGeometry.findHoveredAccessorySlot(
                screen,
                mouseX,
                mouseY
        );
        if (hoveredSlot == null) {
            return;
        }
        PersonalDatabaseLayout.Rect slotRect = hoveredSlot.slotRect();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 230.0F);
        guiGraphics.fill(slotRect.x(), slotRect.y(), slotRect.x() + 16, slotRect.y() + 16, 0x52000000);
        guiGraphics.pose().popPose();
    }

    static void renderDatabaseEntries(PersonalDatabaseScreen screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (screen.layout == null) {
            return;
        }
        for (int panelIndex = 0; panelIndex < PersonalDatabaseScreenCommonHelper.currentPanels(screen).size(); panelIndex++) {
            DatabasePanelView panel = PersonalDatabaseScreenCommonHelper.currentPanels(screen).get(panelIndex);
            PersonalDatabaseLayout.DatabaseViewportLayout viewportLayout = screen.layout.databaseViewportLayout(panelIndex);
            if (viewportLayout.panelRect().contains(mouseX, mouseY)) {
                guiGraphics.fill(
                        viewportLayout.panelRect().x(),
                        viewportLayout.panelRect().y(),
                        viewportLayout.panelRect().right(),
                        viewportLayout.panelRect().bottom(),
                        0x12000000
                );
            }
            for (int slotIndex = 0; slotIndex < screen.layout.visibleDatabaseSlotCount(panelIndex); slotIndex++) {
                PersonalDatabaseLayout.Rect slotRect = screen.layout.visibleDatabaseSlotBounds(panelIndex, slotIndex);
                if (slotRect.contains(mouseX, mouseY)) {
                    VanillaWidgetRenderer.renderSlotHighlight(guiGraphics, slotRect);
                }
                if (slotIndex < panel.entries().size()) {
                    VisibleDatabaseEntry entry = panel.entries().get(slotIndex);
                    ItemStack stack = entry.stack();
                    int itemX = slotRect.x() + (PersonalDatabaseLayout.DATABASE_SLOT_SIZE - 16) / 2;
                    int itemY = slotRect.y() + (PersonalDatabaseLayout.DATABASE_SLOT_SIZE - 16) / 2;
                    guiGraphics.renderItem(stack, itemX, itemY);
                    guiGraphics.renderItemDecorations(
                            screen.screenFont(),
                            stack,
                            itemX,
                            itemY,
                            CompactNumberFormatter.format(entry.amount())
                    );
                }
            }
        }
    }

    static void renderEmptyState(PersonalDatabaseScreen screen, GuiGraphics guiGraphics) {
        if (screen.layout == null) {
            return;
        }
        for (int panelIndex = 0; panelIndex < PersonalDatabaseScreenCommonHelper.currentPanels(screen).size(); panelIndex++) {
            DatabasePanelView panel = PersonalDatabaseScreenCommonHelper.currentPanels(screen).get(panelIndex);
            if (!panel.entries().isEmpty()) {
                continue;
            }
            var query = screen.databaseMenu.viewState().query();
            Component message = Component.translatable(query.searchText().isEmpty()
                    ? query.scope().emptyTranslationKey()
                    : "screen.infiniteinventory.no_results");
            PersonalDatabaseLayout.Rect gridRect = screen.layout.databaseViewportLayout(panelIndex).gridRect();
            int y = gridRect.y() + Math.max(0, gridRect.height() / 2 - 4);
            PersonalDatabaseScreenCommonHelper.drawCenteredShadow(
                    screen,
                    guiGraphics,
                    message,
                    gridRect.x(),
                    gridRect.right(),
                    y,
                    0x7A7A7A
            );
        }
    }

    static void renderFrameText(PersonalDatabaseScreen screen, GuiGraphics guiGraphics) {
        if (screen.layout == null) {
            return;
        }
        var viewState = screen.databaseMenu.viewState();
        guiGraphics.drawString(
                screen.screenFont(),
                screen.databaseScreenTitle,
                screen.layout.titleRect().x(),
                screen.layout.titleRect().y() + 6,
                0x404040,
                true
        );

        for (int panelIndex = 0; panelIndex < PersonalDatabaseScreenCommonHelper.currentPanels(screen).size(); panelIndex++) {
            DatabasePanelView panel = PersonalDatabaseScreenCommonHelper.currentPanels(screen).get(panelIndex);
            PersonalDatabaseLayout.DatabaseViewportLayout viewportLayout = screen.layout.databaseViewportLayout(panelIndex);
            guiGraphics.drawString(
                    screen.screenFont(),
                    PersonalDatabaseScreenCommonHelper.tabLabel(screen, panel.tab()),
                    viewportLayout.headerRect().x(),
                    viewportLayout.headerRect().y() + 4,
                    viewState.query().focusedTabId().equals(panel.tab().id())
                            ? 0x404040
                            : PersonalDatabaseScreen.OVERLAY_TEXT_COLOR,
                    true
            );
        }

        Component footerStats = Component.translatable(
                "screen.infiniteinventory.footer_stats",
                Component.translatable(viewState.query().scope().translationKey()),
                viewState.totalEntries(),
                CompactNumberFormatter.format(viewState.totalItems())
        );
        guiGraphics.drawString(
                screen.screenFont(),
                footerStats,
                screen.layout.databaseFooterRect().x(),
                screen.layout.databaseFooterRect().y() + 6,
                0x404040,
                true
        );

        Component pageLabel = Component.translatable(
                "screen.infiniteinventory.page_compact",
                viewState.query().pageIndex() + 1,
                viewState.totalPages()
        );
        PersonalDatabaseScreenCommonHelper.drawCenteredShadow(
                screen,
                guiGraphics,
                pageLabel,
                screen.layout.pageLabelRect().x(),
                screen.layout.pageLabelRect().right(),
                screen.layout.pageLabelRect().y() + 6,
                0x404040
        );
    }

    static void renderSearchHint(PersonalDatabaseScreen screen, GuiGraphics guiGraphics) {
        if (screen.layout == null
                || screen.searchBox == null
                || screen.searchBox.isFocused()
                || !screen.searchBox.getValue().isEmpty()) {
            return;
        }
        guiGraphics.drawString(
                screen.screenFont(),
                Component.translatable("screen.infiniteinventory.search_hint"),
                screen.layout.searchFieldRect().x() + PersonalDatabaseScreen.SEARCH_TEXT_LEFT_PADDING,
                screen.layout.searchFieldRect().y() + 6,
                0x777777,
                false
        );
    }

    static void renderScreenTooltips(PersonalDatabaseScreen screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (screen.contextMenuExpanded
                || screen.sortDropdownExpanded
                || screen.pagePickerExpanded
                || screen.advancedSearchExpanded
                || screen.enhancementPanelExpanded
                || screen.viewSelectorExpanded
                || screen.moreTabsExpanded
                || screen.tabManagementExpanded
                || screen.iconPickerExpanded
                || screen.targetSelectorExpanded) {
            return;
        }
        screen.invokeSuperRenderTooltip(guiGraphics, mouseX, mouseY);
        PersonalDatabaseLayout.AccessorySlotLayout accessorySlotLayout = PersonalDatabaseScreenGeometry.findHoveredAccessorySlot(
                screen,
                mouseX,
                mouseY
        );
        if (accessorySlotLayout != null) {
            ItemStack hoveredStack = accessorySlotLayout.slotIndex() >= 0
                    && accessorySlotLayout.slotIndex() < screen.databaseMenu.slots.size()
                    ? screen.databaseMenu.getSlot(accessorySlotLayout.slotIndex()).getItem()
                    : ItemStack.EMPTY;
            if (hoveredStack.isEmpty()) {
                Component slotLabel = I18n.exists(accessorySlotLayout.group().translationKey())
                        ? Component.translatable(accessorySlotLayout.group().translationKey())
                        : Component.literal(accessorySlotLayout.group().slotName());
                guiGraphics.renderTooltip(
                        screen.screenFont(),
                        List.of(slotLabel),
                        ItemStack.EMPTY.getTooltipImage(),
                        mouseX,
                        mouseY
                );
            }
            return;
        }
        PersonalDatabaseScreen.DatabaseHitResult hitResult = PersonalDatabaseScreenGeometry.findDatabaseSlot(
                screen,
                mouseX,
                mouseY
        );
        if (hitResult != null && hitResult.panelIndex() < PersonalDatabaseScreenCommonHelper.currentPanels(screen).size()) {
            DatabasePanelView panel = PersonalDatabaseScreenCommonHelper.currentPanels(screen).get(hitResult.panelIndex());
            if (hitResult.slotIndex() < panel.entries().size()) {
                VisibleDatabaseEntry entry = panel.entries().get(hitResult.slotIndex());
                List<Component> tooltip = new ArrayList<>(screen.containerTooltip(entry.stack()));
                tooltip.add(Component.translatable(
                        "screen.infiniteinventory.tooltip.amount",
                        CompactNumberFormatter.format(entry.amount())
                ).withStyle(ChatFormatting.GRAY));
                tooltip.add(PersonalDatabaseScreenCommonHelper.tabLabel(
                        screen,
                        PersonalDatabaseScreenCommonHelper.findTab(screen, entry.tabId())
                ).copy().withStyle(ChatFormatting.BLUE));
                tooltip.add(Component.literal(entry.registryName()).withStyle(ChatFormatting.DARK_GRAY));
                guiGraphics.renderTooltip(screen.screenFont(), tooltip, entry.stack().getTooltipImage(), mouseX, mouseY);
                return;
            }
        }
        DatabaseTab hoveredTab = findHoveredTab(screen, mouseX, mouseY);
        if (hoveredTab != null) {
            guiGraphics.renderTooltip(
                    screen.screenFont(),
                    List.of(PersonalDatabaseScreenCommonHelper.tabLabel(screen, hoveredTab)),
                    ItemStack.EMPTY.getTooltipImage(),
                    mouseX,
                    mouseY
            );
        }
    }

    static void renderToolbarOverlays(PersonalDatabaseScreen screen, GuiGraphics guiGraphics) {
        if (screen.layout == null) {
            return;
        }
        PersonalDatabaseLayout.Rect searchRect = screen.layout.searchFieldRect();
        int iconX = searchRect.x() + 6;
        int iconY = searchRect.y() + (searchRect.height() - PersonalDatabaseScreen.SEARCH_ICON_SIZE) / 2;
        VanillaWidgetRenderer.renderSearchGlyph(guiGraphics, iconX, iconY, 0xFF6D6D6D);

        PersonalDatabaseLayout.Rect sortRect = screen.layout.sortButtonRect();
        VanillaWidgetRenderer.renderDropdownIndicator(
                guiGraphics,
                sortRect.right() - 10,
                sortRect.y() + sortRect.height() / 2,
                0xFF3F3F3F
        );

        PersonalDatabaseLayout.Rect pageRect = screen.layout.pageLabelRect();
        VanillaWidgetRenderer.renderDropdownIndicator(
                guiGraphics,
                pageRect.right() - 10,
                pageRect.y() + pageRect.height() / 2,
                0xFF3F3F3F
        );

        if (screen.advancedSearchButton != null && screen.layout.advancedSearchButtonRect().width() > 0) {
            PersonalDatabaseLayout.Rect advancedRect = screen.layout.advancedSearchButtonRect();
            VanillaWidgetRenderer.renderDropdownIndicator(
                    guiGraphics,
                    advancedRect.right() - 10,
                    advancedRect.y() + advancedRect.height() / 2,
                    0xFF3F3F3F
            );
        }
        if (screen.enhancementButton != null && screen.layout.enhancementButtonRect().width() > 0) {
            PersonalDatabaseLayout.Rect enhancementRect = screen.layout.enhancementButtonRect();
            VanillaWidgetRenderer.renderDropdownIndicator(
                    guiGraphics,
                    enhancementRect.right() - 10,
                    enhancementRect.y() + enhancementRect.height() / 2,
                    0xFF3F3F3F
            );
        }
        if (screen.viewSelectorButton != null && screen.layout.viewSelectorButtonRect().width() > 0) {
            PersonalDatabaseLayout.Rect viewRect = screen.layout.viewSelectorButtonRect();
            VanillaWidgetRenderer.renderDropdownIndicator(
                    guiGraphics,
                    viewRect.right() - 10,
                    viewRect.y() + viewRect.height() / 2,
                    0xFF3F3F3F
            );
        }
    }

    static void renderDatabaseScaffold(PersonalDatabaseScreen screen, GuiGraphics guiGraphics) {
        if (screen.layout == null) {
            return;
        }
        VanillaWidgetRenderer.renderPanel(guiGraphics, screen.layout.databasePanelRect());
        for (int panelIndex = 0; panelIndex < PersonalDatabaseScreenCommonHelper.currentPanels(screen).size(); panelIndex++) {
            PersonalDatabaseLayout.DatabaseViewportLayout viewportLayout = screen.layout.databaseViewportLayout(panelIndex);
            VanillaWidgetRenderer.renderPanel(guiGraphics, viewportLayout.panelRect());
        }
        guiGraphics.fill(
                screen.layout.databaseFooterRect().x(),
                screen.layout.databaseFooterRect().y() - 6,
                screen.layout.databaseFooterRect().right(),
                screen.layout.databaseFooterRect().y() - 5,
                0x66FFFFFF
        );
        guiGraphics.drawString(
                screen.screenFont(),
                Component.translatable(screen.databaseMenu.viewState().query().scope().sectionTranslationKey()),
                screen.layout.databasePanelRect().x() + PersonalDatabaseLayout.GRID_PADDING,
                screen.layout.databasePanelRect().y() - 12,
                0x404040,
                true
        );
    }

    static void renderDatabaseSlots(PersonalDatabaseScreen screen, GuiGraphics guiGraphics) {
        if (screen.layout == null) {
            return;
        }
        for (int panelIndex = 0; panelIndex < PersonalDatabaseScreenCommonHelper.currentPanels(screen).size(); panelIndex++) {
            int visibleSlotCount = screen.layout.visibleDatabaseSlotCount(panelIndex);
            for (int slotIndex = 0; slotIndex < visibleSlotCount; slotIndex++) {
                PersonalDatabaseLayout.Rect slotRect = screen.layout.visibleDatabaseSlotBounds(panelIndex, slotIndex);
                VanillaWidgetRenderer.renderSlot(guiGraphics, slotRect.x(), slotRect.y());
            }
        }
    }

    private static DatabaseTab findHoveredTab(PersonalDatabaseScreen screen, double mouseX, double mouseY) {
        List<DatabaseTab> visibleTabs = PersonalDatabaseScreenTabHelper.visibleTopTabs(screen);
        for (int index = 0; index < visibleTabs.size(); index++) {
            if (PersonalDatabaseScreenTabHelper.topTabRect(
                    screen,
                    index,
                    visibleTabs.size(),
                    !PersonalDatabaseScreenTabHelper.hiddenTopTabs(screen).isEmpty()
            ).contains(mouseX, mouseY)) {
                return visibleTabs.get(index);
            }
        }
        return null;
    }
}
