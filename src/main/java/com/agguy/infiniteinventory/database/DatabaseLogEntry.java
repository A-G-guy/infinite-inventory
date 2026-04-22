package com.agguy.infiniteinventory.database;

import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 数据库操作日志条目，记录一次具体的存/取/转移/删除操作。
 * <p>
 * 使用 {@link ItemStack} 快照保存物品信息，避免仅依赖注册名导致模组移除后无法回溯。
 * 网络传输时 stackSnapshot 数量固定为 1，amount 字段单独记录实际变更数量。
 */
public record DatabaseLogEntry(
        long timestampMillis,
        UUID playerId,
        String playerNameSnapshot,
        DatabaseLogAction action,
        ItemStack stackSnapshot,
        long amount,
        String sourceTabId,
        String targetTabId,
        @Nullable DatabaseScope relatedScope
) {
    private static final String TIMESTAMP_KEY = "timestamp";
    private static final String PLAYER_ID_KEY = "player_id";
    private static final String PLAYER_NAME_KEY = "player_name";
    private static final String ACTION_KEY = "action";
    private static final String STACK_KEY = "stack";
    private static final String AMOUNT_KEY = "amount";
    private static final String SOURCE_TAB_KEY = "source_tab";
    private static final String TARGET_TAB_KEY = "target_tab";
    private static final String RELATED_SCOPE_KEY = "related_scope";

    public static final int MAX_NETWORK_TEXT_LENGTH = 128;

    public DatabaseLogEntry {
        playerId = playerId == null ? new UUID(0L, 0L) : playerId;
        playerNameSnapshot = playerNameSnapshot == null ? "" : playerNameSnapshot.trim();
        action = DatabaseLogAction.normalize(action);
        stackSnapshot = stackSnapshot == null || stackSnapshot.isEmpty() ? ItemStack.EMPTY : stackSnapshot.copyWithCount(1);
        amount = Math.max(0L, amount);
        sourceTabId = sourceTabId == null ? "" : sourceTabId.trim();
        targetTabId = targetTabId == null ? "" : targetTabId.trim();
    }

    public boolean isEmpty() {
        return this.stackSnapshot.isEmpty() || this.amount <= 0L;
    }

    public CompoundTag write(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putLong(TIMESTAMP_KEY, this.timestampMillis);
        tag.putUUID(PLAYER_ID_KEY, this.playerId);
        tag.putString(PLAYER_NAME_KEY, this.playerNameSnapshot);
        tag.putString(ACTION_KEY, this.action.name());
        if (!this.stackSnapshot.isEmpty() && provider != null) {
            Tag serializedStack = this.stackSnapshot.saveOptional(provider);
            if (serializedStack instanceof CompoundTag stackTag) {
                tag.put(STACK_KEY, stackTag);
            }
        }
        tag.putLong(AMOUNT_KEY, this.amount);
        tag.putString(SOURCE_TAB_KEY, this.sourceTabId);
        tag.putString(TARGET_TAB_KEY, this.targetTabId);
        if (this.relatedScope != null) {
            tag.putString(RELATED_SCOPE_KEY, this.relatedScope.name());
        }
        return tag;
    }

    public static DatabaseLogEntry read(HolderLookup.Provider provider, CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            return null;
        }
        long timestamp = tag.getLong(TIMESTAMP_KEY);
        UUID playerId = tag.contains(PLAYER_ID_KEY) ? tag.getUUID(PLAYER_ID_KEY) : new UUID(0L, 0L);
        String playerName = tag.getString(PLAYER_NAME_KEY);
        DatabaseLogAction action = DatabaseLogAction.read(tag.getString(ACTION_KEY));
        ItemStack stack = ItemStack.EMPTY;
        if (tag.contains(STACK_KEY, Tag.TAG_COMPOUND) && provider != null) {
            stack = ItemStack.parseOptional(provider, tag.getCompound(STACK_KEY).copy());
        }
        long amount = Math.max(0L, tag.getLong(AMOUNT_KEY));
        String sourceTabId = tag.getString(SOURCE_TAB_KEY);
        String targetTabId = tag.getString(TARGET_TAB_KEY);
        DatabaseScope relatedScope = null;
        if (tag.contains(RELATED_SCOPE_KEY)) {
            relatedScope = DatabaseScope.normalize(DatabaseScope.read(tag.getString(RELATED_SCOPE_KEY), null));
        }
        DatabaseLogEntry entry = new DatabaseLogEntry(
                timestamp, playerId, playerName, action, stack, amount, sourceTabId, targetTabId, relatedScope
        );
        return entry.isEmpty() ? null : entry;
    }

    public void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeVarLong(this.timestampMillis);
        buffer.writeUUID(this.playerId);
        buffer.writeUtf(this.playerNameSnapshot, MAX_NETWORK_TEXT_LENGTH);
        buffer.writeUtf(this.action.name(), MAX_NETWORK_TEXT_LENGTH);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, this.stackSnapshot);
        buffer.writeVarLong(this.amount);
        buffer.writeUtf(this.sourceTabId, MAX_NETWORK_TEXT_LENGTH);
        buffer.writeUtf(this.targetTabId, MAX_NETWORK_TEXT_LENGTH);
        buffer.writeBoolean(this.relatedScope != null);
        if (this.relatedScope != null) {
            buffer.writeUtf(this.relatedScope.name(), MAX_NETWORK_TEXT_LENGTH);
        }
    }

    public static DatabaseLogEntry read(RegistryFriendlyByteBuf buffer) {
        long timestamp = buffer.readVarLong();
        UUID playerId = buffer.readUUID();
        String playerName = buffer.readUtf(MAX_NETWORK_TEXT_LENGTH);
        DatabaseLogAction action = DatabaseLogAction.read(buffer.readUtf(MAX_NETWORK_TEXT_LENGTH));
        ItemStack stack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
        long amount = buffer.readVarLong();
        String sourceTabId = buffer.readUtf(MAX_NETWORK_TEXT_LENGTH);
        String targetTabId = buffer.readUtf(MAX_NETWORK_TEXT_LENGTH);
        DatabaseScope relatedScope = null;
        if (buffer.readBoolean()) {
            relatedScope = DatabaseScope.normalize(DatabaseScope.read(buffer.readUtf(MAX_NETWORK_TEXT_LENGTH), null));
        }
        DatabaseLogEntry entry = new DatabaseLogEntry(
                timestamp, playerId, playerName, action, stack, amount, sourceTabId, targetTabId, relatedScope
        );
        return entry.isEmpty() ? null : entry;
    }

    public static ListTag writeList(HolderLookup.Provider provider, java.util.List<DatabaseLogEntry> entries) {
        ListTag list = new ListTag();
        for (DatabaseLogEntry entry : entries) {
            if (entry == null || entry.isEmpty()) {
                continue;
            }
            list.add(entry.write(provider));
        }
        return list;
    }

    public static java.util.List<DatabaseLogEntry> readList(HolderLookup.Provider provider, ListTag list) {
        if (list == null || list.isEmpty()) {
            return java.util.List.of();
        }
        java.util.ArrayList<DatabaseLogEntry> entries = new java.util.ArrayList<>(list.size());
        for (Tag element : list) {
            if (!(element instanceof CompoundTag entryTag)) {
                continue;
            }
            DatabaseLogEntry entry = read(provider, entryTag);
            if (entry != null) {
                entries.add(entry);
            }
        }
        return java.util.List.copyOf(entries);
    }
}
