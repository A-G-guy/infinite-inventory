package com.agguy.infiniteinventory.database;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

public record DatabaseEnhancementConfig(long enabledMask) {
    private static final String ENABLED_MASK_KEY = "enabled_mask";
    private static final DatabaseEnhancementConfig DEFAULT = new DatabaseEnhancementConfig(0L);

    public static DatabaseEnhancementConfig defaultConfig() {
        return DEFAULT;
    }

    public boolean isEnabled(DatabaseEnhancementOption option) {
        if (option == null) {
            return false;
        }
        return (this.enabledMask & bitFor(option)) != 0L;
    }

    public DatabaseEnhancementConfig withOption(DatabaseEnhancementOption option, boolean enabled) {
        if (option == null) {
            return this;
        }
        long bit = bitFor(option);
        long updatedMask = enabled ? this.enabledMask | bit : this.enabledMask & ~bit;
        return updatedMask == this.enabledMask ? this : new DatabaseEnhancementConfig(updatedMask);
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putLong(ENABLED_MASK_KEY, this.enabledMask);
        return tag;
    }

    public static DatabaseEnhancementConfig fromTag(CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            return defaultConfig();
        }
        return new DatabaseEnhancementConfig(tag.getLong(ENABLED_MASK_KEY));
    }

    public static DatabaseEnhancementConfig read(FriendlyByteBuf buffer) {
        if (buffer == null) {
            return defaultConfig();
        }
        return new DatabaseEnhancementConfig(buffer.readVarLong());
    }

    public static void write(FriendlyByteBuf buffer, DatabaseEnhancementConfig config) {
        DatabaseEnhancementConfig normalizedConfig = config == null ? defaultConfig() : config;
        buffer.writeVarLong(normalizedConfig.enabledMask);
    }

    private static long bitFor(DatabaseEnhancementOption option) {
        return 1L << option.ordinal();
    }
}
