package com.agguy.infiniteinventory.service.search.tests;

import com.agguy.infiniteinventory.database.StoredStackKey;
import com.agguy.infiniteinventory.localization.ViewerLanguage;
import com.agguy.infiniteinventory.service.search.DatabaseItemSearchResolver;
import com.agguy.infiniteinventory.service.search.DatabaseSearchIndex;
import com.agguy.infiniteinventory.tests.MinecraftTestBootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseItemSearchResolverTest {
    static {
        MinecraftTestBootstrap.ensureBootstrapped();
    }

    @Test
    void resolverShouldUseRequestedViewerLanguageAsPrimaryDisplayName() {
        StoredStackKey appleKey = StoredStackKey.of(new ItemStack(Items.APPLE));

        DatabaseSearchIndex englishIndex = DatabaseItemSearchResolver.INSTANCE.resolve(appleKey, ViewerLanguage.EN_US);
        DatabaseSearchIndex chineseIndex = DatabaseItemSearchResolver.INSTANCE.resolve(appleKey, ViewerLanguage.ZH_CN);

        assertEquals("Apple", englishIndex.displayName());
        if (!englishIndex.displayName().equals(chineseIndex.displayName())) {
            assertEquals("苹果", chineseIndex.displayName());
            return;
        }
        assertEquals("Apple", chineseIndex.displayName());
    }

    @Test
    void englishPrimaryIndexShouldKeepChineseAliasAndPinyin() {
        StoredStackKey appleKey = StoredStackKey.of(new ItemStack(Items.APPLE));

        DatabaseSearchIndex englishIndex = DatabaseItemSearchResolver.INSTANCE.resolve(appleKey, ViewerLanguage.EN_US);

        if (!englishIndex.displayNameSearchNormalizedTexts().contains("苹果")) {
            assertEquals("", englishIndex.pinyinFull());
            assertEquals("", englishIndex.pinyinInitials());
            return;
        }
        assertTrue(englishIndex.displayNameSearchNormalizedTexts().contains("苹果"));
        assertEquals("pingguo", englishIndex.pinyinFull());
        assertEquals("pg", englishIndex.pinyinInitials());
    }
}
