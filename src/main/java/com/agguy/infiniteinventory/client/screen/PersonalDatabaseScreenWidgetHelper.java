package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.database.DatabaseEnhancementConfig;
import com.agguy.infiniteinventory.database.DatabaseEnhancementOption;
import com.agguy.infiniteinventory.database.DatabaseQuery;
import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.DatabaseSearchConfig;
import com.agguy.infiniteinventory.database.DatabaseSearchField;
import com.agguy.infiniteinventory.database.DatabaseSearchWeight;
import com.agguy.infiniteinventory.database.DatabaseTabs;
import com.agguy.infiniteinventory.database.DatabaseViewState;
import com.agguy.infiniteinventory.menu.PersonalDatabaseLayout;
import com.agguy.infiniteinventory.network.DatabaseEnhancementPayload;
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
        DatabaseQuery query = screen.databaseMenu.viewState().query();
        PersonalDatabaseLayout.Rect searchRect = screen.layout.searchFieldRect();
        screen.searchBox = screen.addScreenEditBox(new EditBox(
                screen.screenFont(),
                searchRect.x() + PersonalDatabaseScreen.SEARCH_TEXT_LEFT_PADDING,
                searchRect.y() + 4,
                Math.max(1, searchRect.width() - PersonalDatabaseScreen.SEARCH_TEXT_LEFT_PADDING - 4),
                12,
                Component.translatable("screen.infiniteinventory.search")
        ));
        screen.searchBox.setMaxLength(DatabaseQuery.MAX_SEARCH_LENGTH);
        screen.searchBox.setBordered(false);
        screen.searchBox.setTextColor(0x303030);
        screen.searchBox.setTextColorUneditable(0x606060);
        screen.syncingSearchBox = true;
        screen.searchBox.setValue(query.searchText());
        screen.syncingSearchBox = false;
        screen.searchBox.setResponder(value -> PersonalDatabaseScreenLayoutHelper.onSearchChanged(screen, value));

        PersonalDatabaseLayout.Rect advancedSearchRect = screen.layout.advancedSearchButtonRect();
        screen.advancedSearchButton = screen.addScreenButton(Button.builder(
                        Component.translatable("screen.infiniteinventory.search_advanced_button"),
                        button -> {
                            PersonalDatabaseScreenContextHelper.closeContextMenu(screen);
                            screen.sortDropdownExpanded = false;
                            screen.pagePickerExpanded = false;
                            screen.enhancementPanelExpanded = false;
                            screen.advancedSearchExpanded = !screen.advancedSearchExpanded;
                        }
                )
                .bounds(
                        advancedSearchRect.x(),
                        advancedSearchRect.y(),
                        advancedSearchRect.width(),
                        advancedSearchRect.height()
                )
                .build());
        buildAdvancedSearchButtons(screen);

        PersonalDatabaseLayout.Rect enhancementRect = screen.layout.enhancementButtonRect();
        screen.enhancementButton = screen.addScreenButton(Button.builder(
                        Component.translatable("screen.infiniteinventory.enhancement_button"),
                        button -> {
                            PersonalDatabaseScreenContextHelper.closeContextMenu(screen);
                            screen.sortDropdownExpanded = false;
                            screen.pagePickerExpanded = false;
                            screen.viewSelectorExpanded = false;
                            screen.moreTabsExpanded = false;
                            screen.targetSelectorExpanded = false;
                            screen.tabManagementExpanded = false;
                            screen.advancedSearchExpanded = false;
                            screen.enhancementPanelExpanded = !screen.enhancementPanelExpanded;
                        }
                )
                .bounds(enhancementRect.x(), enhancementRect.y(), enhancementRect.width(), enhancementRect.height())
                .build());
        buildEnhancementButtons(screen);

        PersonalDatabaseLayout.Rect viewSelectorRect = screen.layout.viewSelectorButtonRect();
        screen.viewSelectorButton = screen.addScreenButton(Button.builder(
                        Component.translatable("screen.infiniteinventory.visible_tabs_button"),
                        button -> {
                            PersonalDatabaseScreenContextHelper.closeContextMenu(screen);
                            screen.sortDropdownExpanded = false;
                            screen.pagePickerExpanded = false;
                            screen.advancedSearchExpanded = false;
                            screen.enhancementPanelExpanded = false;
                            screen.moreTabsExpanded = false;
                            screen.tabManagementExpanded = false;
                            screen.targetSelectorExpanded = false;
                            screen.viewSelectorExpanded = !screen.viewSelectorExpanded;
                        }
                )
                .bounds(viewSelectorRect.x(), viewSelectorRect.y(), viewSelectorRect.width(), viewSelectorRect.height())
                .build());

        PersonalDatabaseLayout.Rect tabManagementRect = screen.layout.tabManagementButtonRect();
        screen.tabManagementButton = screen.addScreenButton(Button.builder(
                        Component.translatable("screen.infiniteinventory.tab_management_button"),
                        button -> {
                            PersonalDatabaseScreenContextHelper.closeContextMenu(screen);
                            screen.sortDropdownExpanded = false;
                            screen.pagePickerExpanded = false;
                            screen.advancedSearchExpanded = false;
                            screen.enhancementPanelExpanded = false;
                            screen.moreTabsExpanded = false;
                            screen.targetSelectorExpanded = false;
                            screen.viewSelectorExpanded = false;
                            boolean nextExpanded = !screen.tabManagementExpanded;
                            PersonalDatabaseScreenManagementHelper.closeTabManagementOverlays(screen);
                            screen.tabManagementExpanded = nextExpanded;
                            PersonalDatabaseScreenManagementHelper.ensureManagementWidgets(screen);
                            if (screen.tabManagementExpanded) {
                                PersonalDatabaseScreenManagementHelper.loadManagementDrafts(
                                        screen,
                                        PersonalDatabaseScreenManagementHelper.preferredManagementTab(screen)
                                );
                            }
                        }
                )
                .bounds(tabManagementRect.x(), tabManagementRect.y(), tabManagementRect.width(), tabManagementRect.height())
                .build());

        PersonalDatabaseLayout.Rect sortRect = screen.layout.sortButtonRect();
        screen.sortButton = screen.addScreenButton(Button.builder(
                        Component.empty(),
                        button -> {
                            PersonalDatabaseScreenContextHelper.closeContextMenu(screen);
                            screen.advancedSearchExpanded = false;
                            screen.enhancementPanelExpanded = false;
                            screen.viewSelectorExpanded = false;
                            screen.moreTabsExpanded = false;
                            screen.targetSelectorExpanded = false;
                            screen.tabManagementExpanded = false;
                            screen.pagePickerExpanded = false;
                            screen.sortDropdownExpanded = !screen.sortDropdownExpanded;
                        }
                )
                .bounds(sortRect.x(), sortRect.y(), sortRect.width(), sortRect.height())
                .build());

        PersonalDatabaseLayout.Rect personalScopeRect = screen.layout.personalScopeButtonRect();
        screen.personalScopeButton = screen.addScreenButton(Button.builder(
                        Component.translatable(DatabaseScope.PERSONAL.translationKey()),
                        button -> PersonalDatabaseScreenLayoutHelper.switchScope(screen, DatabaseScope.PERSONAL)
                )
                .bounds(
                        personalScopeRect.x(),
                        personalScopeRect.y(),
                        personalScopeRect.width(),
                        personalScopeRect.height()
                )
                .build());

        PersonalDatabaseLayout.Rect publicScopeRect = screen.layout.publicScopeButtonRect();
        screen.publicScopeButton = screen.addScreenButton(Button.builder(
                        Component.translatable(DatabaseScope.PUBLIC.translationKey()),
                        button -> PersonalDatabaseScreenLayoutHelper.switchScope(screen, DatabaseScope.PUBLIC)
                )
                .bounds(publicScopeRect.x(), publicScopeRect.y(), publicScopeRect.width(), publicScopeRect.height())
                .build());

        PersonalDatabaseLayout.Rect depositRect = screen.layout.depositButtonRect();
        screen.depositButton = screen.addScreenButton(Button.builder(
                        Component.translatable("screen.infiniteinventory.deposit_all"),
                        button -> {
                            PersonalDatabaseScreenContextHelper.closeContextMenu(screen);
                            screen.sortDropdownExpanded = false;
                            screen.pagePickerExpanded = false;
                            screen.enhancementPanelExpanded = false;
                            String directTargetTabId = PersonalDatabaseScreenCommonHelper.resolveSingleStoreTargetTabId(screen);
                            if (directTargetTabId != null) {
                                PacketDistributor.sendToServer(new DepositAllPayload(
                                        screen.databaseMenu.containerId,
                                        screen.databaseMenu.viewState().sessionId(),
                                        directTargetTabId
                                ));
                            } else {
                                PersonalDatabaseScreenTargetHelper.openTargetSelector(
                                        screen,
                                        PersonalDatabaseScreen.TargetSelectorMode.DEPOSIT_ALL,
                                        -1,
                                        -1,
                                        ""
                                );
                            }
                        }
                )
                .bounds(depositRect.x(), depositRect.y(), depositRect.width(), depositRect.height())
                .build());

        PersonalDatabaseLayout.Rect previousRect = screen.layout.previousPageButtonRect();
        screen.previousPageButton = screen.addScreenButton(Button.builder(
                        Component.literal("<"),
                        button -> PersonalDatabaseScreenLayoutHelper.changePage(screen, -1)
                )
                .bounds(previousRect.x(), previousRect.y(), previousRect.width(), previousRect.height())
                .build());

        PersonalDatabaseLayout.Rect nextRect = screen.layout.nextPageButtonRect();
        screen.nextPageButton = screen.addScreenButton(Button.builder(
                        Component.literal(">"),
                        button -> PersonalDatabaseScreenLayoutHelper.changePage(screen, 1)
                )
                .bounds(nextRect.x(), nextRect.y(), nextRect.width(), nextRect.height())
                .build());

        screen.accessoriesToggleButton = null;
        if (PersonalDatabaseScreenLayoutHelper.hasAccessorySlots(screen) && screen.layout.accessoryToggleRect().height() > 0) {
            PersonalDatabaseLayout.Rect accessoryToggleRect = screen.layout.accessoryToggleRect();
            screen.accessoriesToggleButton = screen.addScreenButton(Button.builder(
                            Component.empty(),
                            button -> PersonalDatabaseScreenLayoutHelper.toggleAccessoriesPanel(screen)
                    )
                    .bounds(
                            accessoryToggleRect.x(),
                            accessoryToggleRect.y(),
                            accessoryToggleRect.width(),
                            accessoryToggleRect.height()
                    )
                    .build());
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
            Button toggleButton = screen.addScreenButton(Button.builder(
                            Component.empty(),
                            button -> toggleAdvancedSearchField(screen, field)
                    )
                    .bounds(
                            rowRect.x(),
                            rowRect.y(),
                            PersonalDatabaseScreen.ADVANCED_SEARCH_TOGGLE_WIDTH,
                            PersonalDatabaseScreen.ADVANCED_SEARCH_ROW_HEIGHT
                    )
                    .build());
            Button weightButton = screen.addScreenButton(Button.builder(
                            Component.empty(),
                            button -> cycleAdvancedSearchWeight(screen, field)
                    )
                    .bounds(
                            rowRect.right() - PersonalDatabaseScreen.ADVANCED_SEARCH_WEIGHT_WIDTH,
                            rowRect.y(),
                            PersonalDatabaseScreen.ADVANCED_SEARCH_WEIGHT_WIDTH,
                            PersonalDatabaseScreen.ADVANCED_SEARCH_ROW_HEIGHT
                    )
                    .build());
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
            Button toggleButton = screen.addScreenButton(Button.builder(
                            Component.empty(),
                            button -> toggleEnhancementOption(screen, option)
                    )
                    .bounds(
                            rowRect.x(),
                            rowRect.y(),
                            PersonalDatabaseScreen.ENHANCEMENT_TOGGLE_WIDTH,
                            PersonalDatabaseScreen.ENHANCEMENT_ROW_HEIGHT
                    )
                    .build());
            screen.enhancementToggleButtons.put(option, toggleButton);
        }
    }

    static void syncAdvancedSearchButtons(PersonalDatabaseScreen screen, DatabaseQuery query) {
        DatabaseSearchConfig searchConfig = query.searchConfig();
        int enabledTextFieldCount = enabledTextFieldCount(searchConfig);
        for (DatabaseSearchField field : DatabaseSearchField.values()) {
            DatabaseSearchWeight weight = searchConfig.weightFor(field);
            Button toggleButton = screen.advancedSearchToggleButtons.get(field);
            if (toggleButton != null) {
                toggleButton.visible = screen.advancedSearchExpanded;
                toggleButton.active = !field.isTextField()
                        || weight == DatabaseSearchWeight.OFF
                        || enabledTextFieldCount > 1;
                toggleButton.setMessage(Component.empty());
            }
            Button weightButton = screen.advancedSearchWeightButtons.get(field);
            if (weightButton != null) {
                weightButton.visible = screen.advancedSearchExpanded;
                weightButton.active = weight != DatabaseSearchWeight.OFF;
                weightButton.setMessage(Component.empty());
            }
        }
    }

    static void syncEnhancementButtons(PersonalDatabaseScreen screen, DatabaseEnhancementConfig config) {
        for (DatabaseEnhancementOption option : DatabaseEnhancementOption.orderedValues()) {
            Button toggleButton = screen.enhancementToggleButtons.get(option);
            if (toggleButton != null) {
                toggleButton.visible = screen.enhancementPanelExpanded;
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
        DatabaseScope activeScope = query.scope();
        if (screen.searchBox != null && !screen.searchBox.isFocused() && !screen.searchBox.getValue().equals(query.searchText())) {
            screen.syncingSearchBox = true;
            screen.searchBox.setValue(query.searchText());
            screen.syncingSearchBox = false;
        }
        if (screen.advancedSearchButton != null) {
            screen.advancedSearchButton.setMessage(Component.translatable("screen.infiniteinventory.search_advanced_button"));
        }
        if (screen.enhancementButton != null) {
            screen.enhancementButton.setMessage(Component.translatable("screen.infiniteinventory.enhancement_button"));
        }
        if (screen.viewSelectorButton != null) {
            screen.viewSelectorButton.setMessage(Component.translatable("screen.infiniteinventory.visible_tabs_button"));
        }
        if (screen.tabManagementButton != null) {
            screen.tabManagementButton.setMessage(Component.translatable("screen.infiniteinventory.tab_management_button"));
        }
        if (screen.sortButton != null) {
            screen.sortButton.setMessage(Component.translatable(query.sortOption().translationKey()));
        }
        if (screen.previousPageButton != null) {
            screen.previousPageButton.active = query.pageIndex() > 0;
        }
        if (screen.nextPageButton != null) {
            screen.nextPageButton.active = query.pageIndex() + 1 < viewState.totalPages();
        }
        if (screen.depositButton != null) {
            screen.depositButton.active = screen.minecraftClient() != null && screen.minecraftClient().player != null;
        }
        if (screen.personalScopeButton != null) {
            screen.personalScopeButton.active = activeScope != DatabaseScope.PERSONAL;
        }
        if (screen.publicScopeButton != null) {
            screen.publicScopeButton.active = activeScope != DatabaseScope.PUBLIC;
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
        if (!screen.databaseMenu.getCarried().isEmpty()) {
            PersonalDatabaseScreenTargetHelper.closeTransientOverlays(screen);
            return;
        }
        PersonalDatabaseScreenContextHelper.validateContextMenu(screen, viewState);
    }

    private static void toggleAdvancedSearchField(PersonalDatabaseScreen screen, DatabaseSearchField field) {
        DatabaseQuery currentQuery = screen.databaseMenu.viewState().query();
        DatabaseSearchConfig searchConfig = currentQuery.searchConfig();
        DatabaseSearchWeight currentWeight = searchConfig.weightFor(field);
        DatabaseSearchWeight nextWeight = currentWeight == DatabaseSearchWeight.OFF
                ? field.defaultWeight()
                : DatabaseSearchWeight.OFF;
        if (field.isTextField() && currentWeight != DatabaseSearchWeight.OFF && enabledTextFieldCount(searchConfig) <= 1) {
            return;
        }
        sendSearchConfig(screen, currentQuery, searchConfig.withWeight(field, nextWeight));
    }

    private static void cycleAdvancedSearchWeight(PersonalDatabaseScreen screen, DatabaseSearchField field) {
        DatabaseQuery currentQuery = screen.databaseMenu.viewState().query();
        DatabaseSearchConfig searchConfig = currentQuery.searchConfig();
        DatabaseSearchWeight currentWeight = searchConfig.weightFor(field);
        if (currentWeight == DatabaseSearchWeight.OFF) {
            return;
        }
        sendSearchConfig(screen, currentQuery, searchConfig.withWeight(field, nextWeight(currentWeight)));
    }

    private static void toggleEnhancementOption(PersonalDatabaseScreen screen, DatabaseEnhancementOption option) {
        DatabaseEnhancementConfig currentConfig = screen.databaseMenu.viewState().enhancementConfig();
        sendEnhancementConfig(screen, currentConfig.withOption(option, !currentConfig.isEnabled(option)));
    }

    private static void sendSearchConfig(
            PersonalDatabaseScreen screen,
            DatabaseQuery currentQuery,
            DatabaseSearchConfig newSearchConfig
    ) {
        if (currentQuery.searchConfig().equals(newSearchConfig)) {
            return;
        }
        PersonalDatabaseScreenLayoutHelper.sendQuery(screen, currentQuery.withSearchConfig(newSearchConfig));
    }

    private static void sendEnhancementConfig(PersonalDatabaseScreen screen, DatabaseEnhancementConfig newConfig) {
        DatabaseEnhancementConfig currentConfig = screen.databaseMenu.viewState().enhancementConfig();
        String autoStoreTargetTabId = screen.databaseMenu.viewState().autoStoreTargetTabId();
        if (currentConfig.equals(newConfig)) {
            return;
        }
        PacketDistributor.sendToServer(new DatabaseEnhancementPayload(
                screen.databaseMenu.containerId,
                screen.databaseMenu.viewState().sessionId(),
                newConfig,
                autoStoreTargetTabId
        ));
    }

    private static DatabaseSearchWeight nextWeight(DatabaseSearchWeight currentWeight) {
        return switch (currentWeight) {
            case OFF -> DatabaseSearchWeight.LOW;
            case LOW -> DatabaseSearchWeight.MEDIUM;
            case MEDIUM -> DatabaseSearchWeight.HIGH;
            case HIGH -> DatabaseSearchWeight.LOW;
        };
    }
}
