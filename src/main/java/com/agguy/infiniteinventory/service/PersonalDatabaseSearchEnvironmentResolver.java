package com.agguy.infiniteinventory.service;

import com.agguy.infiniteinventory.service.search.DatabaseSearchEnvironment;
import net.minecraft.server.level.ServerPlayer;

final class PersonalDatabaseSearchEnvironmentResolver {
    private PersonalDatabaseSearchEnvironmentResolver() {
    }

    static DatabaseSearchEnvironment resolve(ServerPlayer player) {
        return new DatabaseSearchEnvironment(
                player.serverLevel().getServer().getWorldData().enabledFeatures(),
                player.hasPermissions(2),
                player.level().registryAccess()
        );
    }
}
