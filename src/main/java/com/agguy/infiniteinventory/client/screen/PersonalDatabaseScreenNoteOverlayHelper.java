package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.database.DatabasePanelView;
import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.DatabaseSelectionEntry;
import com.agguy.infiniteinventory.database.VisibleDatabaseEntry;
import com.agguy.infiniteinventory.menu.PersonalDatabaseLayout;
import com.agguy.infiniteinventory.network.DatabaseNotePayload;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

final class PersonalDatabaseScreenNoteOverlayHelper {
    private static final int OVERLAY_WIDTH = 320;
    private static final int OVERLAY_HEIGHT = 156;
    private static final int OVERLAY_PADDING = 8;
    private static final int FIELD_HEIGHT = 20;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 6;
    private static final int BUTTON_WIDTH = 72;
    private static final int MAX_NOTE_LENGTH = 256;

    private PersonalDatabaseScreenNoteOverlayHelper() {
    }

    static void openOverlay(PersonalDatabaseScreen screen) {
        if (!PersonalDatabaseScreenSelectionHelper.hasSelection(screen)) {
            return;
        }
        ensureNoteBox(screen);
        syncNoteBoxBounds(screen);
        screen.noteOverlayExpanded = true;
        NoteState noteState = resolveInitialNoteState(screen);
        screen.noteOverlayMixed = noteState.mixed;
        screen.noteEditBox.setValue(noteState.value);
        screen.noteEditBox.setFocused(true);
        screen.focusScreen(screen.noteEditBox);
        PersonalDatabaseScreenContextHelper.closeContextMenu(screen);
    }

    static void closeOverlay(PersonalDatabaseScreen screen) {
        screen.noteOverlayExpanded = false;
        screen.noteOverlayMixed = false;
        if (screen.noteEditBox != null) {
            screen.noteEditBox.setFocused(false);
            screen.noteEditBox.setValue("");
        }
        screen.focusScreen(null);
    }

    static void validateOverlay(PersonalDatabaseScreen screen) {
        if (screen.noteOverlayExpanded && !PersonalDatabaseScreenSelectionHelper.hasSelection(screen)) {
            closeOverlay(screen);
        }
    }

    static boolean handleMouseClicked(PersonalDatabaseScreen screen, double mouseX, double mouseY, int button) {
        if (!screen.noteOverlayExpanded) {
            return false;
        }
        PersonalDatabaseLayout.Rect panelRect = panelRect(screen);
        if (PersonalDatabaseScreenOverlayRenderHelper.isOverlayCloseClicked(panelRect, mouseX, mouseY)
                || cancelButtonRect(screen).contains(mouseX, mouseY)
                || !panelRect.contains(mouseX, mouseY)) {
            closeOverlay(screen);
            return true;
        }
        ensureNoteBox(screen);
        if (fieldRect(screen).contains(mouseX, mouseY)) {
            screen.focusScreen(screen.noteEditBox);
            screen.noteEditBox.setFocused(true);
            screen.noteEditBox.mouseClicked(mouseX, mouseY, button);
            return true;
        }
        if (applyButtonRect(screen).contains(mouseX, mouseY)) {
            submit(screen);
            return true;
        }
        return true;
    }

    static boolean keyPressed(PersonalDatabaseScreen screen, int keyCode, int scanCode, int modifiers) {
        if (!screen.noteOverlayExpanded) {
            return false;
        }
        ensureNoteBox(screen);
        if (screen.noteEditBox.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) {
            submit(screen);
            return true;
        }
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            closeOverlay(screen);
            return true;
        }
        return true;
    }

    static boolean charTyped(PersonalDatabaseScreen screen, char codePoint, int modifiers) {
        if (!screen.noteOverlayExpanded) {
            return false;
        }
        ensureNoteBox(screen);
        return screen.noteEditBox.charTyped(codePoint, modifiers);
    }

    static void renderOverlay(PersonalDatabaseScreen screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (!screen.noteOverlayExpanded) {
            return;
        }
        ensureNoteBox(screen);
        syncNoteBoxBounds(screen);
        PersonalDatabaseLayout.Rect panelRect = panelRect(screen);
        PersonalDatabaseLayout.Rect fieldRect = fieldRect(screen);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 261.0F);
        VanillaWidgetRenderer.renderOverlayPanel(guiGraphics, panelRect);
        guiGraphics.drawString(
                screen.screenFont(),
                Component.translatable("screen.infiniteinventory.note.title"),
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
                GuiTheme.DIVIDER
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
        int hintY = panelRect.y() + 51;
        if (screen.noteOverlayMixed && hintY + screen.screenFont().lineHeight <= fieldRect.y() - 4) {
            guiGraphics.drawString(
                    screen.screenFont(),
                    Component.translatable("screen.infiniteinventory.note.mixed_hint"),
                    panelRect.x() + OVERLAY_PADDING,
                    hintY,
                    GuiTheme.NOTE_MIXED_HINT,
                    false
            );
        }
        guiGraphics.drawString(
                screen.screenFont(),
                Component.translatable("screen.infiniteinventory.note.label"),
                fieldRect.x(),
                fieldRect.y() - 12,
                PersonalDatabaseScreen.OVERLAY_TEXT_COLOR,
                false
        );
        VanillaWidgetRenderer.renderTextField(guiGraphics, fieldRect, screen.noteEditBox.isFocused());
        screen.noteEditBox.render(guiGraphics, mouseX, mouseY, 0.0F);
        if (screen.noteEditBox.getValue().isEmpty() && !screen.noteEditBox.isFocused()) {
            guiGraphics.drawString(
                    screen.screenFont(),
                    Component.translatable("screen.infiniteinventory.note.placeholder"),
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
                true
        );
        guiGraphics.pose().popPose();
    }

    private record NoteState(String value, boolean mixed) {
    }

    private static NoteState resolveInitialNoteState(PersonalDatabaseScreen screen) {
        List<DatabaseSelectionEntry> selectedEntries = PersonalDatabaseScreenSelectionHelper.selectedEntries(screen);
        if (selectedEntries.isEmpty()) {
            return new NoteState("", false);
        }
        if (selectedEntries.size() == 1) {
            return new NoteState(findNoteForSelectionEntry(screen, selectedEntries.get(0)), false);
        }
        String commonNote = null;
        boolean hasAnyNote = false;
        for (DatabaseSelectionEntry selectionEntry : selectedEntries) {
            String note = findNoteForSelectionEntry(screen, selectionEntry);
            if (!note.isEmpty()) {
                hasAnyNote = true;
                if (commonNote == null) {
                    commonNote = note;
                } else if (!commonNote.equals(note)) {
                    return new NoteState("", true);
                }
            }
        }
        return new NoteState(hasAnyNote ? commonNote : "", false);
    }

    private static String findNoteForSelectionEntry(PersonalDatabaseScreen screen, DatabaseSelectionEntry selectionEntry) {
        if (selectionEntry == null || selectionEntry.isEmpty()) {
            return "";
        }
        for (DatabasePanelView panel : screen.databaseMenu.viewState().panels()) {
            if (panel.scopedTab().scope() != selectionEntry.scope()) {
                continue;
            }
            for (VisibleDatabaseEntry entry : panel.entries()) {
                if (entry.scope() == selectionEntry.scope()
                        && entry.tabId().equals(selectionEntry.sourceTabId())
                        && ItemStack.isSameItemSameComponents(entry.stack(), selectionEntry.displayStack())) {
                    return entry.note();
                }
            }
        }
        return "";
    }

    private static void submit(PersonalDatabaseScreen screen) {
        List<DatabaseSelectionEntry> selectedEntries = PersonalDatabaseScreenSelectionHelper.selectedEntries(screen);
        if (selectedEntries.isEmpty()) {
            closeOverlay(screen);
            return;
        }
        String noteValue = screen.noteEditBox == null ? "" : screen.noteEditBox.getValue().trim();
        if (noteValue.length() > MAX_NOTE_LENGTH) {
            noteValue = noteValue.substring(0, MAX_NOTE_LENGTH);
        }
        Map<DatabaseScope, List<ItemStack>> stacksByScope = new LinkedHashMap<>();
        for (DatabaseSelectionEntry entry : selectedEntries) {
            if (entry.isEmpty()) {
                continue;
            }
            stacksByScope.computeIfAbsent(entry.scope(), ignored -> new ArrayList<>()).add(entry.displayStack());
        }
        for (Map.Entry<DatabaseScope, List<ItemStack>> entry : stacksByScope.entrySet()) {
            PacketDistributor.sendToServer(new DatabaseNotePayload(
                    screen.databaseMenu.containerId,
                    screen.databaseMenu.viewState().sessionId(),
                    entry.getKey(),
                    entry.getValue(),
                    noteValue
            ));
        }
        closeOverlay(screen);
        PersonalDatabaseScreenSelectionHelper.clearSelection(screen);
    }

    private static void ensureNoteBox(PersonalDatabaseScreen screen) {
        if (screen.noteEditBox != null) {
            return;
        }
        screen.noteEditBox = new EditBox(
                screen.screenFont(),
                0,
                0,
                10,
                12,
                Component.translatable("screen.infiniteinventory.note.label")
        );
        screen.noteEditBox.setBordered(false);
        screen.noteEditBox.setMaxLength(MAX_NOTE_LENGTH);
        screen.noteEditBox.setTextColor(PersonalDatabaseScreen.TEXT_FIELD_TEXT_COLOR);
        screen.noteEditBox.setTextColorUneditable(PersonalDatabaseScreen.TEXT_FIELD_MUTED_TEXT_COLOR);
    }

    private static void syncNoteBoxBounds(PersonalDatabaseScreen screen) {
        if (screen.noteEditBox == null) {
            return;
        }
        PersonalDatabaseLayout.Rect fieldRect = fieldRect(screen);
        screen.noteEditBox.setX(fieldRect.x() + PersonalDatabaseScreen.TEXT_FIELD_LEFT_PADDING);
        screen.noteEditBox.setY(fieldRect.y() + 4);
        screen.noteEditBox.setWidth(Math.max(
                1,
                fieldRect.width() - PersonalDatabaseScreen.TEXT_FIELD_LEFT_PADDING - PersonalDatabaseScreen.TEXT_FIELD_RIGHT_PADDING
        ));
        screen.noteEditBox.setHeight(12);
    }

    private static String descriptionKey(PersonalDatabaseScreen screen) {
        return PersonalDatabaseScreenCommonHelper.selectedEntryCount(screen) > 1
                ? "screen.infiniteinventory.note.selection_description"
                : "screen.infiniteinventory.note.single_description";
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
