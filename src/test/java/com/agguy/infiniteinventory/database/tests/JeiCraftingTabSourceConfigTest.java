package com.agguy.infiniteinventory.database.tests;

import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.DatabaseTabs;
import com.agguy.infiniteinventory.database.JeiCraftingTabSourceConfig;
import io.netty.buffer.Unpooled;
import java.util.Set;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JeiCraftingTabSourceConfigTest {

    @Test
    void emptyShouldHaveNoTabsEnabled() {
        JeiCraftingTabSourceConfig config = JeiCraftingTabSourceConfig.empty();

        assertFalse(config.isTabEnabled(DatabaseScope.PERSONAL, DatabaseTabs.ALL_TAB_ID));
        assertFalse(config.isTabEnabled(DatabaseScope.PUBLIC, DatabaseTabs.ALL_TAB_ID));
        assertTrue(config.enabledTabIdsFor(DatabaseScope.PERSONAL).isEmpty());
        assertTrue(config.enabledTabIdsFor(DatabaseScope.PUBLIC).isEmpty());
    }

    @Test
    void allEnabledShouldHaveAllTabEnabledForBothScopes() {
        JeiCraftingTabSourceConfig config = JeiCraftingTabSourceConfig.allEnabled();

        assertTrue(config.isTabEnabled(DatabaseScope.PERSONAL, DatabaseTabs.ALL_TAB_ID));
        assertTrue(config.isTabEnabled(DatabaseScope.PUBLIC, DatabaseTabs.ALL_TAB_ID));
        assertFalse(config.isTabEnabled(DatabaseScope.PERSONAL, DatabaseTabs.DEFAULT_TAB_ID));
    }

    @Test
    void withTabEnabledShouldEnableTab() {
        JeiCraftingTabSourceConfig config = JeiCraftingTabSourceConfig.empty()
                .withTabEnabled(DatabaseScope.PERSONAL, "blocks", true);

        assertTrue(config.isTabEnabled(DatabaseScope.PERSONAL, "blocks"));
        assertFalse(config.isTabEnabled(DatabaseScope.PUBLIC, "blocks"));
    }

    @Test
    void withTabEnabledShouldDisableTab() {
        JeiCraftingTabSourceConfig config = JeiCraftingTabSourceConfig.allEnabled()
                .withTabEnabled(DatabaseScope.PERSONAL, DatabaseTabs.ALL_TAB_ID, false);

        assertFalse(config.isTabEnabled(DatabaseScope.PERSONAL, DatabaseTabs.ALL_TAB_ID));
        assertTrue(config.isTabEnabled(DatabaseScope.PUBLIC, DatabaseTabs.ALL_TAB_ID));
    }

    @Test
    void withTabEnabledShouldReturnSameInstanceWhenNoChange() {
        JeiCraftingTabSourceConfig config = JeiCraftingTabSourceConfig.allEnabled();
        JeiCraftingTabSourceConfig result = config.withTabEnabled(DatabaseScope.PERSONAL, DatabaseTabs.ALL_TAB_ID, true);

        assertSame(config, result);
    }

    @Test
    void shouldRoundTripThroughTag() {
        JeiCraftingTabSourceConfig config = JeiCraftingTabSourceConfig.empty()
                .withTabEnabled(DatabaseScope.PERSONAL, "blocks", true)
                .withTabEnabled(DatabaseScope.PERSONAL, "tools", true)
                .withTabEnabled(DatabaseScope.PUBLIC, "food", true);

        JeiCraftingTabSourceConfig restored = JeiCraftingTabSourceConfig.fromTag(config.toTag());

        assertEquals(config.enabledTabIdsFor(DatabaseScope.PERSONAL), restored.enabledTabIdsFor(DatabaseScope.PERSONAL));
        assertEquals(config.enabledTabIdsFor(DatabaseScope.PUBLIC), restored.enabledTabIdsFor(DatabaseScope.PUBLIC));
        assertTrue(restored.isTabEnabled(DatabaseScope.PERSONAL, "blocks"));
        assertTrue(restored.isTabEnabled(DatabaseScope.PERSONAL, "tools"));
        assertTrue(restored.isTabEnabled(DatabaseScope.PUBLIC, "food"));
        assertFalse(restored.isTabEnabled(DatabaseScope.PUBLIC, "blocks"));
    }

    @Test
    void fromTagShouldReturnAllEnabledForNullOrEmptyTag() {
        JeiCraftingTabSourceConfig fromNull = JeiCraftingTabSourceConfig.fromTag(null);
        JeiCraftingTabSourceConfig fromEmpty = JeiCraftingTabSourceConfig.fromTag(new net.minecraft.nbt.CompoundTag());

        assertTrue(fromNull.isTabEnabled(DatabaseScope.PERSONAL, DatabaseTabs.ALL_TAB_ID));
        assertTrue(fromEmpty.isTabEnabled(DatabaseScope.PUBLIC, DatabaseTabs.ALL_TAB_ID));
    }

    @Test
    void shouldRoundTripThroughBuffer() {
        JeiCraftingTabSourceConfig config = JeiCraftingTabSourceConfig.empty()
                .withTabEnabled(DatabaseScope.PERSONAL, "blocks", true)
                .withTabEnabled(DatabaseScope.PUBLIC, "food", true)
                .withTabEnabled(DatabaseScope.PUBLIC, "gear", true);

        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        config.write(buffer);
        JeiCraftingTabSourceConfig restored = JeiCraftingTabSourceConfig.read(buffer);

        assertEquals(config.enabledTabIdsFor(DatabaseScope.PERSONAL), restored.enabledTabIdsFor(DatabaseScope.PERSONAL));
        assertEquals(config.enabledTabIdsFor(DatabaseScope.PUBLIC), restored.enabledTabIdsFor(DatabaseScope.PUBLIC));
        assertEquals(Set.of("blocks"), restored.enabledTabIdsFor(DatabaseScope.PERSONAL));
        assertEquals(Set.of("food", "gear"), restored.enabledTabIdsFor(DatabaseScope.PUBLIC));
    }

    @Test
    void enabledTabIdsForShouldBeUnmodifiable() {
        JeiCraftingTabSourceConfig config = JeiCraftingTabSourceConfig.allEnabled();
        Set<String> ids = config.enabledTabIdsFor(DatabaseScope.PERSONAL);

        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class, () -> ids.add("extra"));
    }

    @Test
    void isTabEnabledShouldNormalizeScope() {
        JeiCraftingTabSourceConfig config = JeiCraftingTabSourceConfig.allEnabled()
                .withTabEnabled(DatabaseScope.PERSONAL, DatabaseTabs.ALL_TAB_ID, false);

        assertFalse(config.isTabEnabled(DatabaseScope.PERSONAL, DatabaseTabs.ALL_TAB_ID));
    }
}
