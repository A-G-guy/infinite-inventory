package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.database.DatabasePanelView;
import com.agguy.infiniteinventory.database.DatabaseAutoStoreTarget;
import com.agguy.infiniteinventory.database.DatabaseQuery;
import com.agguy.infiniteinventory.database.DatabaseSearchConfig;
import com.agguy.infiniteinventory.database.DatabaseSearchField;
import com.agguy.infiniteinventory.database.DatabaseSearchWeight;
import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.DatabaseTab;
import com.agguy.infiniteinventory.database.DatabaseTabs;
import com.agguy.infiniteinventory.network.DatabaseClickAction;
import com.agguy.infiniteinventory.menu.PersonalDatabaseLayout;
import com.agguy.infiniteinventory.network.DatabaseSelectionAction;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

final class PersonalDatabaseScreenCommonHelper {
    private static final List<PersonalDatabaseContextMenuItem> SINGLE_SELECTION_CONTEXT_MENU_ITEMS = List.of(
            PersonalDatabaseContextMenuItem.click("screen.infiniteinventory.context.take_single", DatabaseClickAction.TAKE_SINGLE),
            PersonalDatabaseContextMenuItem.click(
                    "screen.infiniteinventory.context.take_half_stack_to_inventory",
                    DatabaseClickAction.TAKE_HALF_STACK_TO_INVENTORY
            ),
            PersonalDatabaseContextMenuItem.click("screen.infiniteinventory.context.take_stack", DatabaseClickAction.TAKE_STACK),
            PersonalDatabaseContextMenuItem.click(
                    "screen.infiniteinventory.context.take_half_entry_to_inventory",
                    DatabaseClickAction.TAKE_HALF_ENTRY_TO_INVENTORY
            ),
            PersonalDatabaseContextMenuItem.click("screen.infiniteinventory.context.take_all_to_inventory", DatabaseClickAction.TAKE_ALL),
            PersonalDatabaseContextMenuItem.local(
                    "screen.infiniteinventory.context.take_custom_to_inventory",
                    PersonalDatabaseContextMenuItem.LocalAction.OPEN_CUSTOM_EXTRACT_OVERLAY
            ),
            PersonalDatabaseContextMenuItem.selection("screen.infiniteinventory.selection.transfer", DatabaseSelectionAction.TRANSFER_TO_TAB)
    );
    private static final List<PersonalDatabaseContextMenuItem> MULTI_SELECTION_CONTEXT_MENU_ITEMS = List.of(
            PersonalDatabaseContextMenuItem.selection(
                    "screen.infiniteinventory.selection.take_one_each",
                    DatabaseSelectionAction.EXTRACT_ONE_TO_INVENTORY
            ),
            PersonalDatabaseContextMenuItem.selection(
                    "screen.infiniteinventory.selection.take_half_stack_each",
                    DatabaseSelectionAction.EXTRACT_HALF_STACK_TO_INVENTORY
            ),
            PersonalDatabaseContextMenuItem.selection(
                    "screen.infiniteinventory.selection.take_stack_each",
                    DatabaseSelectionAction.EXTRACT_STACK_TO_INVENTORY
            ),
            PersonalDatabaseContextMenuItem.selection(
                    "screen.infiniteinventory.selection.take_half_entry_each",
                    DatabaseSelectionAction.EXTRACT_HALF_ENTRY_TO_INVENTORY
            ),
            PersonalDatabaseContextMenuItem.selection(
                    "screen.infiniteinventory.selection.take_all_each",
                    DatabaseSelectionAction.EXTRACT_ALL_TO_INVENTORY
            ),
            PersonalDatabaseContextMenuItem.local(
                    "screen.infiniteinventory.selection.take_custom_each",
                    PersonalDatabaseContextMenuItem.LocalAction.OPEN_CUSTOM_EXTRACT_OVERLAY
            ),
            PersonalDatabaseContextMenuItem.selection("screen.infiniteinventory.selection.transfer", DatabaseSelectionAction.TRANSFER_TO_TAB)
    );

    private PersonalDatabaseScreenCommonHelper() {
    }

    static List<DatabasePanelView> currentPanels(PersonalDatabaseScreen screen) {
        return screen.databaseMenu.viewState().panels();
    }

    static List<DatabaseTab> currentTabs(PersonalDatabaseScreen screen) {
        return screen.databaseMenu.viewState().tabsForScope(screen.databaseMenu.viewState().query().scope());
    }

    static List<DatabaseTab> tabsForScope(PersonalDatabaseScreen screen, DatabaseScope scope) {
        return screen.databaseMenu.viewState().tabsForScope(scope);
    }

    static List<DatabaseTab> currentConcreteTabs(PersonalDatabaseScreen screen) {
        return currentTabs(screen).stream().filter(DatabaseTab::isConcreteTab).toList();
    }

    static int focusedPanelIndex(PersonalDatabaseScreen screen) {
        String focusedTabId = screen.databaseMenu.viewState().query().focusedTabId();
        for (int index = 0; index < currentPanels(screen).size(); index++) {
            if (currentPanels(screen).get(index).tab().id().equals(focusedTabId)) {
                return index;
            }
        }
        return currentPanels(screen).isEmpty() ? -1 : 0;
    }

    static int activePagePickerPanelIndex(PersonalDatabaseScreen screen) {
        if (screen.activePagePickerPanelIndex >= 0 && screen.activePagePickerPanelIndex < currentPanels(screen).size()) {
            return screen.activePagePickerPanelIndex;
        }
        return focusedPanelIndex(screen);
    }

    static int activeSortPanelIndex(PersonalDatabaseScreen screen) {
        if (screen.activeSortPanelIndex >= 0 && screen.activeSortPanelIndex < currentPanels(screen).size()) {
            return screen.activeSortPanelIndex;
        }
        return focusedPanelIndex(screen);
    }

    static DatabaseTab findTab(PersonalDatabaseScreen screen, String tabId) {
        return findTab(screen, screen.databaseMenu.viewState().query().scope(), tabId);
    }

    static DatabaseTab findTab(PersonalDatabaseScreen screen, DatabaseScope scope, String tabId) {
        for (DatabaseTab tab : tabsForScope(screen, scope)) {
            if (tab.id().equals(tabId)) {
                return tab;
            }
        }
        return DatabaseTabs.isAllTabId(tabId) ? DatabaseTabs.allTab() : DatabaseTabs.defaultConcreteTab();
    }

    static Component tabLabel(PersonalDatabaseScreen screen, @Nullable DatabaseTab tab) {
        if (tab == null) {
            return Component.translatable(DatabaseTabs.DEFAULT_TAB_TRANSLATION_KEY);
        }
        if (tab.usesTranslationKey() || (!tab.translationKey().isBlank() && tab.customName().isBlank())) {
            return Component.translatable(tab.translationKey());
        }
        if (!tab.customName().isBlank()) {
            return Component.literal(tab.customName());
        }
        if (!tab.translationKey().isBlank()) {
            return Component.translatable(tab.translationKey());
        }
        return Component.literal(tab.id());
    }

    static Component autoStoreTargetLabel(PersonalDatabaseScreen screen, @Nullable DatabaseAutoStoreTarget autoStoreTarget) {
        DatabaseAutoStoreTarget resolvedTarget = autoStoreTarget == null
                ? DatabaseAutoStoreTarget.defaultTarget()
                : autoStoreTarget;
        Component scopeLabel = Component.translatable(resolvedTarget.scope().translationKey());
        Component tabLabel = tabLabel(screen, findTab(screen, resolvedTarget.scope(), resolvedTarget.tabId()));
        return Component.empty().append(scopeLabel).append(Component.literal(" · ")).append(tabLabel);
    }

    static String tabEditableName(PersonalDatabaseScreen screen, DatabaseTab tab) {
        if (tab == null) {
            return "";
        }
        return tab.customName().isBlank() ? tabLabel(screen, tab).getString() : tab.customName();
    }

    static ItemStack tabIcon(PersonalDatabaseScreen screen, @Nullable DatabaseTab tab) {
        if (tab == null) {
            return new ItemStack(Items.CHEST);
        }
        return resolveIconStack(screen, tab.iconItemId(), tab.isAllTab());
    }

    static ItemStack resolveIconStack(PersonalDatabaseScreen screen, String itemId, boolean allTab) {
        String fallbackItemId = allTab ? DatabaseTabs.DEFAULT_ALL_ICON_ITEM_ID : DatabaseTabs.DEFAULT_CONCRETE_ICON_ITEM_ID;
        ResourceLocation resolvedItemId = parseResourceLocation(itemId);
        if (resolvedItemId != null && BuiltInRegistries.ITEM.containsKey(resolvedItemId)) {
            Item item = BuiltInRegistries.ITEM.get(resolvedItemId);
            if (item != Items.AIR) {
                return new ItemStack(item);
            }
        }
        ResourceLocation fallbackId = parseResourceLocation(fallbackItemId);
        if (fallbackId != null && BuiltInRegistries.ITEM.containsKey(fallbackId)) {
            return new ItemStack(BuiltInRegistries.ITEM.get(fallbackId));
        }
        return new ItemStack(allTab ? Items.COMPASS : Items.WRITABLE_BOOK);
    }

    @Nullable
    static ResourceLocation parseResourceLocation(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return ResourceLocation.parse(value);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    static List<DatabasePagePickerModel.PageOption> pagePickerOptions(PersonalDatabaseScreen screen) {
        int panelIndex = activePagePickerPanelIndex(screen);
        if (panelIndex < 0 || panelIndex >= currentPanels(screen).size()) {
            return DatabasePagePickerModel.build(1, 0);
        }
        DatabasePanelView panel = currentPanels(screen).get(panelIndex);
        return DatabasePagePickerModel.build(panel.totalPages(), panel.pageIndex());
    }

    static Component pagePickerLabel(PersonalDatabaseScreen screen, DatabasePagePickerModel.PageOption option) {
        int panelIndex = activePagePickerPanelIndex(screen);
        int totalPages = 1;
        if (panelIndex >= 0 && panelIndex < currentPanels(screen).size()) {
            totalPages = currentPanels(screen).get(panelIndex).totalPages();
        }
        return switch (option.shortcutType()) {
            case FIRST -> Component.translatable("screen.infiniteinventory.page_picker.first", 1);
            case LAST -> Component.translatable("screen.infiniteinventory.page_picker.last", totalPages);
            case PAGE -> Component.literal(Integer.toString(option.pageIndex() + 1));
        };
    }

    static int selectedEntryCount(PersonalDatabaseScreen screen) {
        return screen.selectedDatabaseEntries.size();
    }

    static List<PersonalDatabaseContextMenuItem> contextMenuItems(PersonalDatabaseScreen screen) {
        return contextMenuItemsForSelectionCount(selectedEntryCount(screen));
    }

    static List<PersonalDatabaseContextMenuItem> contextMenuItemsForSelectionCount(int selectedEntryCount) {
        if (selectedEntryCount <= 0) {
            return List.of();
        }
        return selectedEntryCount > 1 ? MULTI_SELECTION_CONTEXT_MENU_ITEMS : SINGLE_SELECTION_CONTEXT_MENU_ITEMS;
    }

    static Component contextMenuLabel(PersonalDatabaseContextMenuItem item) {
        return Component.translatable(item.translationKey());
    }

    static int contextMenuWidth(PersonalDatabaseScreen screen) {
        int width = PersonalDatabaseScreen.CONTEXT_MENU_MIN_WIDTH;
        for (PersonalDatabaseContextMenuItem item : contextMenuItems(screen)) {
            width = Math.max(width, screen.screenFont().width(contextMenuLabel(item)) + 16);
        }
        return width;
    }

    static int contextMenuHeight(PersonalDatabaseScreen screen) {
        return contextMenuItems(screen).size() * PersonalDatabaseScreen.CONTEXT_MENU_ROW_HEIGHT;
    }

    static void drawCenteredShadow(
            PersonalDatabaseScreen screen,
            net.minecraft.client.gui.GuiGraphics guiGraphics,
            Component text,
            int left,
            int right,
            int y,
            int color
    ) {
        int availableWidth = Math.max(0, right - left);
        int textWidth = screen.screenFont().width(text);
        int x = left + Math.max(0, (availableWidth - textWidth) / 2);
        guiGraphics.drawString(screen.screenFont(), text, x, y, color, true);
    }

    static boolean isAdvancedToggleClickable(DatabaseSearchField field, DatabaseSearchConfig searchConfig) {
        DatabaseSearchWeight weight = searchConfig.weightFor(field);
        return !field.isTextField()
                || weight == DatabaseSearchWeight.OFF
                || PersonalDatabaseScreenWidgetHelper.enabledTextFieldCount(searchConfig) > 1;
    }

    @Nullable
    static String resolveSingleStoreTargetTabId(PersonalDatabaseScreen screen) {
        DatabaseQuery query = screen.databaseMenu.viewState().query();
        if (query.visibleTabIds().size() != 1) {
            return null;
        }
        String onlyVisibleTabId = query.visibleTabIds().getFirst();
        return DatabaseTabs.isAllTabId(onlyVisibleTabId) ? null : onlyVisibleTabId;
    }
}
