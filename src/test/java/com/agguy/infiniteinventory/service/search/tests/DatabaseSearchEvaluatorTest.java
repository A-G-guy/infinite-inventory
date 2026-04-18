package com.agguy.infiniteinventory.service.search.tests;

import com.agguy.infiniteinventory.database.DatabaseQuery;
import com.agguy.infiniteinventory.database.DatabaseSearchConfig;
import com.agguy.infiniteinventory.database.DatabaseSearchField;
import com.agguy.infiniteinventory.database.DatabaseSearchWeight;
import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.DatabaseSortOption;
import com.agguy.infiniteinventory.database.DatabaseTabs;
import com.agguy.infiniteinventory.service.search.DatabaseSearchEvaluator;
import com.agguy.infiniteinventory.service.search.DatabaseSearchIndex;
import com.agguy.infiniteinventory.service.search.DatabaseSearchRanking;
import com.agguy.infiniteinventory.service.search.SearchTextNormalizer;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseSearchEvaluatorTest {
    private final DatabaseSearchEvaluator evaluator = new DatabaseSearchEvaluator();

    @Test
    void exactMatchesShouldOutrankPrefixAndContainsMatches() {
        DatabaseSearchConfig displayOnlyConfig = DatabaseSearchConfig.defaultConfig()
                .withWeight(DatabaseSearchField.ITEM_ID, DatabaseSearchWeight.OFF)
                .withWeight(DatabaseSearchField.PINYIN, DatabaseSearchWeight.OFF)
                .withWeight(DatabaseSearchField.MOD_NAMESPACE, DatabaseSearchWeight.OFF);
        DatabaseQuery query = this.query("diamond", displayOnlyConfig);
        DatabaseSearchRanking exactRanking = this.evaluator.evaluate(query, this.index("diamond", "minecraft:diamond", "minecraft", "", "", List.of()), 1L);
        DatabaseSearchRanking prefixRanking = this.evaluator.evaluate(query, this.index("diamondblade", "minecraft:diamondblade", "minecraft", "", "", List.of()), 1L);
        DatabaseSearchRanking containsRanking = this.evaluator.evaluate(query, this.index("mysticdiamondblade", "minecraft:mysticdiamondblade", "minecraft", "", "", List.of()), 1L);

        assertTrue(exactRanking.exactMatches() > prefixRanking.exactMatches());
        assertTrue(prefixRanking.prefixMatches() > containsRanking.prefixMatches());
        assertTrue(exactRanking.textScore() > prefixRanking.textScore());
        assertTrue(prefixRanking.textScore() > containsRanking.textScore());
    }

    @Test
    void everySearchTermMustMatch() {
        DatabaseQuery query = this.query("diamond sword", DatabaseSearchConfig.defaultConfig());

        DatabaseSearchRanking missingTermRanking = this.evaluator.evaluate(
                query,
                this.index("diamond axe", "minecraft:diamond_axe", "minecraft", "", "", List.of()),
                1L
        );
        DatabaseSearchRanking fullMatchRanking = this.evaluator.evaluate(
                query,
                this.index("diamond sword", "minecraft:diamond_sword", "minecraft", "", "", List.of()),
                1L
        );

        assertFalse(missingTermRanking.matched());
        assertTrue(fullMatchRanking.matched());
        assertTrue(fullMatchRanking.exactMatches() >= 2);
    }

    @Test
    void itemIdAndNamespaceShouldMatchTheirOwnFields() {
        DatabaseSearchConfig idOnlyConfig = DatabaseSearchConfig.defaultConfig()
                .withWeight(DatabaseSearchField.DISPLAY_NAME, DatabaseSearchWeight.OFF)
                .withWeight(DatabaseSearchField.PINYIN, DatabaseSearchWeight.OFF)
                .withWeight(DatabaseSearchField.MOD_NAMESPACE, DatabaseSearchWeight.OFF);
        DatabaseSearchConfig namespaceOnlyConfig = idOnlyConfig
                .withWeight(DatabaseSearchField.ITEM_ID, DatabaseSearchWeight.OFF)
                .withWeight(DatabaseSearchField.MOD_NAMESPACE, DatabaseSearchWeight.HIGH);
        DatabaseSearchIndex diamondSword = this.index("钻石剑", "minecraft:diamond_sword", "minecraft", "zuanshijian", "zsj", List.of("zuan", "shi", "jian"));

        assertTrue(this.evaluator.evaluate(this.query("minecraft:diamond_sword", idOnlyConfig), diamondSword, 1L).matched());
        assertTrue(this.evaluator.evaluate(this.query("diamond", idOnlyConfig), diamondSword, 1L).matched());
        assertTrue(this.evaluator.evaluate(this.query("minecraft", namespaceOnlyConfig), diamondSword, 1L).matched());
        assertFalse(this.evaluator.evaluate(this.query("diamond", namespaceOnlyConfig), diamondSword, 1L).matched());
    }

    @Test
    void pinyinShouldSupportFullSpellAndInitials() {
        DatabaseSearchConfig pinyinOnlyConfig = DatabaseSearchConfig.defaultConfig()
                .withWeight(DatabaseSearchField.DISPLAY_NAME, DatabaseSearchWeight.OFF)
                .withWeight(DatabaseSearchField.ITEM_ID, DatabaseSearchWeight.OFF)
                .withWeight(DatabaseSearchField.MOD_NAMESPACE, DatabaseSearchWeight.OFF)
                .withWeight(DatabaseSearchField.PINYIN, DatabaseSearchWeight.HIGH);
        DatabaseSearchIndex index = this.index("钻石剑", "minecraft:diamond_sword", "minecraft", "zuanshijian", "zsj", List.of("zuan", "shi", "jian"));

        assertTrue(this.evaluator.evaluate(this.query("zuanshijian", pinyinOnlyConfig), index, 1L).matched());
        assertTrue(this.evaluator.evaluate(this.query("zsj", pinyinOnlyConfig), index, 1L).matched());
        assertTrue(this.evaluator.evaluate(this.query("zuan", pinyinOnlyConfig), index, 1L).matched());
    }

    @Test
    void compactNaturalQueriesShouldCountAsExactOrPrefixMatches() {
        DatabaseSearchConfig displayOnlyConfig = DatabaseSearchConfig.defaultConfig()
                .withWeight(DatabaseSearchField.ITEM_ID, DatabaseSearchWeight.OFF)
                .withWeight(DatabaseSearchField.PINYIN, DatabaseSearchWeight.OFF)
                .withWeight(DatabaseSearchField.MOD_NAMESPACE, DatabaseSearchWeight.OFF);
        DatabaseSearchIndex index = this.index("diamond sword", "minecraft:diamond_sword", "minecraft", "", "", List.of());

        DatabaseSearchRanking exactRanking = this.evaluator.evaluate(this.query("diamondsword", displayOnlyConfig), index, 1L);
        DatabaseSearchRanking prefixRanking = this.evaluator.evaluate(this.query("diamondswo", displayOnlyConfig), index, 1L);

        assertTrue(exactRanking.matched());
        assertTrue(exactRanking.exactMatches() > 0);
        assertTrue(exactRanking.fuzzyMatches() == 0);
        assertTrue(prefixRanking.matched());
        assertTrue(prefixRanking.prefixMatches() > 0);
        assertTrue(prefixRanking.fuzzyMatches() == 0);
    }

    @Test
    void compactIdentifierAndFullPinyinQueriesShouldCountAsExactMatches() {
        DatabaseSearchConfig itemIdOnlyConfig = DatabaseSearchConfig.defaultConfig()
                .withWeight(DatabaseSearchField.DISPLAY_NAME, DatabaseSearchWeight.OFF)
                .withWeight(DatabaseSearchField.PINYIN, DatabaseSearchWeight.OFF)
                .withWeight(DatabaseSearchField.MOD_NAMESPACE, DatabaseSearchWeight.OFF);
        DatabaseSearchConfig pinyinOnlyConfig = itemIdOnlyConfig
                .withWeight(DatabaseSearchField.ITEM_ID, DatabaseSearchWeight.OFF)
                .withWeight(DatabaseSearchField.PINYIN, DatabaseSearchWeight.HIGH);
        DatabaseSearchIndex index = this.index("钻石剑", "minecraft:diamond_sword", "minecraft", "zuanshijian", "zsj", List.of("zuan", "shi", "jian"));

        DatabaseSearchRanking itemIdRanking = this.evaluator.evaluate(this.query("diamondsword", itemIdOnlyConfig), index, 1L);
        DatabaseSearchRanking pinyinRanking = this.evaluator.evaluate(this.query("zuanshijian", pinyinOnlyConfig), index, 1L);

        assertTrue(itemIdRanking.matched());
        assertTrue(itemIdRanking.exactMatches() > 0);
        assertTrue(itemIdRanking.fuzzyMatches() == 0);
        assertTrue(pinyinRanking.matched());
        assertTrue(pinyinRanking.exactMatches() > 0);
        assertTrue(pinyinRanking.fuzzyMatches() == 0);
    }

    @Test
    void punctuationOnlyQueriesShouldBehaveLikeEmptySearch() {
        DatabaseSearchRanking ranking = this.evaluator.evaluate(
                this.query("  ::: --- ___  ", DatabaseSearchConfig.defaultConfig()),
                this.index("diamond sword", "minecraft:diamond_sword", "minecraft", "", "", List.of()),
                1L
        );

        assertTrue(ranking.matched());
        assertFalse(ranking.active());
    }

    @Test
    void fuzzyMatchesShouldStayAvailableWithoutExternalLibrary() {
        DatabaseSearchConfig displayOnlyConfig = DatabaseSearchConfig.defaultConfig()
                .withWeight(DatabaseSearchField.ITEM_ID, DatabaseSearchWeight.OFF)
                .withWeight(DatabaseSearchField.PINYIN, DatabaseSearchWeight.OFF)
                .withWeight(DatabaseSearchField.MOD_NAMESPACE, DatabaseSearchWeight.OFF);
        DatabaseSearchRanking fuzzyRanking = this.evaluator.evaluate(
                this.query("dmnd", displayOnlyConfig),
                this.index("diamond", "minecraft:diamond", "minecraft", "", "", List.of()),
                1L
        );

        assertTrue(fuzzyRanking.matched());
        assertTrue(fuzzyRanking.fuzzyMatches() > 0);
    }

    @Test
    void shortTermsShouldNotUseFuzzyMatching() {
        DatabaseSearchConfig displayOnlyConfig = DatabaseSearchConfig.defaultConfig()
                .withWeight(DatabaseSearchField.ITEM_ID, DatabaseSearchWeight.OFF)
                .withWeight(DatabaseSearchField.PINYIN, DatabaseSearchWeight.OFF)
                .withWeight(DatabaseSearchField.MOD_NAMESPACE, DatabaseSearchWeight.OFF);

        DatabaseSearchRanking shortTermRanking = this.evaluator.evaluate(
                this.query("dm", displayOnlyConfig),
                this.index("diamond", "minecraft:diamond", "minecraft", "", "", List.of()),
                1L
        );

        assertFalse(shortTermRanking.matched());
    }

    @Test
    void weightsAndCountBoostShouldInfluenceScores() {
        DatabaseSearchIndex index = this.index("diamond", "minecraft:diamond", "minecraft", "", "", List.of());
        DatabaseSearchConfig highDisplayConfig = DatabaseSearchConfig.defaultConfig()
                .withWeight(DatabaseSearchField.DISPLAY_NAME, DatabaseSearchWeight.HIGH)
                .withWeight(DatabaseSearchField.COUNT_BOOST, DatabaseSearchWeight.HIGH);
        DatabaseSearchConfig lowDisplayConfig = highDisplayConfig.withWeight(DatabaseSearchField.DISPLAY_NAME, DatabaseSearchWeight.LOW);

        DatabaseSearchRanking highWeightRanking = this.evaluator.evaluate(this.query("diamond", highDisplayConfig), index, 64L);
        DatabaseSearchRanking lowWeightRanking = this.evaluator.evaluate(this.query("diamond", lowDisplayConfig), index, 64L);
        DatabaseSearchRanking lowAmountRanking = this.evaluator.evaluate(this.query("diamond", highDisplayConfig), index, 1L);

        assertTrue(highWeightRanking.textScore() > lowWeightRanking.textScore());
        assertTrue(highWeightRanking.countBoostScore() > lowAmountRanking.countBoostScore());
    }

    @Test
    void phraseMatchesShouldOutrankSeparatedTokenMatches() {
        DatabaseSearchConfig displayOnlyConfig = DatabaseSearchConfig.defaultConfig()
                .withWeight(DatabaseSearchField.ITEM_ID, DatabaseSearchWeight.OFF)
                .withWeight(DatabaseSearchField.PINYIN, DatabaseSearchWeight.OFF)
                .withWeight(DatabaseSearchField.MOD_NAMESPACE, DatabaseSearchWeight.OFF);

        DatabaseSearchRanking phraseRanking = this.evaluator.evaluate(
                this.query("diamond sword", displayOnlyConfig),
                this.index("diamond sword", "minecraft:diamond_sword", "minecraft", "", "", List.of()),
                1L
        );
        DatabaseSearchRanking separatedRanking = this.evaluator.evaluate(
                this.query("diamond sword", displayOnlyConfig),
                this.index("diamond axe sword", "minecraft:diamond_axe_sword", "minecraft", "", "", List.of()),
                1L
        );

        assertTrue(phraseRanking.textScore() > separatedRanking.textScore());
    }

    @Test
    void multipleFieldsShouldReceiveCoverageBonus() {
        DatabaseSearchConfig mixedFieldConfig = DatabaseSearchConfig.defaultConfig()
                .withWeight(DatabaseSearchField.DISPLAY_NAME, DatabaseSearchWeight.HIGH)
                .withWeight(DatabaseSearchField.ITEM_ID, DatabaseSearchWeight.HIGH)
                .withWeight(DatabaseSearchField.PINYIN, DatabaseSearchWeight.HIGH)
                .withWeight(DatabaseSearchField.MOD_NAMESPACE, DatabaseSearchWeight.OFF);

        DatabaseSearchRanking multiFieldRanking = this.evaluator.evaluate(
                this.query("diamond zsj", mixedFieldConfig),
                this.index("钻石剑", "minecraft:diamond_sword", "minecraft", "zuanshijian", "zsj", List.of("zuan", "shi", "jian")),
                1L
        );
        DatabaseSearchRanking singleFieldRanking = this.evaluator.evaluate(
                this.query("diamond zsj", mixedFieldConfig),
                this.index("diamond shard zsj", "custom:item", "custom", "", "", List.of()),
                1L
        );

        assertTrue(multiFieldRanking.textScore() > singleFieldRanking.textScore());
    }

    private DatabaseQuery query(String text, DatabaseSearchConfig searchConfig) {
        return new DatabaseQuery(
                DatabaseScope.PERSONAL,
                DatabaseTabs.ALL_TAB_ID,
                List.of(DatabaseTabs.ALL_TAB_ID),
                Map.of(DatabaseTabs.ALL_TAB_ID, 0),
                Map.of(DatabaseTabs.ALL_TAB_ID, DatabaseQuery.DEFAULT_PAGE_SIZE),
                DatabaseSortOption.RECENTLY_CHANGED,
                text,
                searchConfig
        );
    }

    private DatabaseSearchIndex index(
            String displayName,
            String registryName,
            String namespace,
            String pinyinFull,
            String pinyinInitials,
            List<String> pinyinTokens
    ) {
        String registryPath = registryName.substring(registryName.indexOf(':') + 1);
        return new DatabaseSearchIndex(
                displayName,
                SearchTextNormalizer.normalizeNaturalText(displayName),
                SearchTextNormalizer.compactNaturalText(displayName),
                SearchTextNormalizer.tokenizeNaturalText(displayName),
                List.of(SearchTextNormalizer.normalizeNaturalText(displayName)),
                List.of(SearchTextNormalizer.compactNaturalText(displayName)),
                SearchTextNormalizer.normalizeIdentifierText(registryName),
                SearchTextNormalizer.compactIdentifierText(registryName),
                SearchTextNormalizer.normalizeIdentifierText(registryPath),
                SearchTextNormalizer.compactIdentifierText(registryPath),
                SearchTextNormalizer.tokenizeIdentifierText(registryPath),
                SearchTextNormalizer.normalizeIdentifierText(namespace),
                pinyinFull,
                pinyinInitials,
                pinyinTokens
        );
    }
}
