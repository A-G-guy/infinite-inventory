package com.agguy.infiniteinventory.item;

import com.agguy.infiniteinventory.service.PersonalDatabaseService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class DatabaseAccessItem extends Item {
    private static final Logger LOGGER = LogManager.getLogger();

    public DatabaseAccessItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            try {
                PersonalDatabaseService.INSTANCE.open(serverPlayer);
            } catch (Exception exception) {
                LOGGER.error("玩家 {} 右键使用数据库终端时打开菜单失败", serverPlayer.getGameProfile().getName(), exception);
                serverPlayer.sendSystemMessage(Component.translatable("message.infiniteinventory.database.open_failed"));
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
