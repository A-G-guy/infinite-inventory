package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.database.DatabaseCategory;
import com.agguy.infiniteinventory.database.DatabaseItemClassifier;
import com.agguy.infiniteinventory.menu.PersonalDatabaseLayout;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

final class PersonalDatabaseScreenIconPickerHelper {
    private static volatile List<PersonalDatabaseScreen.IconChoice> cachedChoices = List.of();

    private PersonalDatabaseScreenIconPickerHelper() {
    }

    static void renderIconPicker(PersonalDatabaseScreen screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
        PersonalDatabaseLayout.Rect panelRect = PersonalDatabaseScreenIconPickerGeometry.iconPickerRect(screen);
        if (panelRect.width() <= 0 || panelRect.height() <= 0) {
            return;
        }
        PersonalDatabaseScreenManagementHelper.ensureManagementWidgets(screen);
        List<PersonalDatabaseScreen.IconChoice> filteredChoices = filteredChoices(screen);
        int pageSize = PersonalDatabaseScreenIconPickerGeometry.iconPickerPageSize(screen);
        int totalPages = Math.max(1, (filteredChoices.size() + pageSize - 1) / pageSize);
        screen.iconPickerPageIndex = Mth.clamp(screen.iconPickerPageIndex, 0, totalPages - 1);
        List<PersonalDatabaseScreen.IconChoice> pageChoices = pagedChoices(filteredChoices, screen.iconPickerPageIndex, pageSize);
        PersonalDatabaseScreen.IconChoice hoveredChoice = null;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 256.0F);
        VanillaWidgetRenderer.renderOverlayPanel(guiGraphics, panelRect);
        guiGraphics.drawString(
                screen.screenFont(),
                Component.translatable("screen.infiniteinventory.icon_picker.title"),
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

        PersonalDatabaseLayout.Rect searchFieldRect = PersonalDatabaseScreenIconPickerGeometry.iconPickerSearchFieldRect(screen);
        VanillaWidgetRenderer.renderTextField(
                guiGraphics,
                searchFieldRect,
                screen.iconSearchBox != null && screen.iconSearchBox.isFocused()
        );
        if (screen.iconSearchBox != null) {
            screen.iconSearchBox.render(guiGraphics, mouseX, mouseY, 0.0F);
        }
        if (screen.iconSearchBox != null && !screen.iconSearchBox.isFocused() && screen.iconSearchBox.getValue().isBlank()) {
            guiGraphics.drawString(
                    screen.screenFont(),
                    Component.translatable("screen.infiniteinventory.icon_picker.search"),
                    searchFieldRect.x() + 6,
                    searchFieldRect.y() + 6,
                    0x777777,
                    false
            );
        }

        DatabaseCategory[] categories = DatabaseCategory.values();
        for (int index = 0; index < categories.length; index++) {
            DatabaseCategory category = categories[index];
            PersonalDatabaseLayout.Rect rect = PersonalDatabaseScreenIconPickerGeometry.iconPickerCategoryRect(screen, index);
            boolean hovered = rect.contains(mouseX, mouseY);
            boolean selected = screen.iconPickerCategory == category;
            VanillaWidgetRenderer.renderOverlayChip(guiGraphics, rect, hovered, selected, true);
            PersonalDatabaseScreenCommonHelper.drawCenteredShadow(
                    screen,
                    guiGraphics,
                    Component.translatable(shortCategoryTranslationKey(category)),
                    rect.x() + 2,
                    rect.right() - 2,
                    rect.y() + 6,
                    selected ? PersonalDatabaseScreen.OVERLAY_ACCENT_TEXT_COLOR : PersonalDatabaseScreen.OVERLAY_TEXT_COLOR
            );
        }

        for (int index = 0; index < pageChoices.size(); index++) {
            PersonalDatabaseScreen.IconChoice choice = pageChoices.get(index);
            PersonalDatabaseLayout.Rect cellRect = PersonalDatabaseScreenIconPickerGeometry.iconPickerCellRect(screen, index);
            boolean hovered = cellRect.contains(mouseX, mouseY);
            boolean selected = screen.pendingIconItemId.equals(choice.itemId());
            VanillaWidgetRenderer.renderOverlayChip(guiGraphics, cellRect, hovered, selected, true);
            guiGraphics.renderItem(choice.previewStack(), cellRect.x() + (cellRect.width() - 16) / 2, cellRect.y() + 8);
            if (hovered) {
                hoveredChoice = choice;
            }
        }
        if (pageChoices.isEmpty()) {
            PersonalDatabaseScreenCommonHelper.drawCenteredShadow(
                    screen,
                    guiGraphics,
                    Component.translatable("screen.infiniteinventory.icon_picker.empty"),
                    PersonalDatabaseScreenIconPickerGeometry.iconPickerGridRect(screen).x(),
                    PersonalDatabaseScreenIconPickerGeometry.iconPickerGridRect(screen).right(),
                    PersonalDatabaseScreenIconPickerGeometry.iconPickerGridRect(screen).y() + 20,
                    PersonalDatabaseScreen.OVERLAY_MUTED_TEXT_COLOR
            );
        }

        renderFooter(screen, guiGraphics, mouseX, mouseY, totalPages);

        if (hoveredChoice != null) {
            guiGraphics.renderTooltip(
                    screen.screenFont(),
                    List.of(
                            Component.literal(hoveredChoice.itemId()),
                            hoveredChoice.previewStack().getHoverName().copy().withStyle(ChatFormatting.GRAY)
                    ),
                    hoveredChoice.previewStack().getTooltipImage(),
                    mouseX,
                    mouseY
            );
        }
        guiGraphics.pose().popPose();
    }

    static boolean handleIconPickerClick(PersonalDatabaseScreen screen, double mouseX, double mouseY) {
        if (!screen.iconPickerExpanded) {
            return false;
        }
        PersonalDatabaseLayout.Rect panelRect = PersonalDatabaseScreenIconPickerGeometry.iconPickerRect(screen);
        if (PersonalDatabaseScreenOverlayRenderHelper.isOverlayCloseClicked(panelRect, mouseX, mouseY)) {
            PersonalDatabaseScreenManagementHelper.closeIconPicker(screen);
            return true;
        }
        if (!panelRect.contains(mouseX, mouseY)) {
            PersonalDatabaseScreenManagementHelper.closeIconPicker(screen);
            return true;
        }
        if (screen.iconSearchBox != null
                && PersonalDatabaseScreenIconPickerGeometry.iconPickerSearchFieldRect(screen).contains(mouseX, mouseY)) {
            screen.iconSearchBox.mouseClicked(mouseX, mouseY, 0);
            return true;
        }

        DatabaseCategory[] categories = DatabaseCategory.values();
        for (int index = 0; index < categories.length; index++) {
            if (!PersonalDatabaseScreenIconPickerGeometry.iconPickerCategoryRect(screen, index).contains(mouseX, mouseY)) {
                continue;
            }
            screen.iconPickerCategory = categories[index];
            screen.iconPickerPageIndex = 0;
            return true;
        }

        List<PersonalDatabaseScreen.IconChoice> filteredChoices = filteredChoices(screen);
        int pageSize = PersonalDatabaseScreenIconPickerGeometry.iconPickerPageSize(screen);
        int totalPages = Math.max(1, (filteredChoices.size() + pageSize - 1) / pageSize);
        screen.iconPickerPageIndex = Mth.clamp(screen.iconPickerPageIndex, 0, totalPages - 1);
        List<PersonalDatabaseScreen.IconChoice> pageChoices = pagedChoices(filteredChoices, screen.iconPickerPageIndex, pageSize);
        for (int index = 0; index < pageChoices.size(); index++) {
            if (!PersonalDatabaseScreenIconPickerGeometry.iconPickerCellRect(screen, index).contains(mouseX, mouseY)) {
                continue;
            }
            screen.pendingIconItemId = pageChoices.get(index).itemId();
            return true;
        }

        if (PersonalDatabaseScreenIconPickerGeometry.iconPickerPreviousPageButtonRect(screen).contains(mouseX, mouseY)) {
            screen.iconPickerPageIndex = Math.max(0, screen.iconPickerPageIndex - 1);
            return true;
        }
        if (PersonalDatabaseScreenIconPickerGeometry.iconPickerNextPageButtonRect(screen).contains(mouseX, mouseY)) {
            screen.iconPickerPageIndex = Math.min(totalPages - 1, screen.iconPickerPageIndex + 1);
            return true;
        }
        if (PersonalDatabaseScreenIconPickerGeometry.iconPickerCancelButtonRect(screen).contains(mouseX, mouseY)) {
            PersonalDatabaseScreenManagementHelper.closeIconPicker(screen);
            return true;
        }
        if (PersonalDatabaseScreenIconPickerGeometry.iconPickerApplyButtonRect(screen).contains(mouseX, mouseY)) {
            applyAndClose(screen);
            return true;
        }
        return true;
    }

    private static void renderFooter(
            PersonalDatabaseScreen screen,
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            int totalPages
    ) {
        boolean allTab = PersonalDatabaseScreenCommonHelper.findTab(screen, screen.managementSelectedTabId).isAllTab();
        ItemStack selectedStack = PersonalDatabaseScreenCommonHelper.resolveIconStack(screen, screen.pendingIconItemId, allTab);
        PersonalDatabaseLayout.Rect previewRect = PersonalDatabaseScreenIconPickerGeometry.iconPickerSelectedPreviewRect(screen);
        VanillaWidgetRenderer.renderOverlayRow(guiGraphics, previewRect, previewRect.contains(mouseX, mouseY), false);
        guiGraphics.renderItem(selectedStack, previewRect.x() + 3, previewRect.y() + 2);
        String previewText = Component.translatable(
                "screen.infiniteinventory.icon_picker.selected",
                selectedStack.getHoverName().getString() + " (" + screen.pendingIconItemId + ")"
        ).getString();
        guiGraphics.drawString(
                screen.screenFont(),
                PersonalDatabaseScreenGeometry.truncateToWidth(screen, previewText, Math.max(0, previewRect.width() - 28)),
                previewRect.x() + 24,
                previewRect.y() + 6,
                PersonalDatabaseScreen.OVERLAY_TEXT_COLOR,
                false
        );

        renderFooterButton(
                screen,
                guiGraphics,
                PersonalDatabaseScreenIconPickerGeometry.iconPickerPreviousPageButtonRect(screen),
                mouseX,
                mouseY,
                Component.literal("<"),
                screen.iconPickerPageIndex > 0
        );
        renderFooterButton(
                screen,
                guiGraphics,
                PersonalDatabaseScreenIconPickerGeometry.iconPickerNextPageButtonRect(screen),
                mouseX,
                mouseY,
                Component.literal(">"),
                screen.iconPickerPageIndex + 1 < totalPages
        );

        PersonalDatabaseLayout.Rect pageRect = PersonalDatabaseScreenIconPickerGeometry.iconPickerPageLabelRect(screen);
        VanillaWidgetRenderer.renderOverlayChip(guiGraphics, pageRect, pageRect.contains(mouseX, mouseY), false, true);
        PersonalDatabaseScreenCommonHelper.drawCenteredShadow(
                screen,
                guiGraphics,
                Component.translatable("screen.infiniteinventory.page_compact", screen.iconPickerPageIndex + 1, totalPages),
                pageRect.x() + 2,
                pageRect.right() - 2,
                pageRect.y() + 6,
                PersonalDatabaseScreen.OVERLAY_TEXT_COLOR
        );

        renderFooterButton(
                screen,
                guiGraphics,
                PersonalDatabaseScreenIconPickerGeometry.iconPickerCancelButtonRect(screen),
                mouseX,
                mouseY,
                Component.translatable("screen.infiniteinventory.common.cancel"),
                true
        );
        renderFooterButton(
                screen,
                guiGraphics,
                PersonalDatabaseScreenIconPickerGeometry.iconPickerApplyButtonRect(screen),
                mouseX,
                mouseY,
                Component.translatable("screen.infiniteinventory.common.apply"),
                true
        );
    }

    private static void renderFooterButton(
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

    private static List<PersonalDatabaseScreen.IconChoice> filteredChoices(PersonalDatabaseScreen screen) {
        String keyword = screen.iconSearchBox == null
                ? ""
                : screen.iconSearchBox.getValue().trim().toLowerCase(Locale.ROOT);
        DatabaseCategory activeCategory = screen.iconPickerCategory == null ? DatabaseCategory.ALL : screen.iconPickerCategory;
        List<PersonalDatabaseScreen.IconChoice> filtered = new ArrayList<>();
        for (PersonalDatabaseScreen.IconChoice choice : allChoices()) {
            if (activeCategory != DatabaseCategory.ALL && choice.category() != activeCategory) {
                continue;
            }
            if (!keyword.isBlank() && !choice.searchableText().contains(keyword)) {
                continue;
            }
            filtered.add(choice);
        }
        return List.copyOf(filtered);
    }

    private static List<PersonalDatabaseScreen.IconChoice> pagedChoices(
            List<PersonalDatabaseScreen.IconChoice> filteredChoices,
            int pageIndex,
            int pageSize
    ) {
        if (filteredChoices.isEmpty()) {
            return List.of();
        }
        int fromIndex = Math.min(filteredChoices.size(), pageIndex * Math.max(1, pageSize));
        int toIndex = Math.min(filteredChoices.size(), fromIndex + Math.max(1, pageSize));
        return filteredChoices.subList(fromIndex, toIndex);
    }

    private static List<PersonalDatabaseScreen.IconChoice> allChoices() {
        if (!cachedChoices.isEmpty()) {
            return cachedChoices;
        }
        List<PersonalDatabaseScreen.IconChoice> builtChoices = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR) {
                continue;
            }
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
            if (itemId == null) {
                continue;
            }
            ItemStack previewStack = new ItemStack(item);
            String searchableText = (itemId + " " + previewStack.getHoverName().getString()).toLowerCase(Locale.ROOT);
            builtChoices.add(new PersonalDatabaseScreen.IconChoice(
                    itemId.toString(),
                    previewStack,
                    searchableText,
                    DatabaseItemClassifier.INSTANCE.classify(previewStack)
            ));
        }
        builtChoices.sort(Comparator
                .comparing((PersonalDatabaseScreen.IconChoice choice) -> choice.category().ordinal())
                .thenComparing(PersonalDatabaseScreen.IconChoice::itemId));
        cachedChoices = List.copyOf(builtChoices);
        return cachedChoices;
    }

    private static String shortCategoryTranslationKey(DatabaseCategory category) {
        return switch (category) {
            case ALL -> "screen.infiniteinventory.category_short.all";
            case BLOCKS -> "screen.infiniteinventory.category_short.blocks";
            case TOOLS_WEAPONS -> "screen.infiniteinventory.category_short.tools_weapons";
            case EQUIPMENT -> "screen.infiniteinventory.category_short.equipment";
            case CONSUMABLES -> "screen.infiniteinventory.category_short.consumables";
            case MATERIALS -> "screen.infiniteinventory.category_short.materials";
            case OTHER -> "screen.infiniteinventory.category_short.other";
        };
    }

    private static void applyAndClose(PersonalDatabaseScreen screen) {
        screen.iconPickerOriginalItemId = screen.pendingIconItemId;
        screen.iconPickerExpanded = false;
        if (screen.iconSearchBox != null) {
            screen.iconSearchBox.setFocused(false);
        }
    }
}
