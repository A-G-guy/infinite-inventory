package com.agguy.infiniteinventory.registry;

import com.agguy.infiniteinventory.InfiniteInventory;
import com.agguy.infiniteinventory.database.DatabaseViewPreferencesAttachment;
import com.agguy.infiniteinventory.database.PlayerDatabaseAttachment;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> REGISTER = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, InfiniteInventory.MODID);
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerDatabaseAttachment>> PERSONAL_DATABASE = REGISTER.register(
            "personal_database",
            () -> AttachmentType.serializable(PlayerDatabaseAttachment::new).copyOnDeath().build()
    );
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<DatabaseViewPreferencesAttachment>> DATABASE_VIEW_PREFERENCES = REGISTER.register(
            "database_view_preferences",
            () -> AttachmentType.serializable(DatabaseViewPreferencesAttachment::new).copyOnDeath().build()
    );

    private ModAttachments() {
    }
}
