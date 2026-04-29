package com.agguy.infiniteinventory.service;

import com.agguy.infiniteinventory.database.DatabaseLogAction;
import com.agguy.infiniteinventory.database.DatabaseLogEntry;
import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.StoredItemDatabase;
import com.agguy.infiniteinventory.tests.MinecraftTestBootstrap;
import com.mojang.authlib.GameProfile;
import java.lang.reflect.Method;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link PersonalDatabaseServiceLogHelper} 测试：验证日志条目中的玩家名使用实际游戏名。
 */
class PersonalDatabaseServiceLogHelperTest {
    static {
        MinecraftTestBootstrap.ensureBootstrapped();
    }

    @Test
    void recordLogShouldUseActualPlayerNameInsteadOfUuidPrefix() throws ReflectiveOperationException {
        UUID playerId = UUID.fromString("12345678-1234-1234-1234-123456789abc");
        String expectedName = "TestPlayer";

        ServerPlayer player = mock(ServerPlayer.class);
        GameProfile gameProfile = mock(GameProfile.class);
        when(player.getUUID()).thenReturn(playerId);
        when(player.getGameProfile()).thenReturn(gameProfile);
        when(gameProfile.getName()).thenReturn(expectedName);

        StoredItemDatabase database = new StoredItemDatabase();

        PersonalDatabaseService service = mock(PersonalDatabaseService.class);
        when(service.resolveDatabaseForMutation(player, DatabaseScope.PERSONAL)).thenReturn(database);

        Class<?> helperClass = Class.forName(
                "com.agguy.infiniteinventory.service.PersonalDatabaseServiceLogHelper");
        Method recordLog = helperClass.getDeclaredMethod(
                "recordLog",
                PersonalDatabaseService.class,
                ServerPlayer.class,
                DatabaseScope.class,
                DatabaseLogAction.class,
                ItemStack.class,
                long.class,
                String.class,
                String.class,
                DatabaseScope.class
        );
        recordLog.setAccessible(true);

        recordLog.invoke(
                null,
                service,
                player,
                DatabaseScope.PERSONAL,
                DatabaseLogAction.DEPOSIT,
                new ItemStack(Items.DIAMOND, 16),
                16L,
                "source_tab",
                "target_tab",
                null
        );

        assertEquals(1, database.logEntries().size());
        DatabaseLogEntry entry = database.logEntries().get(0);
        assertEquals(expectedName, entry.playerNameSnapshot(),
                "日志中的玩家名应使用实际游戏名，而非 UUID 前缀");
        assertEquals(playerId, entry.playerId());
        assertEquals(DatabaseLogAction.DEPOSIT, entry.action());
    }

    @Test
    void recordLogShouldHandlePlayerNameWithUnicode() throws ReflectiveOperationException {
        UUID playerId = UUID.randomUUID();
        String unicodeName = "玩家名字_测试";

        ServerPlayer player = mock(ServerPlayer.class);
        GameProfile gameProfile = mock(GameProfile.class);
        when(player.getUUID()).thenReturn(playerId);
        when(player.getGameProfile()).thenReturn(gameProfile);
        when(gameProfile.getName()).thenReturn(unicodeName);

        StoredItemDatabase database = new StoredItemDatabase();

        PersonalDatabaseService service = mock(PersonalDatabaseService.class);
        when(service.resolveDatabaseForMutation(player, DatabaseScope.PUBLIC)).thenReturn(database);

        Class<?> helperClass = Class.forName(
                "com.agguy.infiniteinventory.service.PersonalDatabaseServiceLogHelper");
        Method recordLog = helperClass.getDeclaredMethod(
                "recordLog",
                PersonalDatabaseService.class,
                ServerPlayer.class,
                DatabaseScope.class,
                DatabaseLogAction.class,
                ItemStack.class,
                long.class,
                String.class,
                String.class,
                DatabaseScope.class
        );
        recordLog.setAccessible(true);

        recordLog.invoke(
                null,
                service,
                player,
                DatabaseScope.PUBLIC,
                DatabaseLogAction.EXTRACT,
                new ItemStack(Items.STONE, 8),
                8L,
                "",
                "",
                null
        );

        assertEquals(1, database.logEntries().size());
        DatabaseLogEntry entry = database.logEntries().get(0);
        assertEquals(unicodeName, entry.playerNameSnapshot(),
                "日志应正确保存含 Unicode 字符的玩家名");
    }
}
