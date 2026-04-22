package com.agguy.infiniteinventory.database;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

public record DatabaseTab(
        String id,
        String customName,
        String translationKey,
        String iconItemId,
        boolean systemTab,
        boolean protectedTab
) {
    private static final String ID_KEY = "id";
    private static final String CUSTOM_NAME_KEY = "custom_name";
    private static final String TRANSLATION_KEY_KEY = "translation_key";
    private static final String ICON_ITEM_ID_KEY = "icon_item_id";
    private static final String SYSTEM_TAB_KEY = "system_tab";
    private static final String PROTECTED_TAB_KEY = "protected_tab";

    public static final int MAX_NETWORK_TEXT_LENGTH = 128;

    public DatabaseTab {
        boolean allTab = DatabaseTabs.isAllTabId(id);
        boolean favoritesTab = DatabaseTabs.isFavoritesTabId(id);
        id = DatabaseTabs.normalizeTabId(id, allTab ? DatabaseTabs.ALL_TAB_ID : DatabaseTabs.DEFAULT_TAB_ID);
        customName = DatabaseTabs.normalizeTabName(customName, translationKey);
        translationKey = translationKey == null ? "" : translationKey.trim();
        iconItemId = DatabaseTabs.normalizeIconItemId(iconItemId, allTab, favoritesTab);
    }

    public boolean isAllTab() {
        return DatabaseTabs.isAllTabId(this.id);
    }

    public boolean isFavoritesTab() {
        return DatabaseTabs.isFavoritesTabId(this.id);
    }

    public boolean isSystemTab() {
        return DatabaseTabs.isSystemTabId(this.id);
    }

    public boolean isConcreteTab() {
        return !this.isSystemTab();
    }

    public boolean usesTranslationKey() {
        return !this.translationKey.isBlank() && this.customName.isBlank();
    }

    public boolean canRename() {
        return !this.isSystemTab();
    }

    public boolean canDelete() {
        return this.isConcreteTab() && !this.protectedTab;
    }

    public String displayName() {
        return this.customName.isBlank() ? this.translationKey : this.customName;
    }

    public DatabaseTab withName(String name) {
        if (this.isSystemTab()) {
            return this;
        }
        String normalizedName = DatabaseTabs.normalizeTabName(name, this.translationKey);
        if (normalizedName.isBlank()) {
            return this;
        }
        return new DatabaseTab(this.id, normalizedName, "", this.iconItemId, this.systemTab, this.protectedTab);
    }

    public DatabaseTab withIconItemId(String newIconItemId) {
        return new DatabaseTab(
                this.id,
                this.customName,
                this.translationKey,
                DatabaseTabs.normalizeIconItemId(newIconItemId, this.isAllTab(), this.isFavoritesTab()),
                this.systemTab,
                this.protectedTab
        );
    }

    public static DatabaseTab read(FriendlyByteBuf buffer) {
        return new DatabaseTab(
                buffer.readUtf(MAX_NETWORK_TEXT_LENGTH),
                buffer.readUtf(MAX_NETWORK_TEXT_LENGTH),
                buffer.readUtf(MAX_NETWORK_TEXT_LENGTH),
                buffer.readUtf(MAX_NETWORK_TEXT_LENGTH),
                buffer.readBoolean(),
                buffer.readBoolean()
        );
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeUtf(this.id, MAX_NETWORK_TEXT_LENGTH);
        buffer.writeUtf(this.customName, MAX_NETWORK_TEXT_LENGTH);
        buffer.writeUtf(this.translationKey, MAX_NETWORK_TEXT_LENGTH);
        buffer.writeUtf(this.iconItemId, MAX_NETWORK_TEXT_LENGTH);
        buffer.writeBoolean(this.systemTab);
        buffer.writeBoolean(this.protectedTab);
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString(ID_KEY, this.id);
        tag.putString(CUSTOM_NAME_KEY, this.customName);
        tag.putString(TRANSLATION_KEY_KEY, this.translationKey);
        tag.putString(ICON_ITEM_ID_KEY, this.iconItemId);
        tag.putBoolean(SYSTEM_TAB_KEY, this.systemTab);
        tag.putBoolean(PROTECTED_TAB_KEY, this.protectedTab);
        return tag;
    }

    public static DatabaseTab fromTag(CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            return DatabaseTabs.defaultConcreteTab();
        }
        return new DatabaseTab(
                tag.getString(ID_KEY),
                tag.getString(CUSTOM_NAME_KEY),
                tag.getString(TRANSLATION_KEY_KEY),
                tag.getString(ICON_ITEM_ID_KEY),
                tag.getBoolean(SYSTEM_TAB_KEY),
                tag.getBoolean(PROTECTED_TAB_KEY)
        );
    }
}
