package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.database.DatabaseAutoStoreTarget;
import com.agguy.infiniteinventory.database.DatabasePanelView;
import com.agguy.infiniteinventory.database.DatabaseQuery;
import com.agguy.infiniteinventory.database.DatabaseScopedTabRef;
import com.agguy.infiniteinventory.database.DatabaseSearchConfig;
import com.agguy.infiniteinventory.database.DatabaseSearchField;
import com.agguy.infiniteinventory.database.DatabaseSearchWeight;
import com.agguy.infiniteinventory.database.DatabaseSortDirection;
import com.agguy.infiniteinventory.database.DatabaseSortMethod;
import com.agguy.infiniteinventory.database.DatabaseSortOption;
import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.DatabaseTab;
import com.agguy.infiniteinventory.database.DatabaseTabs;
import com.agguy.infiniteinventory.menu.PersonalDatabaseLayout;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

final class PersonalDatabaseScreenCommonHelper {
    private static final Logger LOGGER = LogManager.getLogger();

    private PersonalDatabaseScreenCommonHelper() {
    }

    static void playButtonClickSound(PersonalDatabaseScreen screen) {
        net.minecraft.client.Minecraft.getInstance().getSoundManager().play(
                net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK,
                        1.0F
                )
        );
    }

    static List<DatabasePanelView> currentPanels(PersonalDatabaseScreen screen) {
        List<DatabasePanelView> panels = screen.databaseMenu.viewState().panels();
        int maxVisiblePanels = Math.max(1, maxVisiblePanels(screen));
        if (panels.size() <= maxVisiblePanels) {
            return panels;
        }
        return panels.subList(0, maxVisiblePanels);
    }

    static PersonalDatabaseScreenFitProfile fitProfile(PersonalDatabaseScreen screen) {
        return screen.screenFitProfile();
    }

    static boolean supportsFullUi(PersonalDatabaseScreen screen) {
        return fitProfile(screen).supportsFullUi();
    }

    static int maxVisiblePanels(PersonalDatabaseScreen screen) {
        return fitProfile(screen).maxVisiblePanels();
    }

    static List<DatabaseScopedTabRef> clampVisibleTabsToScreen(
            PersonalDatabaseScreen screen,
            List<DatabaseScopedTabRef> visibleTabs,
            @Nullable DatabaseScopedTabRef focusedTab
    ) {
        return clampVisibleTabs(visibleTabs, focusedTab, maxVisiblePanels(screen));
    }

    static List<DatabaseScopedTabRef> clampVisibleTabs(
            List<DatabaseScopedTabRef> visibleTabs,
            @Nullable DatabaseScopedTabRef focusedTab,
            int maxVisiblePanels
    ) {
        int resolvedMaxVisiblePanels = Math.max(1, maxVisiblePanels);
        DatabaseScopedTabRef fallbackFocusedTab = focusedTab == null
                ? DatabaseScopedTabRef.defaultTab()
                : focusedTab;
        if (visibleTabs == null || visibleTabs.isEmpty()) {
            return List.of(fallbackFocusedTab);
        }
        java.util.LinkedHashSet<DatabaseScopedTabRef> clampedTabs = new java.util.LinkedHashSet<>();
        for (DatabaseScopedTabRef visibleTab : visibleTabs) {
            clampedTabs.add(visibleTab == null ? fallbackFocusedTab : visibleTab);
            if (clampedTabs.size() >= resolvedMaxVisiblePanels) {
                break;
            }
        }
        if (!clampedTabs.contains(fallbackFocusedTab) && visibleTabs.contains(fallbackFocusedTab)) {
            java.util.ArrayList<DatabaseScopedTabRef> orderedTabs = new java.util.ArrayList<>(clampedTabs);
            if (orderedTabs.isEmpty()) {
                orderedTabs.add(fallbackFocusedTab);
            } else {
                orderedTabs.set(orderedTabs.size() - 1, fallbackFocusedTab);
            }
            clampedTabs.clear();
            clampedTabs.addAll(orderedTabs);
        }
        java.util.ArrayList<DatabaseScopedTabRef> orderedVisibleTabs = new java.util.ArrayList<>(clampedTabs.size());
        for (DatabaseScopedTabRef visibleTab : visibleTabs) {
            if (clampedTabs.contains(visibleTab) && !orderedVisibleTabs.contains(visibleTab)) {
                orderedVisibleTabs.add(visibleTab);
            }
        }
        if (orderedVisibleTabs.isEmpty()) {
            orderedVisibleTabs.add(fallbackFocusedTab);
        }
        return List.copyOf(orderedVisibleTabs);
    }

    static List<DatabaseScopedTabRef> allTopTabs(PersonalDatabaseScreen screen) {
        java.util.ArrayList<DatabaseScopedTabRef> topTabs = new java.util.ArrayList<>();
        topTabs.addAll(topTabsForScope(screen, DatabaseScope.PERSONAL));
        topTabs.addAll(topTabsForScope(screen, DatabaseScope.PUBLIC));
        return List.copyOf(topTabs);
    }

    static List<DatabaseScopedTabRef> topTabsForScope(PersonalDatabaseScreen screen, DatabaseScope scope) {
        DatabaseScope normalizedScope = DatabaseScope.normalize(scope);
        java.util.ArrayList<DatabaseScopedTabRef> topTabs = new java.util.ArrayList<>();
        for (DatabaseTab tab : tabsForScope(screen, normalizedScope)) {
            topTabs.add(DatabaseScopedTabRef.concreteTab(normalizedScope, tab.id()));
        }
        return List.copyOf(topTabs);
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

    static List<DatabaseTab> concreteTabsForScope(PersonalDatabaseScreen screen, DatabaseScope scope) {
        return tabsForScope(screen, scope).stream().filter(DatabaseTab::isConcreteTab).toList();
    }

    static DatabaseTab findTab(PersonalDatabaseScreen screen, DatabaseScopedTabRef scopedTab) {
        if (scopedTab == null) {
            return DatabaseTabs.defaultConcreteTab();
        }
        return findTab(screen, scopedTab.scope(), scopedTab.tabId());
    }

    static int focusedPanelIndex(PersonalDatabaseScreen screen) {
        DatabaseScopedTabRef focusedTab = screen.databaseMenu.viewState().query().focusedTab();
        for (int index = 0; index < currentPanels(screen).size(); index++) {
            if (currentPanels(screen).get(index).scopedTab().equals(focusedTab)) {
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
        if (tab.id().startsWith("tab_")) {
            return Component.translatable(DatabaseTabs.DEFAULT_TAB_TRANSLATION_KEY);
        }
        return Component.literal(tab.id());
    }

    static Component tabLabelById(PersonalDatabaseScreen screen, String tabId) {
        return tabLabel(screen, findTab(screen, screen.databaseMenu.viewState().query().focusedTab().scope(), tabId));
    }

    static Component scopeLabel(DatabaseScope scope) {
        return Component.translatable(DatabaseScope.normalize(scope).translationKey());
    }

    static Component viewTitleLabel(PersonalDatabaseScreen screen, DatabaseScopedTabRef scopedTab) {
        DatabaseScopedTabRef resolvedScopedTab = scopedTab == null ? DatabaseScopedTabRef.defaultTab() : scopedTab;
        return Component.empty()
                .append(tabLabel(screen, findTab(screen, resolvedScopedTab)))
                .append(Component.literal(" · "))
                .append(scopeLabel(resolvedScopedTab.scope()));
    }

    static Component scopedTabLabel(PersonalDatabaseScreen screen, DatabaseScopedTabRef scopedTab) {
        return tabLabel(screen, findTab(screen, scopedTab));
    }

    static Component autoStoreTargetLabel(PersonalDatabaseScreen screen, @Nullable DatabaseAutoStoreTarget autoStoreTarget) {
        DatabaseAutoStoreTarget resolvedTarget = autoStoreTarget == null
                ? DatabaseAutoStoreTarget.defaultTarget()
                : autoStoreTarget;
        Component scopeLabel = scopeLabel(resolvedTarget.scope());
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
            LOGGER.debug("无法解析 ResourceLocation：{}", value, exception);
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

    static DatabaseSortOption sortOptionForPanel(PersonalDatabaseScreen screen, int panelIndex) {
        if (panelIndex >= 0 && panelIndex < currentPanels(screen).size()) {
            return screen.databaseMenu.viewState().query().sortOptionFor(currentPanels(screen).get(panelIndex).scopedTab());
        }
        return screen.databaseMenu.viewState().query().sortOption();
    }

    static Component sortMethodLabel(DatabaseSortMethod method) {
        DatabaseSortMethod resolvedMethod = method == null ? DatabaseSortMethod.RECENTLY_CHANGED : method;
        return Component.translatable(resolvedMethod.translationKey());
    }

    static Component sortButtonLabel(DatabaseSortOption sortOption) {
        DatabaseSortOption resolvedOption = sortOption == null ? DatabaseSortOption.RECENTLY_CHANGED : sortOption;
        return Component.translatable(resolvedOption.method().buttonTranslationKey());
    }

    static Component sortDirectionLabel(DatabaseSortDirection direction) {
        DatabaseSortDirection resolvedDirection = direction == null ? DatabaseSortDirection.DESC : direction;
        return Component.translatable(resolvedDirection.translationKey());
    }

    static int selectedEntryCount(PersonalDatabaseScreen screen) {
        return screen.selectedDatabaseEntries.size();
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
    static DatabaseScopedTabRef resolveSingleStoreTarget(PersonalDatabaseScreen screen) {
        DatabaseQuery query = screen.databaseMenu.viewState().query();
        if (query.visibleTabs().size() != 1) {
            return null;
        }
        DatabaseScopedTabRef onlyVisibleTab = query.visibleTabs().getFirst();
        return onlyVisibleTab.isSystemTab() ? null : onlyVisibleTab;
    }

    static String truncateToWidth(PersonalDatabaseScreen screen, String text, int maxWidth) {
        if (maxWidth <= 0 || screen.screenFont().width(text) <= maxWidth) {
            return text;
        }
        String suffix = "...";
        int suffixWidth = screen.screenFont().width(suffix);
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            if (screen.screenFont().width(builder.toString() + character) + suffixWidth > maxWidth) {
                break;
            }
            builder.append(character);
        }
        return builder.isEmpty() ? "" : builder.append(suffix).toString();
    }
}
