package com.agguy.infiniteinventory.client.screen;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

public final class DatabasePagePickerModel {
    private static final int FULL_LIST_LIMIT = 7;
    private static final int NEIGHBOR_RADIUS = 2;

    private DatabasePagePickerModel() {
    }

    public static List<PageOption> build(int totalPages, int currentPageIndex) {
        int safeTotalPages = Math.max(1, totalPages);
        int safeCurrentPageIndex = Math.max(0, Math.min(currentPageIndex, safeTotalPages - 1));
        if (safeTotalPages <= FULL_LIST_LIMIT) {
            List<PageOption> options = new ArrayList<>(safeTotalPages);
            for (int pageIndex = 0; pageIndex < safeTotalPages; pageIndex++) {
                options.add(new PageOption(pageIndex, ShortcutType.PAGE));
            }
            return List.copyOf(options);
        }

        TreeSet<Integer> pageIndexes = new TreeSet<>();
        pageIndexes.add(0);
        pageIndexes.add(safeTotalPages - 1);

        int neighborhoodStart = Math.max(0, safeCurrentPageIndex - NEIGHBOR_RADIUS);
        int neighborhoodEnd = Math.min(safeTotalPages - 1, safeCurrentPageIndex + NEIGHBOR_RADIUS);
        for (int pageIndex = neighborhoodStart; pageIndex <= neighborhoodEnd; pageIndex++) {
            pageIndexes.add(pageIndex);
        }

        addJumpPage(pageIndexes, 1, neighborhoodStart - 1);
        addJumpPage(pageIndexes, neighborhoodEnd + 1, safeTotalPages - 2);

        List<PageOption> options = new ArrayList<>(pageIndexes.size());
        for (int pageIndex : pageIndexes) {
            ShortcutType shortcutType = ShortcutType.PAGE;
            if (pageIndex == 0) {
                shortcutType = ShortcutType.FIRST;
            } else if (pageIndex == safeTotalPages - 1) {
                shortcutType = ShortcutType.LAST;
            }
            options.add(new PageOption(pageIndex, shortcutType));
        }
        return List.copyOf(options);
    }

    private static void addJumpPage(TreeSet<Integer> pageIndexes, int start, int end) {
        if (start > end) {
            return;
        }
        pageIndexes.add(start + (end - start) / 2);
    }

    public enum ShortcutType {
        FIRST,
        PAGE,
        LAST
    }

    public record PageOption(int pageIndex, ShortcutType shortcutType) {
        public PageOption {
            pageIndex = Math.max(0, pageIndex);
            shortcutType = shortcutType == null ? ShortcutType.PAGE : shortcutType;
        }
    }
}
