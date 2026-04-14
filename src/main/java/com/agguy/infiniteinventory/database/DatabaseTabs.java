package com.agguy.infiniteinventory.database;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public final class DatabaseTabs {
    public static final String ALL_TAB_ID = "__all";
    public static final String DEFAULT_TAB_ID = "__default";
    public static final String ALL_TAB_TRANSLATION_KEY = "screen.infiniteinventory.tab.all";
    public static final String DEFAULT_TAB_TRANSLATION_KEY = "screen.infiniteinventory.tab.default";
    public static final String DEFAULT_ALL_ICON_ITEM_ID = "minecraft:compass";
    public static final String DEFAULT_CONCRETE_ICON_ITEM_ID = "minecraft:chest";
    public static final int MAX_VISIBLE_TAB_COUNT = 4;
    public static final int MAX_TAB_NAME_LENGTH = 32;

    private DatabaseTabs() {
    }

    public static DatabaseTab allTab() {
        return new DatabaseTab(
                ALL_TAB_ID,
                "",
                ALL_TAB_TRANSLATION_KEY,
                DEFAULT_ALL_ICON_ITEM_ID,
                true,
                true
        );
    }

    public static DatabaseTab defaultConcreteTab() {
        return new DatabaseTab(
                DEFAULT_TAB_ID,
                "",
                DEFAULT_TAB_TRANSLATION_KEY,
                DEFAULT_CONCRETE_ICON_ITEM_ID,
                false,
                true
        );
    }

    public static boolean isReservedId(String tabId) {
        return ALL_TAB_ID.equals(tabId) || DEFAULT_TAB_ID.equals(tabId);
    }

    public static boolean isAllTabId(String tabId) {
        return ALL_TAB_ID.equals(tabId);
    }

    public static boolean isConcreteTabId(String tabId) {
        return !isAllTabId(tabId) && tabId != null && !tabId.isBlank();
    }

    public static String normalizeTabId(String tabId, String fallback) {
        if (tabId == null || tabId.isBlank()) {
            return fallback;
        }
        return tabId.trim();
    }

    public static String normalizeConcreteTarget(String tabId) {
        if (!isConcreteTabId(tabId)) {
            return DEFAULT_TAB_ID;
        }
        return tabId.trim();
    }

    public static String normalizeTabName(String name, String fallbackTranslationKey) {
        if (name == null) {
            return "";
        }
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        if (trimmed.length() <= MAX_TAB_NAME_LENGTH) {
            return trimmed;
        }
        return trimmed.substring(0, MAX_TAB_NAME_LENGTH);
    }

    public static String normalizeIconItemId(String iconItemId, boolean allTab) {
        if (iconItemId == null || iconItemId.isBlank()) {
            return allTab ? DEFAULT_ALL_ICON_ITEM_ID : DEFAULT_CONCRETE_ICON_ITEM_ID;
        }
        return iconItemId.trim().toLowerCase(Locale.ROOT);
    }

    public static String newCustomTabId() {
        return "tab_" + UUID.randomUUID().toString().replace("-", "");
    }

    public static List<String> normalizeVisibleTabIds(List<String> requestedTabIds, DatabaseTabDirectory directory, String fallbackFocusedTabId) {
        Objects.requireNonNull(directory, "directory");
        java.util.LinkedHashSet<String> normalizedTabIds = new java.util.LinkedHashSet<>();
        if (requestedTabIds != null) {
            for (String requestedTabId : requestedTabIds) {
                String resolvedTabId = directory.resolveVisibleTabId(requestedTabId);
                if (resolvedTabId == null) {
                    continue;
                }
                normalizedTabIds.add(resolvedTabId);
                if (normalizedTabIds.size() >= MAX_VISIBLE_TAB_COUNT) {
                    break;
                }
            }
        }
        if (normalizedTabIds.isEmpty()) {
            normalizedTabIds.add(directory.resolveVisibleTabId(fallbackFocusedTabId));
        }
        java.util.List<String> orderedTabIds = new java.util.ArrayList<>(MAX_VISIBLE_TAB_COUNT);
        for (DatabaseTab tab : directory.orderedTabs()) {
            if (normalizedTabIds.contains(tab.id())) {
                orderedTabIds.add(tab.id());
            }
        }
        if (orderedTabIds.isEmpty()) {
            orderedTabIds.add(ALL_TAB_ID);
        }
        return List.copyOf(orderedTabIds);
    }
}
