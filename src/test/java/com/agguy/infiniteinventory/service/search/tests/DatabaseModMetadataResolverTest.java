package com.agguy.infiniteinventory.service.search.tests;

import com.agguy.infiniteinventory.service.search.DatabaseModMetadataResolver;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DatabaseModMetadataResolver 白盒测试。
 *
 * <p>无 NeoForge 运行时，ModList.get() 返回 null，resolver 返回空候选集。
 * 测试覆盖 null/空白 namespace 等边界场景。</p>
 */
class DatabaseModMetadataResolverTest {

    private final DatabaseModMetadataResolver resolver = DatabaseModMetadataResolver.INSTANCE;

    @Test
    void displayNameForNamespaceShouldReturnEmptyForNull() {
        assertEquals("", resolver.displayNameForNamespace(null));
    }

    @Test
    void displayNameForNamespaceShouldReturnEmptyForBlank() {
        assertEquals("", resolver.displayNameForNamespace("   "));
    }

    @Test
    void displayNameForNamespaceShouldReturnFallbackWhenNotLoaded() {
        // 无 NeoForge 运行时 ModList.get() 为 null，resolver 无数据，
        // displayNameForNamespace 返回 fallback (namespace 本身)
        String result = resolver.displayNameForNamespace("testmod");
        assertEquals("testmod", result);
    }

    @Test
    void modDisplayNameCandidatesShouldReturnEmptySetWhenNotLoaded() {
        Set<String> candidates = resolver.modDisplayNameCandidates();
        assertTrue(candidates.isEmpty());
    }
}
