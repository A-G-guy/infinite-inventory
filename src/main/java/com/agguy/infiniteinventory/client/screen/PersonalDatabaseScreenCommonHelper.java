package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.database.DatabasePanelView;
import com.agguy.infiniteinventory.database.DatabaseQuery;
import com.agguy.infiniteinventory.database.DatabaseSearchConfig;
import com.agguy.infiniteinventory.database.DatabaseSearchField;
import com.agguy.infiniteinventory.database.DatabaseSearchWeight;
import com.agguy.infiniteinventory.database.DatabaseTab;
import com.agguy.infiniteinventory.database.DatabaseTabs;
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
    private PersonalDatabaseScreenCommonHelper() {
    }

    static List<DatabasePanelView> currentPanels(PersonalDatabaseScreen screen) {
        return screen.databaseMenu.viewState().panels();
    }

    static List<DatabaseTab> currentTabs(PersonalDatabaseScreen screen) {
        return screen.databaseMenu.viewState().tabsForScope(screen.databaseMenu.viewState().query().scope());
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
        for (DatabaseTab tab : currentTabs(screen)) {
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

    static Component contextMenuLabel(DatabaseSelectionAction action) {
        return switch (action) {
            case EXTRACT_ONE_TO_INVENTORY -> Component.translatable("screen.infiniteinventory.selection.take_one_each");
            case EXTRACT_HALF_STACK_TO_INVENTORY -> Component.translatable("screen.infiniteinventory.selection.take_half_stack_each");
            case EXTRACT_STACK_TO_INVENTORY -> Component.translatable("screen.infiniteinventory.selection.take_stack_each");
            case EXTRACT_ALL_TO_INVENTORY -> Component.translatable("screen.infiniteinventory.selection.take_all_each");
            case TRANSFER_TO_TAB -> Component.translatable("screen.infiniteinventory.selection.transfer");
        };
    }

    static int contextMenuWidth(PersonalDatabaseScreen screen) {
        int width = PersonalDatabaseScreen.CONTEXT_MENU_MIN_WIDTH;
        for (DatabaseSelectionAction action : PersonalDatabaseScreen.CONTEXT_MENU_ACTIONS) {
            width = Math.max(width, screen.screenFont().width(contextMenuLabel(action)) + 16);
        }
        return width;
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
        guiGraphics.drawString(screen.screenFont(), text, x, y, color, false);
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
