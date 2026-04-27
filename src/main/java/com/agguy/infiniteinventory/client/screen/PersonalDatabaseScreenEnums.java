package com.agguy.infiniteinventory.client.screen;

/**
 * {@link PersonalDatabaseScreen} 使用的枚举类型，抽取以控制主类规模。
 */
final class PersonalDatabaseScreenEnums {
    private PersonalDatabaseScreenEnums() {
    }

    enum TargetSelectorMode {
        NONE,
        DEPOSIT_ALL,
        DEPOSIT_EXISTING_BY_TAB,
        CARRIED_STORE,
        QUICK_DEPOSIT,
        TRANSFER_TAB,
        TRANSFER_SELECTION,
        DELETE_TAB,
        AUTO_STORE_TARGET
    }

    enum SettingsPanelTab {
        ADVANCED_SEARCH("screen.infiniteinventory.search_advanced_title"),
        ENHANCEMENT("screen.infiniteinventory.enhancement_title"),
        MANAGEMENT("screen.infiniteinventory.management.title");

        private final String translationKey;

        SettingsPanelTab(String translationKey) {
            this.translationKey = translationKey;
        }

        public String translationKey() {
            return this.translationKey;
        }
    }
}
