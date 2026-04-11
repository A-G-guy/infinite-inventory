package com.agguy.infiniteinventory.database;

import java.util.Objects;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public final class PublicDatabaseSavedData extends SavedData {
    private static final String DATA_NAME = "infiniteinventory_public_database";
    private static final String DATABASE_KEY = "database";

    private final StoredItemDatabase database = new StoredItemDatabase();

    private PublicDatabaseSavedData() {
    }

    private PublicDatabaseSavedData(CompoundTag tag, HolderLookup.Provider provider) {
        this.database.deserializeNBT(provider, tag.getCompound(DATABASE_KEY));
    }

    public static PublicDatabaseSavedData get(MinecraftServer server) {
        ServerLevel overworld = Objects.requireNonNull(server.overworld(), "The overworld must exist before database access");
        SavedData.Factory<PublicDatabaseSavedData> factory = new SavedData.Factory<>(
                PublicDatabaseSavedData::new,
                PublicDatabaseSavedData::new
        );
        return overworld.getDataStorage().computeIfAbsent(factory, DATA_NAME);
    }

    public StoredItemDatabase database() {
        return this.database;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.put(DATABASE_KEY, this.database.serializeNBT(provider));
        return tag;
    }
}
