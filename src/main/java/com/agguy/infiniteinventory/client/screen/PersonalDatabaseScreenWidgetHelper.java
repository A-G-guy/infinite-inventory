package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.database.DatabaseAutoStoreTarget;
import com.agguy.infiniteinventory.database.DatabaseEnhancementConfig;
import com.agguy.infiniteinventory.database.DatabaseEnhancementOption;
import com.agguy.infiniteinventory.database.DatabasePanelView;
import com.agguy.infiniteinventory.database.DatabaseQuery;
import com.agguy.infiniteinventory.database.DatabaseScopedTabRef;
import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.DatabaseSearchConfig;
import com.agguy.infiniteinventory.database.DatabaseSearchField;
import com.agguy.infiniteinventory.database.DatabaseSearchWeight;
import com.agguy.infiniteinventory.database.DatabaseViewState;
import com.agguy.infiniteinventory.menu.PersonalDatabaseLayout;
import com.agguy.infiniteinventory.network.DatabaseLogRequestPayload;
import com.agguy.infiniteinventory.network.DatabaseStatisticsRequestPayload;
import com.agguy.infiniteinventory.network.DepositAllPayload;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

final class PersonalDatabaseScreenWidgetHelper {
    private PersonalDatabaseScreenWidgetHelper() {
    }

    static void buildWidgets(PersonalDatabaseScreen screen) {
        if (screen.layout == null) {
            return;
        }
        screen.personalScopeButton = null;
        screen.publicScopeButton = null;
        buildPanelWidgets(screen);

        buildAdvancedSearchButtons(screen);
        buildEnhancementButtons(screen);

        PersonalDatabaseLayout.Rect settingsRect = screen.layout.settingsButtonRect();
        screen.settingsButton = screen.addScreenButton(IconButton.create(
                settingsRect.x(), settingsRect.y(), settingsRect.width(), settingsRect.height(),
                Component.translatable("screen.infiniteinventory.settings_button"),
                button -> {
                    PersonalDatabaseScreenTargetHelper.closeTransientOverlays(screen);
                    screen.settingsPanelExpanded = true;
                    screen.activeSettingsTab = PersonalDatabaseScreenEnums.SettingsPanelTab.ADVANCED_SEARCH;
                    PersonalDatabaseScreenSettingsHelper.syncSettingsSubPanelStates(screen);
                },
                RemixIcon.SETTINGS
        ));

        PersonalDatabaseLayout.Rect viewSelectorRect = screen.layout.viewSelectorButtonRect();
        if (viewSelectorRect.width() > 0 && viewSelectorRect.height() > 0) {
            screen.viewSelectorButton = screen.addScreenButton(IconButton.create(
                    viewSelectorRect.x(), viewSelectorRect.y(), viewSelectorRect.width(), viewSelectorRect.height(),
                    Component.translatable("screen.infiniteinventory.view_selector_button"),
                    button -> {
                        PersonalDatabaseScreenTargetHelper.closeTransientOverlays(screen);
                        screen.viewSelectorExpanded = !screen.viewSelectorExpanded;
                        if (screen.viewSelectorExpanded) {
                            screen.viewSelectorPersonalScrollIndex = 0;
                            screen.viewSelectorPublicScrollIndex = 0;
                        }
                    },
                    RemixIcon.VIEWS
            ));
        }

        PersonalDatabaseLayout.Rect statisticsRect = screen.layout.statisticsButtonRect();
        if (statisticsRect.width() > 0 && statisticsRect.height() > 0) {
            screen.statisticsButton = screen.addScreenButton(IconButton.create(
                    statisticsRect.x(), statisticsRect.y(), statisticsRect.width(), statisticsRect.height(),
                    Component.translatable("screen.infiniteinventory.statistics_button"),
                    button -> {
                        PersonalDatabaseScreenTargetHelper.closeTransientOverlays(screen);
                        screen.statisticsPanelExpanded = !screen.statisticsPanelExpanded;
                        if (screen.statisticsPanelExpanded) {
                            screen.statisticsPanelScope = screen.databaseMenu.viewState().query().focusedTab().scope();
                            screen.statisticsCategoryScrollIndex = 0;
                            screen.statisticsModsScrollIndex = 0;
                            screen.statisticsTabsScrollIndex = 0;
                            screen.statisticsTrendsScrollIndex = 0;
                            screen.statisticsLogScrollIndex = 0;
                            PacketDistributor.sendToServer(
                                    new DatabaseStatisticsRequestPayload(screen.statisticsPanelScope));
                            PacketDistributor.sendToServer(
                                    new DatabaseLogRequestPayload(screen.statisticsPanelScope));
                        }
                    },
                    RemixIcon.STATISTICS
            ));
        }

        PersonalDatabaseLayout.Rect personalScopeRect = screen.layout.personalScopeButtonRect();
        screen.personalScopeButton = screen.addScreenButton(IconButton.create(
                personalScopeRect.x(),
                personalScopeRect.y(),
                personalScopeRect.width(),
                personalScopeRect.height(),
                Component.translatable(DatabaseScope.PERSONAL.translationKey()),
                button -> {
                    DatabaseScope currentScope = PersonalDatabaseScreenTabHelper.syncTopTabScopeFilter(screen);
                    DatabaseScope nextScope = currentScope == DatabaseScope.PUBLIC
                            ? DatabaseScope.PERSONAL
                            : DatabaseScope.PUBLIC;
                    PersonalDatabaseScreenTabHelper.switchTopTabScopeFilter(screen, nextScope);
                },
                RemixIcon.SCOPE_PERSONAL
        ));

        PersonalDatabaseLayout.Rect publicScopeRect = screen.layout.publicScopeButtonRect();
        if (publicScopeRect.width() > 0 && publicScopeRect.height() > 0) {
            screen.publicScopeButton = screen.addScreenButton(IconButton.create(
                    publicScopeRect.x(), publicScopeRect.y(), publicScopeRect.width(), publicScopeRect.height(),
                    Component.translatable(DatabaseScope.PUBLIC.translationKey()),
                    button -> PersonalDatabaseScreenTabHelper.switchTopTabScopeFilter(screen, DatabaseScope.PUBLIC),
                    RemixIcon.SCOPE_PUBLIC
            ));
        }

        PersonalDatabaseLayout.Rect depositExistingRect = screen.layout.depositExistingButtonRect();
        screen.depositExistingButton = screen.addScreenButton(IconButton.create(
                depositExistingRect.x(), depositExistingRect.y(), depositExistingRect.width(), depositExistingRect.height(),
                Component.translatable("screen.infiniteinventory.deposit_existing"),
                button -> {
                    PersonalDatabaseScreenContextHelper.closeContextMenu(screen);
                    screen.sortDropdownExpanded = false;
                    screen.pagePickerExpanded = false;
                    screen.enhancementPanelExpanded = false;
                    PersonalDatabaseScreenTabHelper.closeTopTabPrompt(screen);
                    PersonalDatabaseScreenTargetHelper.openTargetSelector(
                            screen,
                            PersonalDatabaseScreenEnums.TargetSelectorMode.DEPOSIT_EXISTING_BY_TAB,
                            -1, -1, ""
                    );
                },
                RemixIcon.DEPOSIT_EXISTING
        ));

        PersonalDatabaseLayout.Rect depositRect = screen.layout.depositButtonRect();
        screen.depositButton = screen.addScreenButton(IconButton.create(
                depositRect.x(), depositRect.y(), depositRect.width(), depositRect.height(),
                Component.translatable("screen.infiniteinventory.deposit_all"),
                button -> {
                    PersonalDatabaseScreenContextHelper.closeContextMenu(screen);
                    screen.sortDropdownExpanded = false;
                    screen.pagePickerExpanded = false;
                    screen.enhancementPanelExpanded = false;
                    PersonalDatabaseScreenTabHelper.closeTopTabPrompt(screen);
                    DatabaseScopedTabRef directTarget = PersonalDatabaseScreenCommonHelper.resolveSingleStoreTarget(screen);
                    if (directTarget != null) {
                        PacketDistributor.sendToServer(new DepositAllPayload(
                                screen.databaseMenu.containerId,
                                screen.databaseMenu.viewState().sessionId(),
                                directTarget.scope(),
                                directTarget.tabId()
                        ));
                    } else {
                        PersonalDatabaseScreenTargetHelper.openTargetSelector(
                                screen,
                                PersonalDatabaseScreenEnums.TargetSelectorMode.DEPOSIT_ALL,
                                -1,
                                -1,
                                ""
                        );
                    }
                },
                RemixIcon.DEPOSIT
        ));

        screen.accessoriesToggleButton = null;
        if (PersonalDatabaseScreenLayoutHelper.hasAccessorySlots(screen) && screen.layout.accessoryToggleRect().height() > 0) {
            PersonalDatabaseLayout.Rect accessoryToggleRect = screen.layout.accessoryToggleRect();
            screen.accessoriesToggleButton = screen.addScreenButton(IconButton.create(
                    accessoryToggleRect.x(),
                    accessoryToggleRect.y(),
                    accessoryToggleRect.width(),
                    accessoryToggleRect.height(),
                    Component.empty(),
                    button -> PersonalDatabaseScreenLayoutHelper.toggleAccessoriesPanel(screen),
                    RemixIcon.ACCESSORIES
            ));
        }
        PersonalDatabaseScreenManagementHelper.ensureManagementWidgets(screen);
    }

    static void buildAdvancedSearchButtons(PersonalDatabaseScreen screen) {
        screen.advancedSearchToggleButtons.clear();
        screen.advancedSearchWeightButtons.clear();
        if (screen.layout == null) {
            return;
        }
        for (DatabaseSearchField field : DatabaseSearchField.values()) {
            PersonalDatabaseLayout.Rect rowRect = PersonalDatabaseScreenGeometry.advancedSearchRowRect(screen, field);
            Button toggleButton = screen.addScreenButton(IconButton.create(
                    rowRect.x(),
                    rowRect.y(),
                    PersonalDatabaseScreen.ADVANCED_SEARCH_TOGGLE_WIDTH,
                    PersonalDatabaseScreen.ADVANCED_SEARCH_ROW_HEIGHT,
                    Component.empty(),
                    button -> PersonalDatabaseScreenConfigActionHelper.toggleAdvancedSearchField(screen, field),
                    RemixIcon.SEARCH_TOGGLE
            ));
            Button weightButton = screen.addScreenButton(IconButton.create(
                    rowRect.right() - PersonalDatabaseScreen.ADVANCED_SEARCH_WEIGHT_WIDTH,
                    rowRect.y(),
                    PersonalDatabaseScreen.ADVANCED_SEARCH_WEIGHT_WIDTH,
                    PersonalDatabaseScreen.ADVANCED_SEARCH_ROW_HEIGHT,
                    Component.empty(),
                    button -> PersonalDatabaseScreenConfigActionHelper.cycleAdvancedSearchWeight(screen, field),
                    RemixIcon.SEARCH_WEIGHT
            ));
            screen.advancedSearchToggleButtons.put(field, toggleButton);
            screen.advancedSearchWeightButtons.put(field, weightButton);
        }
    }

    static void buildEnhancementButtons(PersonalDatabaseScreen screen) {
        screen.enhancementToggleButtons.clear();
        if (screen.layout == null) {
            return;
        }
        for (DatabaseEnhancementOption option : DatabaseEnhancementOption.orderedValues()) {
            PersonalDatabaseLayout.Rect rowRect = PersonalDatabaseScreenGeometry.enhancementRowRect(screen, option);
            Button toggleButton = screen.addScreenButton(IconButton.create(
                    rowRect.x(),
                    rowRect.y(),
                    PersonalDatabaseScreen.ENHANCEMENT_TOGGLE_WIDTH,
                    PersonalDatabaseScreen.ENHANCEMENT_ROW_HEIGHT,
                    Component.empty(),
                    button -> PersonalDatabaseScreenConfigActionHelper.toggleEnhancementOption(screen, option),
                    RemixIcon.ENHANCEMENT
            ));
            screen.enhancementToggleButtons.put(option, toggleButton);
        }
    }

    static void syncAdvancedSearchButtons(PersonalDatabaseScreen screen, DatabaseQuery query) {
        DatabaseSearchConfig searchConfig = query.searchConfig();
        int enabledTextFieldCount = enabledTextFieldCount(searchConfig);
        boolean visible = screen.settingsPanelExpanded && screen.activeSettingsTab == PersonalDatabaseScreenEnums.SettingsPanelTab.ADVANCED_SEARCH;
        for (DatabaseSearchField field : DatabaseSearchField.values()) {
            DatabaseSearchWeight weight = searchConfig.weightFor(field);
            PersonalDatabaseLayout.Rect rowRect = PersonalDatabaseScreenGeometry.advancedSearchRowRect(screen, field);
            Button toggleButton = screen.advancedSearchToggleButtons.get(field);
            if (toggleButton != null) {
                toggleButton.setPosition(rowRect.x(), rowRect.y());
                toggleButton.visible = visible;
                toggleButton.active = !field.isTextField()
                        || weight == DatabaseSearchWeight.OFF
                        || enabledTextFieldCount > 1;
                toggleButton.setMessage(Component.empty());
            }
            PersonalDatabaseLayout.Rect weightRect = new PersonalDatabaseLayout.Rect(
                    rowRect.right() - PersonalDatabaseScreen.ADVANCED_SEARCH_WEIGHT_WIDTH,
                    rowRect.y(),
                    PersonalDatabaseScreen.ADVANCED_SEARCH_WEIGHT_WIDTH,
                    PersonalDatabaseScreen.ADVANCED_SEARCH_ROW_HEIGHT
            );
            Button weightButton = screen.advancedSearchWeightButtons.get(field);
            if (weightButton != null) {
                weightButton.setPosition(weightRect.x(), weightRect.y());
                weightButton.visible = visible;
                weightButton.active = weight != DatabaseSearchWeight.OFF;
                weightButton.setMessage(Component.empty());
            }
        }
    }

    static void syncEnhancementButtons(PersonalDatabaseScreen screen, DatabaseEnhancementConfig config) {
        boolean visible = screen.settingsPanelExpanded && screen.activeSettingsTab == PersonalDatabaseScreenEnums.SettingsPanelTab.ENHANCEMENT;
        for (DatabaseEnhancementOption option : DatabaseEnhancementOption.orderedValues()) {
            PersonalDatabaseLayout.Rect rowRect = PersonalDatabaseScreenGeometry.enhancementRowRect(screen, option);
            Button toggleButton = screen.enhancementToggleButtons.get(option);
            if (toggleButton != null) {
                toggleButton.setPosition(rowRect.x(), rowRect.y());
                toggleButton.visible = visible;
                toggleButton.active = true;
                toggleButton.setMessage(Component.empty());
            }
        }
    }

    static int enabledTextFieldCount(DatabaseSearchConfig searchConfig) {
        int count = 0;
        for (DatabaseSearchField field : DatabaseSearchField.values()) {
            if (field.isTextField() && searchConfig.weightFor(field) != DatabaseSearchWeight.OFF) {
                count++;
            }
        }
        return count;
    }

    static void syncWidgetsFromState(PersonalDatabaseScreen screen) {
        DatabaseViewState viewState = screen.databaseMenu.viewState();
        DatabaseQuery query = viewState.query();
        DatabaseScope topTabScopeFilter = PersonalDatabaseScreenTabHelper.syncTopTabScopeFilter(screen);
        syncPanelWidgets(screen, viewState, query);
        if (screen.settingsButton != null) {
            screen.settingsButton.setMessage(Component.translatable("screen.infiniteinventory.settings_button"));
            screen.settingsButton.visible = screen.layout != null && screen.layout.settingsButtonRect().width() > 0;
        }
        if (screen.viewSelectorButton != null) {
            PersonalDatabaseLayout.Rect viewSelectorRect = screen.layout != null ? screen.layout.viewSelectorButtonRect() : PersonalDatabaseLayout.Rect.empty();
            screen.viewSelectorButton.setPosition(viewSelectorRect.x(), viewSelectorRect.y());
            screen.viewSelectorButton.visible = viewSelectorRect.width() > 0 && viewSelectorRect.height() > 0;
            screen.viewSelectorButton.active = true;
            screen.viewSelectorButton.setMessage(Component.translatable("screen.infiniteinventory.view_selector_button"));
        }
        if (screen.statisticsButton != null) {
            PersonalDatabaseLayout.Rect statisticsRect = screen.layout != null ? screen.layout.statisticsButtonRect() : PersonalDatabaseLayout.Rect.empty();
            screen.statisticsButton.setPosition(statisticsRect.x(), statisticsRect.y());
            screen.statisticsButton.visible = statisticsRect.width() > 0 && statisticsRect.height() > 0;
            screen.statisticsButton.active = true;
            screen.statisticsButton.setMessage(Component.translatable("screen.infiniteinventory.statistics_button"));
        }
        if (screen.depositExistingButton != null) {
            screen.depositExistingButton.active = screen.minecraftClient() != null && screen.minecraftClient().player != null;
        }
        if (screen.depositButton != null) {
            screen.depositButton.active = screen.minecraftClient() != null && screen.minecraftClient().player != null;
        }
        if (screen.personalScopeButton != null) {
            screen.personalScopeButton.visible = screen.layout != null && screen.layout.personalScopeButtonRect().width() > 0;
            DatabaseScope nextScope = topTabScopeFilter == DatabaseScope.PUBLIC ? DatabaseScope.PERSONAL : DatabaseScope.PUBLIC;
            screen.personalScopeButton.active = !PersonalDatabaseScreenCommonHelper.topTabsForScope(screen, nextScope).isEmpty();
            screen.personalScopeButton.setMessage(Component.translatable(topTabScopeFilter.translationKey()));
            if (screen.personalScopeButton instanceof IconButton iconButton) {
                iconButton.setIcon(topTabScopeFilter == DatabaseScope.PERSONAL ? RemixIcon.SCOPE_PERSONAL : RemixIcon.SCOPE_PUBLIC);
            }
        }
        if (screen.publicScopeButton != null) {
            screen.publicScopeButton.visible = screen.layout != null && screen.layout.publicScopeButtonRect().width() > 0;
            screen.publicScopeButton.active = topTabScopeFilter != DatabaseScope.PUBLIC;
            screen.publicScopeButton.setMessage(Component.translatable(DatabaseScope.PUBLIC.translationKey()));
        }
        if (screen.accessoriesToggleButton != null) {
            screen.accessoriesToggleButton.visible = PersonalDatabaseScreenLayoutHelper.hasAccessorySlots(screen)
                    && screen.layout != null
                    && screen.layout.accessoryToggleRect().height() > 0;
            screen.accessoriesToggleButton.active = PersonalDatabaseScreenLayoutHelper.hasAccessorySlots(screen);
            screen.accessoriesToggleButton.setMessage(Component.translatable(
                    screen.accessoriesExpanded
                            ? "screen.infiniteinventory.accessories_toggle.collapse"
                            : "screen.infiniteinventory.accessories_toggle.expand"
            ));
        }
        syncAdvancedSearchButtons(screen, query);
        syncEnhancementButtons(screen, viewState.enhancementConfig());
        PersonalDatabaseScreenManagementHelper.syncManagementWidgets(screen);
        PersonalDatabaseScreenSelectionHelper.syncSelectionWithViewState(screen);
        if (!screen.databaseMenu.getCarried().isEmpty()) {
            PersonalDatabaseScreenTargetHelper.closeTransientOverlays(screen);
            return;
        }
        PersonalDatabaseScreenContextHelper.validateContextMenu(screen, viewState);
    }

    private static void buildPanelWidgets(PersonalDatabaseScreen screen) {
        screen.panelSearchBoxes.clear();
        screen.panelSortButtons.clear();
        screen.panelPreviousPageButtons.clear();
        screen.panelPageButtons.clear();
        screen.panelNextPageButtons.clear();
        if (screen.layout == null) {
            return;
        }
        for (int panelIndex = 0; panelIndex < PersonalDatabaseScreenCommonHelper.currentPanels(screen).size(); panelIndex++) {
            DatabasePanelView panel = PersonalDatabaseScreenCommonHelper.currentPanels(screen).get(panelIndex);
            String tabId = panel.tab().id();
            PersonalDatabaseLayout.Rect searchRect = PersonalDatabaseScreenGeometry.panelSearchFieldRect(screen, panelIndex);
            EditBox searchBox = screen.addScreenEditBox(new EditBox(
                    screen.screenFont(),
                    searchRect.x() + PersonalDatabaseScreen.SEARCH_TEXT_LEFT_PADDING,
                    searchRect.y() + 4,
                    Math.max(
                            1,
                            searchRect.width()
                                    - PersonalDatabaseScreen.SEARCH_TEXT_LEFT_PADDING
                                    - PersonalDatabaseScreen.TEXT_FIELD_RIGHT_PADDING
                    ),
                    12,
                    Component.translatable("screen.infiniteinventory.search")
            ));
            searchBox.setMaxLength(DatabaseQuery.MAX_SEARCH_LENGTH);
            searchBox.setBordered(false);
            searchBox.setTextColor(PersonalDatabaseScreen.TEXT_FIELD_TEXT_COLOR);
            searchBox.setTextColorUneditable(PersonalDatabaseScreen.TEXT_FIELD_MUTED_TEXT_COLOR);
            searchBox.setValue(screen.databaseMenu.viewState().query().searchTextFor(tabId));
            final int resolvedPanelIndex = panelIndex;
            searchBox.setResponder(value -> PersonalDatabaseScreenLayoutHelper.onPanelSearchChanged(screen, resolvedPanelIndex, value));
            screen.panelSearchBoxes.add(searchBox);
            PersonalDatabaseLayout.Rect sortRect = PersonalDatabaseScreenGeometry.panelSortButtonRect(screen, panelIndex);
            screen.panelSortButtons.add(screen.addScreenButton(IconButton.create(
                    sortRect.x(), sortRect.y(), sortRect.width(), sortRect.height(),
                    Component.empty(),
                    button -> {
                        PersonalDatabaseScreenContextHelper.closeContextMenu(screen);
                        screen.pagePickerExpanded = false;
                        screen.activePagePickerPanelIndex = -1;
                        boolean samePanel = screen.sortDropdownExpanded && screen.activeSortPanelIndex == resolvedPanelIndex;
                        screen.activeSortPanelIndex = resolvedPanelIndex;
                        screen.sortDropdownExpanded = !samePanel;
                    },
                    RemixIcon.SORT
            )));
            PersonalDatabaseLayout.Rect previousRect = PersonalDatabaseScreenGeometry.panelPreviousPageButtonRect(screen, panelIndex);
            screen.panelPreviousPageButtons.add(screen.addScreenButton(IconButton.create(
                    previousRect.x(), previousRect.y(), previousRect.width(), previousRect.height(),
                    Component.empty(),
                    button -> PersonalDatabaseScreenLayoutHelper.changePanelPage(screen, resolvedPanelIndex, -1),
                    RemixIcon.PAGE_PREVIOUS
            )));
            PersonalDatabaseLayout.Rect pageRect = PersonalDatabaseScreenGeometry.panelPageButtonRect(screen, panelIndex);
            screen.panelPageButtons.add(screen.addScreenButton(IconButton.create(
                    pageRect.x(), pageRect.y(), pageRect.width(), pageRect.height(),
                    Component.empty(),
                    button -> {
                        PersonalDatabaseScreenContextHelper.closeContextMenu(screen);
                        screen.sortDropdownExpanded = false;
                        screen.activeSortPanelIndex = -1;
                        boolean samePanel = screen.pagePickerExpanded && screen.activePagePickerPanelIndex == resolvedPanelIndex;
                        screen.activePagePickerPanelIndex = resolvedPanelIndex;
                        screen.pagePickerExpanded = !samePanel;
                    },
                    RemixIcon.PAGE_INDICATOR
            )));
            PersonalDatabaseLayout.Rect nextRect = PersonalDatabaseScreenGeometry.panelNextPageButtonRect(screen, panelIndex);
            screen.panelNextPageButtons.add(screen.addScreenButton(IconButton.create(
                    nextRect.x(), nextRect.y(), nextRect.width(), nextRect.height(),
                    Component.empty(),
                    button -> PersonalDatabaseScreenLayoutHelper.changePanelPage(screen, resolvedPanelIndex, 1),
                    RemixIcon.PAGE_NEXT
            )));
        }
    }

    private static void syncPanelWidgets(PersonalDatabaseScreen screen, DatabaseViewState viewState, DatabaseQuery query) {
        if (screen.activeSortPanelIndex >= PersonalDatabaseScreenCommonHelper.currentPanels(screen).size()) {
            screen.activeSortPanelIndex = -1;
            screen.sortDropdownExpanded = false;
        }
        if (screen.activePagePickerPanelIndex >= PersonalDatabaseScreenCommonHelper.currentPanels(screen).size()) {
            screen.activePagePickerPanelIndex = -1;
            screen.pagePickerExpanded = false;
        }
        for (int panelIndex = 0; panelIndex < PersonalDatabaseScreenCommonHelper.currentPanels(screen).size(); panelIndex++) {
            DatabasePanelView panel = PersonalDatabaseScreenCommonHelper.currentPanels(screen).get(panelIndex);
            String tabId = panel.tab().id();
            var scopedTab = panel.scopedTab();
            EditBox searchBox = panelIndex < screen.panelSearchBoxes.size() ? screen.panelSearchBoxes.get(panelIndex) : null;
            String pendingSearchText = screen.pendingSearchTexts.get(scopedTab);
            String resolvedSearchText = pendingSearchText == null ? query.searchTextFor(tabId) : pendingSearchText;
            if (searchBox != null && !searchBox.getValue().equals(resolvedSearchText) && (!searchBox.isFocused() || pendingSearchText == null)) {
                screen.syncingSearchBox = true;
                searchBox.setValue(resolvedSearchText);
                screen.syncingSearchBox = false;
            }
            if (searchBox != null && scopedTab.equals(screen.activeSearchTab) && !searchBox.isFocused()) {
                screen.focusScreen(searchBox);
                searchBox.setFocused(true);
            }
            Button sortButton = panelIndex < screen.panelSortButtons.size() ? screen.panelSortButtons.get(panelIndex) : null;
            if (sortButton != null) {
                sortButton.setMessage(Component.empty());
            }
            Button previousButton = panelIndex < screen.panelPreviousPageButtons.size()
                    ? screen.panelPreviousPageButtons.get(panelIndex)
                    : null;
            if (previousButton != null) {
                previousButton.active = panel.pageIndex() > 0;
            }
            Button pageButton = panelIndex < screen.panelPageButtons.size() ? screen.panelPageButtons.get(panelIndex) : null;
            if (pageButton != null) {
                pageButton.setMessage(Component.translatable(
                        "screen.infiniteinventory.page_compact",
                        panel.pageIndex() + 1,
                        panel.totalPages()
                ));
            }
            Button nextButton = panelIndex < screen.panelNextPageButtons.size() ? screen.panelNextPageButtons.get(panelIndex) : null;
            if (nextButton != null) {
                nextButton.active = panel.pageIndex() + 1 < panel.totalPages();
            }
        }
    }
}
