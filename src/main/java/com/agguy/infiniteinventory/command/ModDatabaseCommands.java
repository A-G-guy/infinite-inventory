package com.agguy.infiniteinventory.command;

import com.agguy.infiniteinventory.InfiniteInventory;
import com.agguy.infiniteinventory.database.DatabaseBackupInfo;
import com.agguy.infiniteinventory.database.DatabaseBackupManager;
import com.agguy.infiniteinventory.service.PersonalDatabaseService;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = InfiniteInventory.MODID)
public final class ModDatabaseCommands {
    private static final DateTimeFormatter BACKUP_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
    private static final SuggestionProvider<CommandSourceStack> SNAPSHOT_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(
                    DatabaseBackupManager.listBackups(context.getSource().getServer()).stream().map(DatabaseBackupInfo::fileName),
                    builder
            );

    private ModDatabaseCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("infiniteinventory")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("database")
                        .then(Commands.literal("backup")
                                .then(Commands.literal("now").executes(context -> createManualBackup(context.getSource())))
                                .then(Commands.literal("list").executes(context -> listBackups(context.getSource()))))
                        .then(Commands.literal("restore")
                                .then(Commands.argument("snapshot", StringArgumentType.word())
                                        .suggests(SNAPSHOT_SUGGESTIONS)
                                        .executes(context -> restoreBackup(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "snapshot")
                                        ))))));
    }

    private static int createManualBackup(CommandSourceStack source) {
        try {
            Path backupPath = DatabaseBackupManager.createManualBackup(source.getServer(), "manual-command");
            source.sendSuccess(() -> Component.translatable(
                    "command.infiniteinventory.database.backup.created",
                    backupPath.getFileName().toString()
            ), false);
            return 1;
        } catch (IOException exception) {
            source.sendFailure(Component.translatable(
                    "command.infiniteinventory.database.error",
                    exception.getMessage()
            ));
            return 0;
        }
    }

    private static int listBackups(CommandSourceStack source) {
        List<DatabaseBackupInfo> backups = DatabaseBackupManager.listBackups(source.getServer());
        if (backups.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("command.infiniteinventory.database.backup.empty"), false);
            return 0;
        }
        source.sendSuccess(() -> Component.translatable(
                "command.infiniteinventory.database.backup.list_header",
                backups.size()
        ), false);
        backups.stream().limit(20).forEach(backupInfo -> source.sendSuccess(() -> Component.translatable(
                "command.infiniteinventory.database.backup.list_entry",
                backupInfo.fileName(),
                backupInfo.type().name().toLowerCase(java.util.Locale.ROOT),
                backupInfo.reason(),
                BACKUP_TIME_FORMATTER.format(Instant.ofEpochMilli(backupInfo.createdAtMillis()))
        ), false));
        return backups.size();
    }

    private static int restoreBackup(CommandSourceStack source, String snapshotFileName) {
        try {
            Path preRestoreBackup = DatabaseBackupManager.createPreRestoreBackup(source.getServer(), "before-restore");
            DatabaseBackupInfo restoredBackup = DatabaseBackupManager.restoreBackup(source.getServer(), snapshotFileName);
            PersonalDatabaseService.INSTANCE.syncAllViewersAndNotifyCurrentScope(source.getServer());
            source.sendSuccess(() -> Component.translatable(
                    "command.infiniteinventory.database.restore.completed",
                    restoredBackup.fileName(),
                    preRestoreBackup.getFileName().toString()
            ), true);
            return 1;
        } catch (IOException exception) {
            source.sendFailure(Component.translatable(
                    "command.infiniteinventory.database.error",
                    exception.getMessage()
            ));
            return 0;
        }
    }
}
