package com.agguy.infiniteinventory.database;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

/**
 * JEI 合成材料提取源页签配置。
 * 每个作用域维护一组允许作为自动提取来源的页签 ID。
 */
public final class JeiCraftingTabSourceConfig {
    private static final String PERSONAL_KEY = "personal";
    private static final String PUBLIC_KEY = "public";

    private final Map<DatabaseScope, Set<String>> enabledTabIds;

    private JeiCraftingTabSourceConfig(Map<DatabaseScope, Set<String>> enabledTabIds) {
        this.enabledTabIds = new LinkedHashMap<>();
        for (DatabaseScope scope : DatabaseScope.values()) {
            this.enabledTabIds.put(scope, new LinkedHashSet<>(enabledTabIds.getOrDefault(scope, Set.of())));
        }
    }

    public static JeiCraftingTabSourceConfig empty() {
        return new JeiCraftingTabSourceConfig(Map.of());
    }

    public static JeiCraftingTabSourceConfig allEnabled() {
        Map<DatabaseScope, Set<String>> all = new LinkedHashMap<>();
        for (DatabaseScope scope : DatabaseScope.values()) {
            all.put(scope, new LinkedHashSet<>());
            all.get(scope).add(DatabaseTabs.ALL_TAB_ID);
        }
        return new JeiCraftingTabSourceConfig(all);
    }

    public boolean isTabEnabled(DatabaseScope scope, String tabId) {
        Set<String> tabs = this.enabledTabIds.get(DatabaseScope.normalize(scope));
        return tabs != null && tabs.contains(tabId);
    }

    public JeiCraftingTabSourceConfig withTabEnabled(DatabaseScope scope, String tabId, boolean enabled) {
        DatabaseScope normalizedScope = DatabaseScope.normalize(scope);
        Set<String> current = this.enabledTabIds.getOrDefault(normalizedScope, new LinkedHashSet<>());
        Set<String> updated = new LinkedHashSet<>(current);
        if (enabled) {
            updated.add(tabId);
        } else {
            updated.remove(tabId);
        }
        if (updated.equals(current)) {
            return this;
        }
        Map<DatabaseScope, Set<String>> copy = new LinkedHashMap<>(this.enabledTabIds);
        copy.put(normalizedScope, updated);
        return new JeiCraftingTabSourceConfig(copy);
    }

    public Set<String> enabledTabIdsFor(DatabaseScope scope) {
        return Collections.unmodifiableSet(this.enabledTabIds.getOrDefault(DatabaseScope.normalize(scope), Set.of()));
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        for (DatabaseScope scope : DatabaseScope.values()) {
            String key = scope == DatabaseScope.PERSONAL ? PERSONAL_KEY : PUBLIC_KEY;
            ListTag list = new ListTag();
            for (String tabId : this.enabledTabIds.getOrDefault(scope, Set.of())) {
                list.add(StringTag.valueOf(tabId));
            }
            tag.put(key, list);
        }
        return tag;
    }

    public static JeiCraftingTabSourceConfig fromTag(CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            return allEnabled();
        }
        Map<DatabaseScope, Set<String>> map = new LinkedHashMap<>();
        for (DatabaseScope scope : DatabaseScope.values()) {
            String key = scope == DatabaseScope.PERSONAL ? PERSONAL_KEY : PUBLIC_KEY;
            Set<String> tabs = new LinkedHashSet<>();
            ListTag list = tag.getList(key, Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                tabs.add(list.getString(i));
            }
            map.put(scope, tabs);
        }
        return new JeiCraftingTabSourceConfig(map);
    }

    public void write(net.minecraft.network.RegistryFriendlyByteBuf buffer) {
        for (DatabaseScope scope : DatabaseScope.values()) {
            Set<String> tabs = this.enabledTabIds.getOrDefault(scope, Set.of());
            buffer.writeVarInt(tabs.size());
            for (String tabId : tabs) {
                buffer.writeUtf(tabId, com.agguy.infiniteinventory.database.DatabaseQuery.MAX_TAB_ID_LENGTH);
            }
        }
    }

    public static JeiCraftingTabSourceConfig read(net.minecraft.network.RegistryFriendlyByteBuf buffer) {
        Map<DatabaseScope, Set<String>> map = new LinkedHashMap<>();
        for (DatabaseScope scope : DatabaseScope.values()) {
            int count = buffer.readVarInt();
            Set<String> tabs = new LinkedHashSet<>();
            for (int i = 0; i < count; i++) {
                tabs.add(buffer.readUtf(com.agguy.infiniteinventory.database.DatabaseQuery.MAX_TAB_ID_LENGTH));
            }
            map.put(scope, tabs);
        }
        return new JeiCraftingTabSourceConfig(map);
    }
}
