package com.agguy.infiniteinventory.database.tests;

import com.agguy.infiniteinventory.database.DatabaseSearchConfig;
import com.agguy.infiniteinventory.database.DatabaseSearchField;
import com.agguy.infiniteinventory.database.DatabaseSearchWeight;
import com.agguy.infiniteinventory.tests.MinecraftTestBootstrap;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseSearchConfigTest {
    static {
        MinecraftTestBootstrap.ensureBootstrapped();
    }

    @Test
    void defaultConfigShouldHaveDefaultWeights() {
        DatabaseSearchConfig config = DatabaseSearchConfig.defaultConfig();

        assertEquals(DatabaseSearchField.DISPLAY_NAME.defaultWeight(), config.displayNameWeight());
        assertEquals(DatabaseSearchField.ITEM_ID.defaultWeight(), config.itemIdWeight());
        assertEquals(DatabaseSearchField.PINYIN.defaultWeight(), config.pinyinWeight());
        assertEquals(DatabaseSearchField.MOD_NAMESPACE.defaultWeight(), config.modNamespaceWeight());
        assertEquals(DatabaseSearchField.NOTE.defaultWeight(), config.noteWeight());
        assertEquals(DatabaseSearchField.COUNT_BOOST.defaultWeight(), config.countBoostWeight());
    }

    @Test
    void defaultConfigShouldHaveEnabledTextField() {
        assertTrue(DatabaseSearchConfig.defaultConfig().hasEnabledTextField());
    }

    @Test
    void shouldDefaultNullWeightsToFieldDefaults() {
        DatabaseSearchConfig config = new DatabaseSearchConfig(null, null, null, null, null, null);

        assertEquals(DatabaseSearchField.DISPLAY_NAME.defaultWeight(), config.displayNameWeight());
        assertEquals(DatabaseSearchField.ITEM_ID.defaultWeight(), config.itemIdWeight());
    }

    @Test
    void shouldReenableDisplayNameWhenAllTextFieldsAreDisabled() {
        DatabaseSearchConfig config = new DatabaseSearchConfig(
                DatabaseSearchWeight.OFF,
                DatabaseSearchWeight.OFF,
                DatabaseSearchWeight.OFF,
                DatabaseSearchWeight.OFF,
                DatabaseSearchWeight.OFF,
                DatabaseSearchWeight.OFF
        );

        assertEquals(DatabaseSearchField.DISPLAY_NAME.defaultWeight(), config.displayNameWeight());
        assertTrue(config.hasEnabledTextField());
    }

    @Test
    void hasEnabledTextFieldShouldBeTrueEvenWhenOnlyCountBoostEnabled() {
        // allTextFieldsDisabled guard re-enables displayName, so hasEnabledTextField is always true
        DatabaseSearchConfig config = new DatabaseSearchConfig(
                DatabaseSearchWeight.OFF,
                DatabaseSearchWeight.OFF,
                DatabaseSearchWeight.OFF,
                DatabaseSearchWeight.OFF,
                DatabaseSearchWeight.OFF,
                DatabaseSearchWeight.HIGH
        );

        assertTrue(config.hasEnabledTextField());
    }

    @Test
    void weightForShouldReturnCorrectWeight() {
        DatabaseSearchConfig config = new DatabaseSearchConfig(
                DatabaseSearchWeight.HIGH,
                DatabaseSearchWeight.MEDIUM,
                DatabaseSearchWeight.LOW,
                DatabaseSearchWeight.OFF,
                DatabaseSearchWeight.HIGH,
                DatabaseSearchWeight.MEDIUM
        );

        assertEquals(DatabaseSearchWeight.HIGH, config.weightFor(DatabaseSearchField.DISPLAY_NAME));
        assertEquals(DatabaseSearchWeight.MEDIUM, config.weightFor(DatabaseSearchField.ITEM_ID));
        assertEquals(DatabaseSearchWeight.LOW, config.weightFor(DatabaseSearchField.PINYIN));
        assertEquals(DatabaseSearchWeight.OFF, config.weightFor(DatabaseSearchField.MOD_NAMESPACE));
        assertEquals(DatabaseSearchWeight.HIGH, config.weightFor(DatabaseSearchField.NOTE));
        assertEquals(DatabaseSearchWeight.MEDIUM, config.weightFor(DatabaseSearchField.COUNT_BOOST));
    }

    @Test
    void withWeightShouldUpdateSpecificField() {
        DatabaseSearchConfig config = DatabaseSearchConfig.defaultConfig();
        DatabaseSearchConfig updated = config.withWeight(DatabaseSearchField.NOTE, DatabaseSearchWeight.HIGH);

        assertEquals(DatabaseSearchWeight.HIGH, updated.noteWeight());
        assertEquals(config.displayNameWeight(), updated.displayNameWeight());
    }

    @Test
    void toTagShouldRoundTrip() {
        DatabaseSearchConfig config = new DatabaseSearchConfig(
                DatabaseSearchWeight.HIGH,
                DatabaseSearchWeight.OFF,
                DatabaseSearchWeight.MEDIUM,
                DatabaseSearchWeight.LOW,
                DatabaseSearchWeight.HIGH,
                DatabaseSearchWeight.OFF
        );
        CompoundTag tag = config.toTag();

        DatabaseSearchConfig restored = DatabaseSearchConfig.fromTag(tag);

        assertEquals(config.displayNameWeight(), restored.displayNameWeight());
        assertEquals(config.itemIdWeight(), restored.itemIdWeight());
        assertEquals(config.pinyinWeight(), restored.pinyinWeight());
        assertEquals(config.modNamespaceWeight(), restored.modNamespaceWeight());
        assertEquals(config.noteWeight(), restored.noteWeight());
        assertEquals(config.countBoostWeight(), restored.countBoostWeight());
    }

    @Test
    void fromTagShouldHandleNullTag() {
        DatabaseSearchConfig config = DatabaseSearchConfig.fromTag(null);

        assertEquals(DatabaseSearchConfig.defaultConfig().displayNameWeight(), config.displayNameWeight());
    }

    @Test
    void fromTagShouldHandleEmptyTag() {
        DatabaseSearchConfig config = DatabaseSearchConfig.fromTag(new CompoundTag());

        assertEquals(DatabaseSearchConfig.defaultConfig().displayNameWeight(), config.displayNameWeight());
    }

    @Test
    void fromTagShouldHandleInvalidWeightValue() {
        CompoundTag tag = new CompoundTag();
        tag.putString("display_name", "INVALID_WEIGHT");
        tag.putString("item_id", "MEDIUM");
        tag.putString("pinyin", "HIGH");
        tag.putString("mod_namespace", "LOW");
        tag.putString("note", "OFF");
        tag.putString("count_boost", "LOW");

        DatabaseSearchConfig config = DatabaseSearchConfig.fromTag(tag);

        assertEquals(DatabaseSearchField.DISPLAY_NAME.defaultWeight(), config.displayNameWeight());
        assertEquals(DatabaseSearchWeight.MEDIUM, config.itemIdWeight());
    }
}
