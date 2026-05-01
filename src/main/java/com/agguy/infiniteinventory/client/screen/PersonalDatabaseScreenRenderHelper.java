package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.client.DatabaseAmountCache;
import com.agguy.infiniteinventory.database.DatabasePanelView;
import com.agguy.infiniteinventory.database.DatabaseScopedTabRef;
import com.agguy.infiniteinventory.database.DatabaseSortDirection;
import com.agguy.infiniteinventory.database.DatabaseSortOption;
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
    private static final int UNSUPPORTED_NOTICE_WIDTH = 320;
    private static final int UNSUPPORTED_NOTICE_HEIGHT = 126;

    private PersonalDatabaseScreenRenderHelper() {
    }

    static void renderUnsupportedScreen(PersonalDatabaseScreen screen, GuiGraphics guiGraphics) {
        PersonalDatabaseLayout.Rect panelRect = PersonalDatabaseScreenGeometry.centeredOverlayRect(
                screen,
                UNSUPPORTED_NOTICE_WIDTH,
                UNSUPPORTED_NOTICE_HEIGHT
        );
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 260.0F);
        VanillaWidgetRenderer.renderOverlayPanel(guiGraphics, panelRect);
        guiGraphics.drawString(
                screen.screenFont(),
                Component.translatable("screen.infiniteinventory.window_too_small.title"),
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
        guiGraphics.drawWordWrap(
                screen.screenFont(),
                Component.translatable("screen.infiniteinventory.window_too_small.description"),
                panelRect.x() + 8,
                panelRect.y() + 40,
                Math.max(1, panelRect.width() - 16),
                PersonalDatabaseScreen.OVERLAY_MUTED_TEXT_COLOR
        );
        guiGraphics.pose().popPose();
    }
    static void renderBg(PersonalDatabaseScreen screen, GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        if (screen.layout == null) {
            return;
        }
        VanillaWidgetRenderer.renderPanel(guiGraphics, screen.layout.frameRect());
        PersonalDatabaseScreenTabHelper.renderTabs(screen, guiGraphics, mouseX, mouseY);
        renderDatabaseScaffold(screen, guiGraphics);
        renderPanelTextFields(screen, guiGraphics);

        var mc = screen.minecraftClient();
        if (mc != null && mc.player != null) {
            screen.inventoryPaneProvider.renderEquipmentPanel(guiGraphics, mc.player,
                    screen.layout.equipmentPanelRect().x(), screen.layout.equipmentPanelRect().y(), mouseX, mouseY);
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
            guiGraphics.fill(slotRect.x() - 1, slotRect.y() - 1, slotRect.right() + 1, slotRect.y(), 0xB08A7A60);
            guiGraphics.fill(slotRect.x() - 1, slotRect.bottom(), slotRect.right() + 1, slotRect.bottom() + 1, 0x90382E24);
            guiGraphics.fill(slotRect.x() - 1, slotRect.y(), slotRect.x(), slotRect.bottom(), 0xB08A7A60);
            guiGraphics.fill(slotRect.right(), slotRect.y(), slotRect.right() + 1, slotRect.bottom(), 0x90382E24);
        }
    }
    static boolean shouldSkipSlotHighlight(PersonalDatabaseScreen screen, Slot slot) {
        return PersonalDatabaseScreenGeometry.resolveAccessorySlotLayout(screen, slot) != null;
    }
    static void renderAccessoriesPanel(PersonalDatabaseScreen screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
        PersonalDatabaseScreenAccessoryPanelHelper.renderAccessoriesPanel(screen, guiGraphics);
    }
    static void renderAccessorySlotHover(PersonalDatabaseScreen screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
        PersonalDatabaseScreenAccessoryPanelHelper.renderAccessorySlotHover(screen, guiGraphics, mouseX, mouseY);
    }
    static void renderDatabaseEntries(PersonalDatabaseScreen screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (screen.layout == null) {
            return;
        }
        if (screen.settingsPanelExpanded) {
            return;
        }
        PersonalDatabaseLayout.Rect logPanelRect = screen.logPanelExpanded
                ? PersonalDatabaseScreenLogGeometry.logPanelRect(screen)
                : PersonalDatabaseLayout.Rect.empty();
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
                if (logPanelRect.intersects(slotRect)) {
                    continue;
                }
                if (slotIndex < panel.entries().size()) {
                    VisibleDatabaseEntry entry = panel.entries().get(slotIndex);
                    if (PersonalDatabaseScreenSelectionHelper.isSelected(screen, entry)) {
                        VanillaWidgetRenderer.renderSlotSelection(guiGraphics, slotRect);
                    }
                    if (slotRect.contains(mouseX, mouseY)) {
                        VanillaWidgetRenderer.renderSlotHighlight(guiGraphics, slotRect);
                    }
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
                    if (entry.starred()) {
                        int sx = itemX - 1, sy = itemY - 1;
                        guiGraphics.fill(sx, sy, sx + 5, sy + 1, 0xFF8B6914);
                        guiGraphics.fill(sx, sy + 4, sx + 5, sy + 5, 0xFF8B6914);
                        guiGraphics.fill(sx, sy + 1, sx + 1, sy + 4, 0xFF8B6914);
                        guiGraphics.fill(sx + 4, sy + 1, sx + 5, sy + 4, 0xFF8B6914);
                        guiGraphics.fill(sx + 1, sy + 1, sx + 4, sy + 4, 0xFFFFD700);
                        guiGraphics.fill(sx + 1, sy + 1, sx + 2, sy + 2, 0xFFFFEC8B);
                    }
                } else if (slotRect.contains(mouseX, mouseY)) {
                    VanillaWidgetRenderer.renderSlotHighlight(guiGraphics, slotRect);
                }
            }
        }
    }
    static void renderEmptyState(PersonalDatabaseScreen screen, GuiGraphics guiGraphics) {
        if (screen.layout == null || screen.settingsPanelExpanded) {
            return;
        }
        for (int panelIndex = 0; panelIndex < PersonalDatabaseScreenCommonHelper.currentPanels(screen).size(); panelIndex++) {
            DatabasePanelView panel = PersonalDatabaseScreenCommonHelper.currentPanels(screen).get(panelIndex);
            if (!panel.entries().isEmpty()) {
                continue;
            }
            var query = screen.databaseMenu.viewState().query();
            Component message = Component.translatable(query.searchTextFor(panel.scopedTab()).isEmpty()
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
        renderFrameBadgeText(
                screen,
                guiGraphics,
                screen.databaseScreenTitle.getString(),
                screen.layout.titleRect().x(),
                screen.layout.titleRect().y() + 5,
                PersonalDatabaseScreen.FRAME_TEXT_COLOR
        );

        for (int panelIndex = 0; panelIndex < PersonalDatabaseScreenCommonHelper.currentPanels(screen).size(); panelIndex++) {
            DatabasePanelView panel = PersonalDatabaseScreenCommonHelper.currentPanels(screen).get(panelIndex);
            PersonalDatabaseLayout.DatabaseViewportLayout viewportLayout = screen.layout.databaseViewportLayout(panelIndex);
            PersonalDatabaseLayout.Rect titleRect = viewportLayout.headerRect();
            renderFrameBadgeText(
                    screen,
                    guiGraphics,
                    PersonalDatabaseScreenCommonHelper.truncateToWidth(
                            screen,
                            PersonalDatabaseScreenCommonHelper.viewTitleLabel(screen, panel.scopedTab()).getString(),
                            Math.max(0, titleRect.width() - 8)
                    ),
                    titleRect.x(),
                    titleRect.y(),
                    viewState.query().focusedTab().equals(panel.scopedTab())
                            ? PersonalDatabaseScreen.OVERLAY_ACCENT_TEXT_COLOR
                            : PersonalDatabaseScreen.OVERLAY_TEXT_COLOR
            );
        }

        if (screen.layout.toolbarRect().width() > 0) {
            String toolbarStats = Component.translatable(
                    "screen.infiniteinventory.total_entries",
                    CompactNumberFormatter.format(viewState.totalEntries())
            ).getString() + "   " + Component.translatable(
                    "screen.infiniteinventory.total_items",
                    CompactNumberFormatter.format(viewState.totalItems())
            ).getString();
            renderFrameBadgeText(
                    screen,
                    guiGraphics,
                    PersonalDatabaseScreenCommonHelper.truncateToWidth(
                            screen,
                            toolbarStats,
                            screen.layout.toolbarRect().width()
                    ),
                    screen.layout.toolbarRect().x(),
                    screen.layout.toolbarRect().y() + 5,
                    PersonalDatabaseScreen.FRAME_MUTED_TEXT_COLOR
            );
        }
    }
    static void renderSearchHint(PersonalDatabaseScreen screen, GuiGraphics guiGraphics) {
        if (screen.layout == null) {
            return;
        }
        for (int panelIndex = 0; panelIndex < screen.panelSearchBoxes.size(); panelIndex++) {
            if (panelIndex >= PersonalDatabaseScreenCommonHelper.currentPanels(screen).size()) {
                break;
            }
            var searchBox = screen.panelSearchBoxes.get(panelIndex);
            if (searchBox.isFocused() || !searchBox.getValue().isEmpty()) {
                continue;
            }
            PersonalDatabaseLayout.Rect searchRect = PersonalDatabaseScreenGeometry.panelSearchFieldRect(screen, panelIndex);
            guiGraphics.drawString(
                    screen.screenFont(),
                    Component.translatable("screen.infiniteinventory.search_hint"),
                    searchRect.x() + PersonalDatabaseScreen.SEARCH_TEXT_LEFT_PADDING,
                    searchRect.y() + 6,
                    PersonalDatabaseScreen.TEXT_FIELD_MUTED_TEXT_COLOR,
                    false
            );
        }
    }
    static void renderScreenTooltips(PersonalDatabaseScreen screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (screen.statisticsPanelExpanded || screen.logPanelExpanded
                || screen.customExtractOverlayExpanded || screen.contextMenuExpanded || screen.tabContextMenuExpanded
                || screen.sortDropdownExpanded || screen.pagePickerExpanded || screen.advancedSearchExpanded
                || screen.enhancementPanelExpanded || screen.viewSelectorExpanded || screen.moreTabsExpanded
                || screen.topTabActionPromptExpanded || screen.topTabReplaceExpanded || screen.tabManagementExpanded
                || screen.iconPickerExpanded || screen.targetSelectorExpanded) {
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
                if (entry.starred()) {
                    tooltip.add(Component.literal("★ ").withStyle(ChatFormatting.GOLD).append(Component.translatable("screen.infiniteinventory.tooltip.starred").withStyle(ChatFormatting.GOLD)));
                }
                if (!entry.note().isEmpty()) {
                    tooltip.add(Component.literal(entry.note()).withStyle(ChatFormatting.YELLOW));
                }
                tooltip.add(Component.translatable(
                        "screen.infiniteinventory.tooltip.amount",
                        CompactNumberFormatter.format(entry.amount())
                ).withStyle(ChatFormatting.GRAY));
                tooltip.add(PersonalDatabaseScreenCommonHelper.tabLabel(
                        screen,
                        PersonalDatabaseScreenCommonHelper.findTab(screen, entry.tabId())
                ).copy().withStyle(ChatFormatting.BLUE));
                tooltip.add(Component.literal(entry.registryName()).withStyle(ChatFormatting.DARK_GRAY));
                var pa = DatabaseAmountCache.INSTANCE.getPersonal(entry.stack());
                var pu = DatabaseAmountCache.INSTANCE.getPublic(entry.stack());
                if (pa != null) tooltip.add(Component.translatable("screen.infiniteinventory.amount.personal", pa.tabName(), CompactNumberFormatter.format(pa.amount())).withColor(0x55FFFF));
                if (pu != null) tooltip.add(Component.translatable("screen.infiniteinventory.amount.public", pu.tabName(), CompactNumberFormatter.format(pu.amount())).withColor(0xFFAA00));
                guiGraphics.renderTooltip(screen.screenFont(), tooltip, entry.stack().getTooltipImage(), mouseX, mouseY);
                return;
            }
        }
        DatabaseScopedTabRef hoveredTab = findHoveredTab(screen, mouseX, mouseY);
        if (hoveredTab != null) {
            guiGraphics.renderTooltip(
                    screen.screenFont(),
                    List.of(
                            PersonalDatabaseScreenCommonHelper.tabLabel(
                                    screen,
                                    PersonalDatabaseScreenCommonHelper.findTab(screen, hoveredTab)
                            ),
                            PersonalDatabaseScreenCommonHelper.scopeLabel(hoveredTab.scope()).copy().withStyle(ChatFormatting.GRAY)
                    ),
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
        for (int panelIndex = 0; panelIndex < PersonalDatabaseScreenCommonHelper.currentPanels(screen).size(); panelIndex++) {
            PersonalDatabaseLayout.Rect searchRect = PersonalDatabaseScreenGeometry.panelSearchFieldRect(screen, panelIndex);
            int iconX = searchRect.x() + 6;
            int iconY = searchRect.y() + (searchRect.height() - PersonalDatabaseScreen.SEARCH_ICON_SIZE) / 2;
            VanillaWidgetRenderer.renderSearchGlyph(guiGraphics, iconX, iconY, 0xFFDCD4C8);

            PersonalDatabaseLayout.Rect sortRect = PersonalDatabaseScreenGeometry.panelSortButtonRect(screen, panelIndex);
            DatabaseSortOption sortOption = PersonalDatabaseScreenCommonHelper.sortOptionForPanel(screen, panelIndex);
            String sortLabel = PersonalDatabaseScreenCommonHelper.truncateToWidth(
                    screen,
                    PersonalDatabaseScreenCommonHelper.sortButtonLabel(sortOption).getString(),
                    Math.max(0, sortRect.width() - 28)
            );
            guiGraphics.drawString(
                    screen.screenFont(),
                    sortLabel,
                    sortRect.x() + 6,
                    sortRect.y() + 6,
                    PersonalDatabaseScreen.SORT_BUTTON_TEXT_COLOR,
                    false
            );
            VanillaWidgetRenderer.renderSortDirectionIndicator(
                    guiGraphics,
                    sortRect.right() - 21,
                    sortRect.y() + sortRect.height() / 2,
                    sortOption.direction() == DatabaseSortDirection.ASC,
                    0xFFD8D0C4
            );
            VanillaWidgetRenderer.renderDropdownIndicator(
                    guiGraphics,
                    sortRect.right() - 9,
                    sortRect.y() + sortRect.height() / 2,
                    0xFFD8D0C4
            );

            PersonalDatabaseLayout.Rect pageRect = PersonalDatabaseScreenGeometry.panelPageButtonRect(screen, panelIndex);
            VanillaWidgetRenderer.renderDropdownIndicator(
                    guiGraphics,
                    pageRect.right() - 10,
                    pageRect.y() + pageRect.height() / 2,
                    0xFFD8D0C4
            );
        }

        if (screen.settingsButton != null && screen.layout.settingsButtonRect().width() > 0) {
            PersonalDatabaseLayout.Rect settingsRect = screen.layout.settingsButtonRect();
            VanillaWidgetRenderer.renderDropdownIndicator(guiGraphics, settingsRect.right() - 10, settingsRect.y() + settingsRect.height() / 2, 0xFFD8D0C4);
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
        renderFrameBadgeText(
                screen,
                guiGraphics,
                Component.translatable(screen.databaseMenu.viewState().query().visibleTabs().stream()
                        .map(com.agguy.infiniteinventory.database.DatabaseScopedTabRef::scope)
                        .distinct()
                        .count() > 1L
                        ? "screen.infiniteinventory.database.section.mixed"
                        : screen.databaseMenu.viewState().query().scope().sectionTranslationKey()).getString(),
                screen.layout.databasePanelRect().x() + PersonalDatabaseLayout.GRID_PADDING,
                screen.layout.databasePanelRect().y() - 14,
                PersonalDatabaseScreen.FRAME_TEXT_COLOR
        );
    }
    static void renderDatabaseSlots(PersonalDatabaseScreen screen, GuiGraphics guiGraphics) {
        if (screen.layout == null) {
            return;
        }
        if (screen.settingsPanelExpanded) {
            return;
        }
        PersonalDatabaseLayout.Rect logPanelRect = screen.logPanelExpanded
                ? PersonalDatabaseScreenLogGeometry.logPanelRect(screen)
                : PersonalDatabaseLayout.Rect.empty();
        for (int panelIndex = 0; panelIndex < PersonalDatabaseScreenCommonHelper.currentPanels(screen).size(); panelIndex++) {
            int visibleSlotCount = screen.layout.visibleDatabaseSlotCount(panelIndex);
            for (int slotIndex = 0; slotIndex < visibleSlotCount; slotIndex++) {
                PersonalDatabaseLayout.Rect slotRect = screen.layout.visibleDatabaseSlotBounds(panelIndex, slotIndex);
                if (logPanelRect.intersects(slotRect)) {
                    continue;
                }
                VanillaWidgetRenderer.renderSlot(guiGraphics, slotRect.x(), slotRect.y());
            }
        }
    }
    private static DatabaseScopedTabRef findHoveredTab(PersonalDatabaseScreen screen, double mouseX, double mouseY) {
        List<DatabaseScopedTabRef> visibleTabs = PersonalDatabaseScreenTabHelper.visibleTopTabs(screen);
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
    private static void renderPanelTextFields(PersonalDatabaseScreen screen, GuiGraphics guiGraphics) {
        for (int panelIndex = 0; panelIndex < screen.panelSearchBoxes.size(); panelIndex++) {
            PersonalDatabaseLayout.Rect searchRect = PersonalDatabaseScreenGeometry.panelSearchFieldRect(screen, panelIndex);
            boolean focused = screen.panelSearchBoxes.get(panelIndex).isFocused();
            VanillaWidgetRenderer.renderTextField(guiGraphics, searchRect, focused);
        }
    }
    private static void renderFrameBadgeText(PersonalDatabaseScreen screen, GuiGraphics guiGraphics, String text, int x, int y, int textColor) {
        if (text == null || text.isBlank()) return;
        int textWidth = screen.screenFont().width(text);
        int left = x - 4;
        int top = y - 3;
        int right = x + textWidth + 4;
        int bottom = y + screen.screenFont().lineHeight + 3;
        guiGraphics.fill(left, top, right, bottom, PersonalDatabaseScreen.FRAME_TEXT_BACKDROP_COLOR);
        guiGraphics.fill(left, bottom - 1, right, bottom, PersonalDatabaseScreen.FRAME_TEXT_OUTLINE_COLOR);
        guiGraphics.drawString(screen.screenFont(), text, x, y, textColor, true);
    }
}
