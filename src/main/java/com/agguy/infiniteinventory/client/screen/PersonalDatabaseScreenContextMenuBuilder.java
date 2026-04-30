package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.network.DatabaseClickAction;
import com.agguy.infiniteinventory.network.DatabaseSelectionAction;
import java.util.List;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

final class PersonalDatabaseScreenContextMenuBuilder {
    private static final List<PersonalDatabaseContextMenuItem> SINGLE_SELECTION_CONTEXT_MENU_ITEMS = List.of(
            PersonalDatabaseContextMenuItem.click("screen.infiniteinventory.context.take_single", DatabaseClickAction.TAKE_SINGLE),
            PersonalDatabaseContextMenuItem.click("screen.infiniteinventory.context.take_half_stack_to_inventory", DatabaseClickAction.TAKE_HALF_STACK_TO_INVENTORY),
            PersonalDatabaseContextMenuItem.click("screen.infiniteinventory.context.take_stack", DatabaseClickAction.TAKE_STACK),
            PersonalDatabaseContextMenuItem.click("screen.infiniteinventory.context.take_half_entry_to_inventory", DatabaseClickAction.TAKE_HALF_ENTRY_TO_INVENTORY),
            PersonalDatabaseContextMenuItem.click("screen.infiniteinventory.context.take_all_to_inventory", DatabaseClickAction.TAKE_ALL),
            PersonalDatabaseContextMenuItem.local("screen.infiniteinventory.context.take_custom_to_inventory", PersonalDatabaseContextMenuItem.LocalAction.OPEN_CUSTOM_EXTRACT_OVERLAY),
            PersonalDatabaseContextMenuItem.local("screen.infiniteinventory.context.edit_note", PersonalDatabaseContextMenuItem.LocalAction.OPEN_NOTE_OVERLAY),
            PersonalDatabaseContextMenuItem.local("screen.infiniteinventory.context.toggle_star", PersonalDatabaseContextMenuItem.LocalAction.TOGGLE_STAR),
            PersonalDatabaseContextMenuItem.selection("screen.infiniteinventory.selection.transfer", DatabaseSelectionAction.TRANSFER_TO_TAB)
    );
    private static final List<PersonalDatabaseContextMenuItem> MULTI_SELECTION_CONTEXT_MENU_ITEMS = List.of(
            PersonalDatabaseContextMenuItem.selection("screen.infiniteinventory.selection.take_one_each", DatabaseSelectionAction.EXTRACT_ONE_TO_INVENTORY),
            PersonalDatabaseContextMenuItem.selection("screen.infiniteinventory.selection.take_half_stack_each", DatabaseSelectionAction.EXTRACT_HALF_STACK_TO_INVENTORY),
            PersonalDatabaseContextMenuItem.selection("screen.infiniteinventory.selection.take_stack_each", DatabaseSelectionAction.EXTRACT_STACK_TO_INVENTORY),
            PersonalDatabaseContextMenuItem.selection("screen.infiniteinventory.selection.take_half_entry_each", DatabaseSelectionAction.EXTRACT_HALF_ENTRY_TO_INVENTORY),
            PersonalDatabaseContextMenuItem.selection("screen.infiniteinventory.selection.take_all_each", DatabaseSelectionAction.EXTRACT_ALL_TO_INVENTORY),
            PersonalDatabaseContextMenuItem.local("screen.infiniteinventory.selection.take_custom_each", PersonalDatabaseContextMenuItem.LocalAction.OPEN_CUSTOM_EXTRACT_OVERLAY),
            PersonalDatabaseContextMenuItem.local("screen.infiniteinventory.context.edit_note", PersonalDatabaseContextMenuItem.LocalAction.OPEN_NOTE_OVERLAY),
            PersonalDatabaseContextMenuItem.local("screen.infiniteinventory.context.toggle_star", PersonalDatabaseContextMenuItem.LocalAction.TOGGLE_STAR),
            PersonalDatabaseContextMenuItem.selection("screen.infiniteinventory.selection.transfer", DatabaseSelectionAction.TRANSFER_TO_TAB)
    );

    private PersonalDatabaseScreenContextMenuBuilder() {
    }

    static List<PersonalDatabaseContextMenuItem> contextMenuItems(PersonalDatabaseScreen screen) {
        int count = PersonalDatabaseScreenCommonHelper.selectedEntryCount(screen);
        if (count <= 0) return List.of();
        if (count == 1) return buildSingleSelectionMenu(screen);
        return buildMultiSelectionMenu(screen);
    }

    static List<PersonalDatabaseContextMenuItem> contextMenuItemsForSelectionCount(int selectedEntryCount) {
        if (selectedEntryCount <= 0) return List.of();
        return selectedEntryCount > 1 ? MULTI_SELECTION_CONTEXT_MENU_ITEMS : SINGLE_SELECTION_CONTEXT_MENU_ITEMS;
    }

    private static List<PersonalDatabaseContextMenuItem> buildSingleSelectionMenu(PersonalDatabaseScreen screen) {
        var entry = PersonalDatabaseScreenSelectionHelper.selectedEntries(screen).get(0);
        String starKey = PersonalDatabaseScreenStarMenuHelper.singleSelectionStarKey(screen, entry);
        return List.of(
                PersonalDatabaseContextMenuItem.click("screen.infiniteinventory.context.take_single", DatabaseClickAction.TAKE_SINGLE),
                PersonalDatabaseContextMenuItem.click("screen.infiniteinventory.context.take_half_stack_to_inventory", DatabaseClickAction.TAKE_HALF_STACK_TO_INVENTORY),
                PersonalDatabaseContextMenuItem.click("screen.infiniteinventory.context.take_stack", DatabaseClickAction.TAKE_STACK),
                PersonalDatabaseContextMenuItem.click("screen.infiniteinventory.context.take_half_entry_to_inventory", DatabaseClickAction.TAKE_HALF_ENTRY_TO_INVENTORY),
                PersonalDatabaseContextMenuItem.click("screen.infiniteinventory.context.take_all_to_inventory", DatabaseClickAction.TAKE_ALL),
                PersonalDatabaseContextMenuItem.local("screen.infiniteinventory.context.take_custom_to_inventory", PersonalDatabaseContextMenuItem.LocalAction.OPEN_CUSTOM_EXTRACT_OVERLAY),
                PersonalDatabaseContextMenuItem.local("screen.infiniteinventory.context.edit_note", PersonalDatabaseContextMenuItem.LocalAction.OPEN_NOTE_OVERLAY),
                PersonalDatabaseContextMenuItem.local(starKey, PersonalDatabaseContextMenuItem.LocalAction.TOGGLE_STAR),
                PersonalDatabaseContextMenuItem.selection("screen.infiniteinventory.selection.transfer", DatabaseSelectionAction.TRANSFER_TO_TAB)
        );
    }

    private static List<PersonalDatabaseContextMenuItem> buildMultiSelectionMenu(PersonalDatabaseScreen screen) {
        var starState = PersonalDatabaseScreenStarMenuHelper.resolveStarSelectionState(screen);
        java.util.ArrayList<PersonalDatabaseContextMenuItem> items = new java.util.ArrayList<>();
        items.add(PersonalDatabaseContextMenuItem.selection("screen.infiniteinventory.selection.take_one_each", DatabaseSelectionAction.EXTRACT_ONE_TO_INVENTORY));
        items.add(PersonalDatabaseContextMenuItem.selection("screen.infiniteinventory.selection.take_half_stack_each", DatabaseSelectionAction.EXTRACT_HALF_STACK_TO_INVENTORY));
        items.add(PersonalDatabaseContextMenuItem.selection("screen.infiniteinventory.selection.take_stack_each", DatabaseSelectionAction.EXTRACT_STACK_TO_INVENTORY));
        items.add(PersonalDatabaseContextMenuItem.selection("screen.infiniteinventory.selection.take_half_entry_each", DatabaseSelectionAction.EXTRACT_HALF_ENTRY_TO_INVENTORY));
        items.add(PersonalDatabaseContextMenuItem.selection("screen.infiniteinventory.selection.take_all_each", DatabaseSelectionAction.EXTRACT_ALL_TO_INVENTORY));
        items.add(PersonalDatabaseContextMenuItem.local("screen.infiniteinventory.selection.take_custom_each", PersonalDatabaseContextMenuItem.LocalAction.OPEN_CUSTOM_EXTRACT_OVERLAY));
        items.add(PersonalDatabaseContextMenuItem.local("screen.infiniteinventory.context.edit_note", PersonalDatabaseContextMenuItem.LocalAction.OPEN_NOTE_OVERLAY));
        items.addAll(PersonalDatabaseScreenStarMenuHelper.starMenuItemsForState(starState));
        items.add(PersonalDatabaseContextMenuItem.selection("screen.infiniteinventory.selection.transfer", DatabaseSelectionAction.TRANSFER_TO_TAB));
        return List.copyOf(items);
    }

    static Component contextMenuLabel(PersonalDatabaseContextMenuItem item) {
        return Component.translatable(item.translationKey());
    }

    @Nullable
    static RemixIcon contextMenuItemIcon(PersonalDatabaseContextMenuItem item) {
        return RemixIcon.forContextMenuKey(item.translationKey());
    }

    static int contextMenuWidth(PersonalDatabaseScreen screen) {
        int width = PersonalDatabaseScreen.CONTEXT_MENU_MIN_WIDTH;
        for (PersonalDatabaseContextMenuItem item : contextMenuItems(screen)) {
            width = Math.max(width, screen.screenFont().width(contextMenuLabel(item)) + 36);
        }
        return width;
    }

    static int contextMenuHeight(PersonalDatabaseScreen screen) {
        return contextMenuItems(screen).size() * PersonalDatabaseScreen.CONTEXT_MENU_ROW_HEIGHT;
    }
}
