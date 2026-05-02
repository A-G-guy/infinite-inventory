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

    enum ScrollbarDragTarget {
        NONE,
        STATS_LOG,
        ACCESSORY
    }

    enum StatisticsPanelTab {
        OVERVIEW("screen.infiniteinventory.statistics.overview"),
        CATEGORY("screen.infiniteinventory.statistics.category"),
        MODS("screen.infiniteinventory.statistics.mods"),
        TABS("screen.infiniteinventory.statistics.tabs"),
        TRENDS("screen.infiniteinventory.statistics.trends"),
        LOGS("screen.infiniteinventory.statistics.logs");

        private final String translationKey;

        StatisticsPanelTab(String translationKey) {
            this.translationKey = translationKey;
        }

        public String translationKey() {
            return this.translationKey;
        }
    }
}
