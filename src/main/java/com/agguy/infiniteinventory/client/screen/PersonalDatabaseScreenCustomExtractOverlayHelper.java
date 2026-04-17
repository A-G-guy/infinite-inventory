package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.menu.PersonalDatabaseLayout;
import com.agguy.infiniteinventory.network.DatabaseSelectionAction;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

final class PersonalDatabaseScreenCustomExtractOverlayHelper {
    private static final int OVERLAY_WIDTH = 236;
    private static final int OVERLAY_HEIGHT = 134;
    private static final int OVERLAY_PADDING = 8;
    private static final int FIELD_HEIGHT = 20;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 6;
    private static final int BUTTON_WIDTH = 72;

    private PersonalDatabaseScreenCustomExtractOverlayHelper() {
    }

    static void openOverlay(PersonalDatabaseScreen screen) {
        if (!PersonalDatabaseScreenSelectionHelper.hasSelection(screen)) {
            return;
        }
        ensureAmountBox(screen);
        syncAmountBoxBounds(screen);
        screen.customExtractOverlayExpanded = true;
        screen.customExtractAmountBox.setValue("");
        screen.customExtractAmountBox.setFocused(true);
        screen.focusScreen(screen.customExtractAmountBox);
        PersonalDatabaseScreenContextHelper.closeContextMenu(screen);
    }

    static void closeOverlay(PersonalDatabaseScreen screen) {
        screen.customExtractOverlayExpanded = false;
        if (screen.customExtractAmountBox != null) {
            screen.customExtractAmountBox.setFocused(false);
            screen.customExtractAmountBox.setValue("");
        }
        screen.focusScreen(null);
    }

    static void validateOverlay(PersonalDatabaseScreen screen) {
        if (screen.customExtractOverlayExpanded && !PersonalDatabaseScreenSelectionHelper.hasSelection(screen)) {
            closeOverlay(screen);
        }
    }

    static boolean handleMouseClicked(PersonalDatabaseScreen screen, double mouseX, double mouseY, int button) {
        if (!screen.customExtractOverlayExpanded) {
            return false;
        }
        PersonalDatabaseLayout.Rect panelRect = panelRect(screen);
        if (PersonalDatabaseScreenOverlayRenderHelper.isOverlayCloseClicked(panelRect, mouseX, mouseY)
                || cancelButtonRect(screen).contains(mouseX, mouseY)
                || !panelRect.contains(mouseX, mouseY)) {
            closeOverlay(screen);
            return true;
        }
        ensureAmountBox(screen);
        if (fieldRect(screen).contains(mouseX, mouseY)) {
            screen.focusScreen(screen.customExtractAmountBox);
            screen.customExtractAmountBox.setFocused(true);
            screen.customExtractAmountBox.mouseClicked(mouseX, mouseY, button);
            return true;
        }
        if (applyButtonRect(screen).contains(mouseX, mouseY) && canSubmit(screen)) {
            submit(screen);
            return true;
        }
        return true;
    }

    static boolean keyPressed(PersonalDatabaseScreen screen, int keyCode, int scanCode, int modifiers) {
        if (!screen.customExtractOverlayExpanded) {
            return false;
        }
        ensureAmountBox(screen);
        if (screen.customExtractAmountBox.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) {
            if (canSubmit(screen)) {
                submit(screen);
            }
            return true;
        }
        return true;
    }

    static boolean charTyped(PersonalDatabaseScreen screen, char codePoint, int modifiers) {
        if (!screen.customExtractOverlayExpanded) {
            return false;
        }
        ensureAmountBox(screen);
        return screen.customExtractAmountBox.charTyped(codePoint, modifiers);
    }

    static void renderOverlay(PersonalDatabaseScreen screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (!screen.customExtractOverlayExpanded) {
            return;
        }
        ensureAmountBox(screen);
        syncAmountBoxBounds(screen);
        PersonalDatabaseLayout.Rect panelRect = panelRect(screen);
        PersonalDatabaseLayout.Rect fieldRect = fieldRect(screen);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 261.0F);
        VanillaWidgetRenderer.renderOverlayPanel(guiGraphics, panelRect);
        guiGraphics.drawString(
                screen.screenFont(),
                Component.translatable("screen.infiniteinventory.custom_extract.title"),
                panelRect.x() + OVERLAY_PADDING,
                panelRect.y() + OVERLAY_PADDING,
                PersonalDatabaseScreen.OVERLAY_TEXT_COLOR,
                false
        );
        guiGraphics.fill(
                panelRect.x() + OVERLAY_PADDING,
                panelRect.y() + OVERLAY_PADDING + PersonalDatabaseScreen.OVERLAY_SECTION_TITLE_HEIGHT,
                panelRect.right() - OVERLAY_PADDING,
                panelRect.y() + OVERLAY_PADDING + PersonalDatabaseScreen.OVERLAY_SECTION_TITLE_HEIGHT + 1,
                0x70A89E8C
        );
        PersonalDatabaseScreenOverlayRenderHelper.renderOverlayCloseButton(screen, guiGraphics, panelRect, mouseX, mouseY);
        guiGraphics.drawString(
                screen.screenFont(),
                Component.translatable(descriptionKey(screen)),
                panelRect.x() + OVERLAY_PADDING,
                panelRect.y() + 38,
                PersonalDatabaseScreen.OVERLAY_MUTED_TEXT_COLOR,
                false
        );
        guiGraphics.drawString(
                screen.screenFont(),
                Component.translatable("screen.infiniteinventory.custom_extract.label"),
                fieldRect.x(),
                fieldRect.y() - 12,
                PersonalDatabaseScreen.OVERLAY_TEXT_COLOR,
                false
        );
        VanillaWidgetRenderer.renderTextField(guiGraphics, fieldRect, screen.customExtractAmountBox.isFocused());
        screen.customExtractAmountBox.render(guiGraphics, mouseX, mouseY, 0.0F);
        if (screen.customExtractAmountBox.getValue().isEmpty() && !screen.customExtractAmountBox.isFocused()) {
            guiGraphics.drawString(
                    screen.screenFont(),
                    Component.translatable("screen.infiniteinventory.custom_extract.placeholder"),
                    fieldRect.x() + 4,
                    fieldRect.y() + 6,
                    PersonalDatabaseScreen.OVERLAY_MUTED_TEXT_COLOR,
                    false
            );
        }
        renderActionButton(
                screen,
                guiGraphics,
                cancelButtonRect(screen),
                mouseX,
                mouseY,
                Component.translatable("screen.infiniteinventory.common.cancel"),
                true
        );
        renderActionButton(
                screen,
                guiGraphics,
                applyButtonRect(screen),
                mouseX,
                mouseY,
                Component.translatable("screen.infiniteinventory.common.apply"),
                canSubmit(screen)
        );
        guiGraphics.pose().popPose();
    }

    static long saturatedAmount(@Nullable String value) {
        if (value == null || value.isEmpty()) {
            return 0L;
        }
        long parsed = 0L;
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character < '0' || character > '9') {
                return 0L;
            }
            int digit = character - '0';
            if (parsed > (Long.MAX_VALUE - digit) / 10L) {
                return Long.MAX_VALUE;
            }
            parsed = parsed * 10L + digit;
        }
        return parsed;
    }

    private static void submit(PersonalDatabaseScreen screen) {
        long requestedAmount = saturatedAmount(screen.customExtractAmountBox == null ? "" : screen.customExtractAmountBox.getValue());
        if (requestedAmount <= 0L) {
            return;
        }
        closeOverlay(screen);
        PersonalDatabaseScreenSelectionHelper.sendSelectionAction(
                screen,
                DatabaseSelectionAction.EXTRACT_CUSTOM_TO_INVENTORY,
                "",
                requestedAmount
        );
    }

    private static void ensureAmountBox(PersonalDatabaseScreen screen) {
        if (screen.customExtractAmountBox != null) {
            return;
        }
        screen.customExtractAmountBox = new EditBox(
                screen.screenFont(),
                0,
                0,
                10,
                12,
                Component.translatable("screen.infiniteinventory.custom_extract.label")
        );
        screen.customExtractAmountBox.setBordered(false);
        screen.customExtractAmountBox.setMaxLength(Integer.MAX_VALUE);
        screen.customExtractAmountBox.setTextColor(PersonalDatabaseScreen.OVERLAY_ACCENT_TEXT_COLOR);
        screen.customExtractAmountBox.setTextColorUneditable(PersonalDatabaseScreen.OVERLAY_MUTED_TEXT_COLOR);
        screen.customExtractAmountBox.setFilter(PersonalDatabaseScreenCustomExtractOverlayHelper::isAllowedInput);
    }

    private static void syncAmountBoxBounds(PersonalDatabaseScreen screen) {
        if (screen.customExtractAmountBox == null) {
            return;
        }
        PersonalDatabaseLayout.Rect fieldRect = fieldRect(screen);
        screen.customExtractAmountBox.setX(fieldRect.x() + 4);
        screen.customExtractAmountBox.setY(fieldRect.y() + 4);
        screen.customExtractAmountBox.setWidth(Math.max(1, fieldRect.width() - 8));
        screen.customExtractAmountBox.setHeight(12);
    }

    private static boolean isAllowedInput(String value) {
        if (value == null || value.isEmpty()) {
            return true;
        }
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character < '0' || character > '9') {
                return false;
            }
        }
        return true;
    }

    private static boolean canSubmit(PersonalDatabaseScreen screen) {
        return saturatedAmount(screen.customExtractAmountBox == null ? "" : screen.customExtractAmountBox.getValue()) > 0L;
    }

    private static String descriptionKey(PersonalDatabaseScreen screen) {
        return PersonalDatabaseScreenCommonHelper.selectedEntryCount(screen) > 1
                ? "screen.infiniteinventory.custom_extract.selection_description"
                : "screen.infiniteinventory.custom_extract.single_description";
    }

    private static PersonalDatabaseLayout.Rect panelRect(PersonalDatabaseScreen screen) {
        return PersonalDatabaseScreenGeometry.centeredOverlayRect(screen, OVERLAY_WIDTH, OVERLAY_HEIGHT);
    }

    private static PersonalDatabaseLayout.Rect fieldRect(PersonalDatabaseScreen screen) {
        PersonalDatabaseLayout.Rect panelRect = panelRect(screen);
        return new PersonalDatabaseLayout.Rect(
                panelRect.x() + OVERLAY_PADDING,
                panelRect.y() + 60,
                panelRect.width() - OVERLAY_PADDING * 2,
                FIELD_HEIGHT
        );
    }

    private static PersonalDatabaseLayout.Rect cancelButtonRect(PersonalDatabaseScreen screen) {
        PersonalDatabaseLayout.Rect panelRect = panelRect(screen);
        int buttonsY = panelRect.bottom() - OVERLAY_PADDING - BUTTON_HEIGHT;
        int applyX = panelRect.right() - OVERLAY_PADDING - BUTTON_WIDTH;
        return new PersonalDatabaseLayout.Rect(
                applyX - BUTTON_GAP - BUTTON_WIDTH,
                buttonsY,
                BUTTON_WIDTH,
                BUTTON_HEIGHT
        );
    }

    private static PersonalDatabaseLayout.Rect applyButtonRect(PersonalDatabaseScreen screen) {
        PersonalDatabaseLayout.Rect panelRect = panelRect(screen);
        return new PersonalDatabaseLayout.Rect(
                panelRect.right() - OVERLAY_PADDING - BUTTON_WIDTH,
                panelRect.bottom() - OVERLAY_PADDING - BUTTON_HEIGHT,
                BUTTON_WIDTH,
                BUTTON_HEIGHT
        );
    }

    private static void renderActionButton(
            PersonalDatabaseScreen screen,
            GuiGraphics guiGraphics,
            PersonalDatabaseLayout.Rect rect,
            int mouseX,
            int mouseY,
            Component label,
            boolean active
    ) {
        boolean hovered = active && rect.contains(mouseX, mouseY);
        VanillaWidgetRenderer.renderOverlayChip(guiGraphics, rect, hovered, false, active);
        PersonalDatabaseScreenCommonHelper.drawCenteredShadow(
                screen,
                guiGraphics,
                label,
                rect.x() + 2,
                rect.right() - 2,
                rect.y() + 6,
                active ? PersonalDatabaseScreen.OVERLAY_TEXT_COLOR : PersonalDatabaseScreen.OVERLAY_MUTED_TEXT_COLOR
        );
    }
}
