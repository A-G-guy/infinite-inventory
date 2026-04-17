package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.database.DatabaseAutoStoreTarget;
import com.agguy.infiniteinventory.database.DatabaseEnhancementConfig;
import com.agguy.infiniteinventory.database.DatabaseEnhancementOption;
import com.agguy.infiniteinventory.database.DatabaseQuery;
import com.agguy.infiniteinventory.database.DatabaseSearchConfig;
import com.agguy.infiniteinventory.database.DatabaseSearchField;
import com.agguy.infiniteinventory.database.DatabaseSearchWeight;
import com.agguy.infiniteinventory.network.DatabaseEnhancementPayload;
import net.neoforged.neoforge.network.PacketDistributor;

final class PersonalDatabaseScreenConfigActionHelper {
    private PersonalDatabaseScreenConfigActionHelper() {
    }

    static void toggleAdvancedSearchField(PersonalDatabaseScreen screen, DatabaseSearchField field) {
        DatabaseQuery currentQuery = screen.databaseMenu.viewState().query();
        DatabaseSearchConfig searchConfig = currentQuery.searchConfig();
        DatabaseSearchWeight currentWeight = searchConfig.weightFor(field);
        DatabaseSearchWeight nextWeight = currentWeight == DatabaseSearchWeight.OFF
                ? field.defaultWeight()
                : DatabaseSearchWeight.OFF;
        if (field.isTextField() && currentWeight != DatabaseSearchWeight.OFF
                && PersonalDatabaseScreenWidgetHelper.enabledTextFieldCount(searchConfig) <= 1) {
            return;
        }
        sendSearchConfig(screen, currentQuery, searchConfig.withWeight(field, nextWeight));
    }

    static void cycleAdvancedSearchWeight(PersonalDatabaseScreen screen, DatabaseSearchField field) {
        DatabaseQuery currentQuery = screen.databaseMenu.viewState().query();
        DatabaseSearchConfig searchConfig = currentQuery.searchConfig();
        DatabaseSearchWeight currentWeight = searchConfig.weightFor(field);
        if (currentWeight == DatabaseSearchWeight.OFF) {
            return;
        }
        sendSearchConfig(screen, currentQuery, searchConfig.withWeight(field, nextWeight(currentWeight)));
    }

    static void toggleEnhancementOption(PersonalDatabaseScreen screen, DatabaseEnhancementOption option) {
        DatabaseEnhancementConfig currentConfig = screen.databaseMenu.viewState().enhancementConfig();
        sendEnhancementConfig(screen, currentConfig.withOption(option, !currentConfig.isEnabled(option)));
    }

    private static void sendSearchConfig(
            PersonalDatabaseScreen screen,
            DatabaseQuery currentQuery,
            DatabaseSearchConfig newSearchConfig
    ) {
        if (!currentQuery.searchConfig().equals(newSearchConfig)) {
            PersonalDatabaseScreenLayoutHelper.sendQuery(screen, currentQuery.withSearchConfig(newSearchConfig));
        }
    }

    private static void sendEnhancementConfig(PersonalDatabaseScreen screen, DatabaseEnhancementConfig newConfig) {
        DatabaseEnhancementConfig currentConfig = screen.databaseMenu.viewState().enhancementConfig();
        DatabaseAutoStoreTarget autoStoreTarget = screen.databaseMenu.viewState().autoStoreTarget();
        if (currentConfig.equals(newConfig)) {
            return;
        }
        PacketDistributor.sendToServer(new DatabaseEnhancementPayload(
                screen.databaseMenu.containerId,
                screen.databaseMenu.viewState().sessionId(),
                newConfig,
                autoStoreTarget
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
