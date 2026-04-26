package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.database.DatabaseAutoStoreTarget;
import com.agguy.infiniteinventory.database.DatabaseEnhancementConfig;
import com.agguy.infiniteinventory.database.DatabasePanelView;
import com.agguy.infiniteinventory.database.DatabaseQuery;
import com.agguy.infiniteinventory.database.DatabaseScopedTabRef;
import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.DatabaseTab;
import com.agguy.infiniteinventory.database.DatabaseTabs;
import com.agguy.infiniteinventory.database.DatabaseViewState;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersonalDatabaseTargetSelectorModelTest {
    @Test
    void transferSelectionRowsShouldGroupCurrentScopeBeforeOtherScope() {
        DatabaseViewState viewState = viewState(
                DatabaseScope.PERSONAL,
                List.of(DatabaseTabs.allTab(), DatabaseTabs.defaultConcreteTab(), tab("personal_tools", "Personal Tools")),
                List.of(DatabaseTabs.allTab(), DatabaseTabs.defaultConcreteTab(), tab("public_blocks", "Public Blocks"))
        );

        List<PersonalDatabaseTargetSelectorModel.Row> rows = PersonalDatabaseTargetSelectorModel.buildRows(
                PersonalDatabaseScreenEnums.TargetSelectorMode.TRANSFER_SELECTION,
                viewState,
                DatabaseScope.PERSONAL,
                "",
                List.of()
        );

        assertHeader(rows.get(0), DatabaseScope.PERSONAL, true);
        assertTarget(rows.get(1), DatabaseScope.PERSONAL, DatabaseTabs.DEFAULT_TAB_ID);
        assertTarget(rows.get(2), DatabaseScope.PERSONAL, "personal_tools");
        assertHeader(rows.get(3), DatabaseScope.PUBLIC, false);
        assertTarget(rows.get(4), DatabaseScope.PUBLIC, DatabaseTabs.DEFAULT_TAB_ID);
        assertTarget(rows.get(5), DatabaseScope.PUBLIC, "public_blocks");
    }

    @Test
    void transferTabRowsShouldOnlyExcludeCurrentTabFromSourceScopeGroup() {
        DatabaseViewState viewState = viewState(
                DatabaseScope.PERSONAL,
                List.of(DatabaseTabs.allTab(), DatabaseTabs.defaultConcreteTab(), tab("shared_tab", "Shared Personal")),
                List.of(DatabaseTabs.allTab(), DatabaseTabs.defaultConcreteTab(), tab("shared_tab", "Shared Public"))
        );

        List<PersonalDatabaseTargetSelectorModel.Row> rows = PersonalDatabaseTargetSelectorModel.buildRows(
                PersonalDatabaseScreenEnums.TargetSelectorMode.TRANSFER_TAB,
                viewState,
                DatabaseScope.PERSONAL,
                "shared_tab",
                List.of()
        );

        assertHeader(rows.get(0), DatabaseScope.PERSONAL, true);
        assertTarget(rows.get(1), DatabaseScope.PERSONAL, DatabaseTabs.DEFAULT_TAB_ID);
        assertHeader(rows.get(2), DatabaseScope.PUBLIC, false);
        assertTarget(rows.get(3), DatabaseScope.PUBLIC, DatabaseTabs.DEFAULT_TAB_ID);
        assertTarget(rows.get(4), DatabaseScope.PUBLIC, "shared_tab");
        assertFalse(rows.stream().anyMatch(row -> row.isClickable()
                && row.scope() == DatabaseScope.PERSONAL
                && "shared_tab".equals(row.tab().id())));
    }

    @Test
    void deleteTabRowsShouldRemainFlatWithinCurrentScope() {
        DatabaseViewState viewState = viewState(
                DatabaseScope.PUBLIC,
                List.of(DatabaseTabs.allTab(), DatabaseTabs.defaultConcreteTab(), tab("personal_misc", "Personal Misc")),
                List.of(DatabaseTabs.allTab(), DatabaseTabs.defaultConcreteTab(), tab("public_blocks", "Public Blocks"), tab("public_food", "Public Food"))
        );

        List<PersonalDatabaseTargetSelectorModel.Row> rows = PersonalDatabaseTargetSelectorModel.buildRows(
                PersonalDatabaseScreenEnums.TargetSelectorMode.DELETE_TAB,
                viewState,
                DatabaseScope.PUBLIC,
                "public_blocks",
                List.of()
        );

        assertEquals(2, rows.size());
        assertTrue(rows.stream().allMatch(row -> row.type() == PersonalDatabaseTargetSelectorModel.RowType.TARGET));
        assertTarget(rows.get(0), DatabaseScope.PUBLIC, DatabaseTabs.DEFAULT_TAB_ID);
        assertTarget(rows.get(1), DatabaseScope.PUBLIC, "public_food");
    }

    @Test
    void depositAllRowsShouldGroupVisibleConcreteTabsAcrossScopes() {
        DatabaseViewState viewState = viewState(
                DatabaseScope.PUBLIC,
                List.of(DatabaseTabs.allTab(), DatabaseTabs.defaultConcreteTab(), tab("personal_misc", "Personal Misc")),
                List.of(DatabaseTabs.allTab(), DatabaseTabs.defaultConcreteTab(), tab("public_blocks", "Public Blocks"), tab("public_food", "Public Food"))
        );

        List<PersonalDatabaseTargetSelectorModel.Row> rows = PersonalDatabaseTargetSelectorModel.buildRows(
                PersonalDatabaseScreenEnums.TargetSelectorMode.DEPOSIT_ALL,
                viewState,
                DatabaseScope.PUBLIC,
                "",
                List.of(
                        panel(DatabaseScope.PERSONAL, tab("personal_misc", "Personal Misc")),
                        panel(DatabaseScope.PUBLIC, tab("public_food", "Public Food"))
                )
        );

        assertEquals(4, rows.size());
        assertHeader(rows.get(0), DatabaseScope.PERSONAL, false);
        assertTarget(rows.get(1), DatabaseScope.PERSONAL, "personal_misc");
        assertHeader(rows.get(2), DatabaseScope.PUBLIC, false);
        assertTarget(rows.get(3), DatabaseScope.PUBLIC, "public_food");
    }

    @Test
    void autoStoreTargetRowsShouldGroupPersonalAndPublicTabs() {
        DatabaseViewState viewState = viewState(
                DatabaseScope.PERSONAL,
                List.of(DatabaseTabs.allTab(), DatabaseTabs.defaultConcreteTab(), tab("personal_tools", "Personal Tools")),
                List.of(DatabaseTabs.allTab(), DatabaseTabs.defaultConcreteTab(), tab("public_food", "Public Food"))
        );

        List<PersonalDatabaseTargetSelectorModel.Row> rows = PersonalDatabaseTargetSelectorModel.buildRows(
                PersonalDatabaseScreenEnums.TargetSelectorMode.AUTO_STORE_TARGET,
                viewState,
                DatabaseScope.PERSONAL,
                "",
                List.of()
        );

        assertEquals(6, rows.size());
        assertHeader(rows.get(0), DatabaseScope.PERSONAL, false);
        assertTarget(rows.get(1), DatabaseScope.PERSONAL, DatabaseTabs.DEFAULT_TAB_ID);
        assertTarget(rows.get(2), DatabaseScope.PERSONAL, "personal_tools");
        assertHeader(rows.get(3), DatabaseScope.PUBLIC, false);
        assertTarget(rows.get(4), DatabaseScope.PUBLIC, DatabaseTabs.DEFAULT_TAB_ID);
        assertTarget(rows.get(5), DatabaseScope.PUBLIC, "public_food");
    }

    private static void assertHeader(
            PersonalDatabaseTargetSelectorModel.Row row,
            DatabaseScope expectedScope,
            boolean expectedSourceScopeGroup
    ) {
        assertEquals(PersonalDatabaseTargetSelectorModel.RowType.HEADER, row.type());
        assertEquals(expectedScope, row.scope());
        assertEquals(expectedSourceScopeGroup, row.sourceScopeGroup());
        assertFalse(row.isClickable());
    }

    private static void assertTarget(
            PersonalDatabaseTargetSelectorModel.Row row,
            DatabaseScope expectedScope,
            String expectedTabId
    ) {
        assertEquals(PersonalDatabaseTargetSelectorModel.RowType.TARGET, row.type());
        assertTrue(row.isClickable());
        assertEquals(expectedScope, row.scope());
        assertEquals(expectedTabId, row.tab().id());
        assertEquals(expectedScope, row.targetSelection().scope());
        assertEquals(expectedTabId, row.targetSelection().tabId());
    }

    private static DatabaseViewState viewState(
            DatabaseScope queryScope,
            List<DatabaseTab> personalTabs,
            List<DatabaseTab> publicTabs
    ) {
        return new DatabaseViewState(
                1,
                42L,
                DatabaseQuery.defaultQuery(queryScope),
                DatabaseEnhancementConfig.defaultConfig(),
                DatabaseAutoStoreTarget.defaultTarget(),
                personalTabs,
                publicTabs,
                List.of()
        );
    }

    private static DatabasePanelView panel(DatabaseScope scope, DatabaseTab tab) {
        return new DatabasePanelView(DatabaseScopedTabRef.concreteTab(scope, tab.id()), tab, 0, 54, 0, 1, 0L, List.of());
    }

    private static DatabaseTab tab(String id, String name) {
        return new DatabaseTab(id, name, "", DatabaseTabs.DEFAULT_CONCRETE_ICON_ITEM_ID, false, false);
    }
}
