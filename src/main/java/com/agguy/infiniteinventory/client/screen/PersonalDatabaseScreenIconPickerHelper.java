package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.menu.PersonalDatabaseLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

final class PersonalDatabaseScreenIconPickerHelper {
    private PersonalDatabaseScreenIconPickerHelper() {
    }

    static void closeIconPicker(PersonalDatabaseScreen screen) {
        screen.iconPickerExpanded = false;
        if (screen.iconSearchBox != null) {
            screen.iconSearchBox.setFocused(false);
        }
    }

    static void renderIconPicker(PersonalDatabaseScreen screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
        PersonalDatabaseLayout.Rect panelRect = PersonalDatabaseScreenGeometry.iconPickerRect(screen);
        if (panelRect.width() <= 0 || panelRect.height() <= 0) {
            return;
        }
        PersonalDatabaseScreenManagementHelper.ensureManagementWidgets(screen);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 256.0F);
        VanillaWidgetRenderer.renderOverlayPanel(guiGraphics, panelRect);
        guiGraphics.drawString(
                screen.screenFont(),
                Component.translatable("screen.infiniteinventory.icon_picker.title"),
                panelRect.x() + PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING,
                panelRect.y() + PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING,
                PersonalDatabaseScreen.OVERLAY_TEXT_COLOR,
                true
        );
        guiGraphics.fill(
                panelRect.x() + PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING,
                panelRect.y() + PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING + PersonalDatabaseScreen.OVERLAY_SECTION_TITLE_HEIGHT,
                panelRect.right() - PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING,
                panelRect.y() + PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING + PersonalDatabaseScreen.OVERLAY_SECTION_TITLE_HEIGHT + 1,
                0x70A89E8C
        );
        PersonalDatabaseLayout.Rect searchFieldRect = PersonalDatabaseScreenGeometry.iconPickerSearchFieldRect(screen);
        VanillaWidgetRenderer.renderTextField(
                guiGraphics,
                searchFieldRect,
                screen.iconSearchBox != null && screen.iconSearchBox.isFocused()
        );
        if (screen.iconSearchBox != null) {
            screen.iconSearchBox.render(guiGraphics, mouseX, mouseY, 0.0F);
        }

        List<PersonalDatabaseScreen.IconChoice> choices = matchingIconChoices(screen);
        PersonalDatabaseScreen.IconChoice hoveredChoice = null;
        for (int index = 0; index < choices.size(); index++) {
            PersonalDatabaseScreen.IconChoice choice = choices.get(index);
            PersonalDatabaseLayout.Rect cellRect = PersonalDatabaseScreenGeometry.iconPickerCellRect(screen, index);
            boolean hovered = cellRect.contains(mouseX, mouseY);
            boolean selected = screen.pendingIconItemId.equals(choice.itemId());
            VanillaWidgetRenderer.renderOverlayChip(guiGraphics, cellRect, hovered, selected, true);
            guiGraphics.renderItem(choice.previewStack(), cellRect.x() + (cellRect.width() - 16) / 2, cellRect.y() + 6);
            if (hovered) {
                hoveredChoice = choice;
            }
        }
        if (choices.isEmpty()) {
            PersonalDatabaseScreenCommonHelper.drawCenteredShadow(
                    screen,
                    guiGraphics,
                    Component.translatable("screen.infiniteinventory.icon_picker.empty"),
                    panelRect.x() + PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING,
                    panelRect.right() - PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING,
                    searchFieldRect.bottom() + 16,
                    PersonalDatabaseScreen.OVERLAY_MUTED_TEXT_COLOR
            );
        }
        guiGraphics.drawString(
                screen.screenFont(),
                PersonalDatabaseScreenGeometry.truncateToWidth(
                        screen,
                        Component.translatable("screen.infiniteinventory.icon_picker.selected", screen.pendingIconItemId)
                                .getString(),
                        panelRect.width() - PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING * 2
                ),
                panelRect.x() + PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING,
                panelRect.bottom() - PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING - 12,
                PersonalDatabaseScreen.OVERLAY_TEXT_COLOR,
                true
        );
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
        if (screen.iconSearchBox != null
                && PersonalDatabaseScreenGeometry.iconPickerSearchFieldRect(screen).contains(mouseX, mouseY)) {
            screen.iconSearchBox.mouseClicked(mouseX, mouseY, 0);
            return true;
        }
        List<PersonalDatabaseScreen.IconChoice> choices = matchingIconChoices(screen);
        for (int index = 0; index < choices.size(); index++) {
            PersonalDatabaseLayout.Rect cellRect = PersonalDatabaseScreenGeometry.iconPickerCellRect(screen, index);
            if (!cellRect.contains(mouseX, mouseY)) {
                continue;
            }
            screen.pendingIconItemId = choices.get(index).itemId();
            closeIconPicker(screen);
            return true;
        }
        if (!PersonalDatabaseScreenGeometry.iconPickerRect(screen).contains(mouseX, mouseY)) {
            closeIconPicker(screen);
            return true;
        }
        return true;
    }

    private static List<PersonalDatabaseScreen.IconChoice> matchingIconChoices(PersonalDatabaseScreen screen) {
        String keyword = screen.iconSearchBox == null
                ? ""
                : screen.iconSearchBox.getValue().trim().toLowerCase(Locale.ROOT);
        List<PersonalDatabaseScreen.IconChoice> matchingChoices = new ArrayList<>(PersonalDatabaseScreen.ICON_PICKER_MAX_RESULTS);
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
            if (!keyword.isBlank() && !searchableText.contains(keyword)) {
                continue;
            }
            matchingChoices.add(new PersonalDatabaseScreen.IconChoice(itemId.toString(), previewStack, searchableText));
            if (matchingChoices.size() >= PersonalDatabaseScreen.ICON_PICKER_MAX_RESULTS) {
                break;
            }
        }
        return List.copyOf(matchingChoices);
    }
}
