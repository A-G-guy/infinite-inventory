package com.agguy.infiniteinventory.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * 支持在文字左侧绘制 Remix Icon 的按钮组件。
 *
 * <p>继承自原版 {@link Button}，覆盖 {@link #renderString} 方法实现图标+文字的
 * 联合居中渲染。图标尺寸固定为 14x14，图标与文字间距为 4 像素。
 *
 * <p>若图标为 null 或按钮消息为空，则回退到原版纯文字渲染，保持与现有行为的兼容性。
 */
final class IconButton extends Button {
    private static final int ICON_SIZE = 14;
    private static final int ICON_TEXT_GAP = 4;

    private @Nullable RemixIcon icon;

    private IconButton(
            int x,
            int y,
            int width,
            int height,
            Component message,
            OnPress onPress,
            CreateNarration narration,
            @Nullable RemixIcon icon
    ) {
        super(x, y, width, height, message, onPress, narration);
        this.icon = icon;
    }

    static IconButton create(
            int x,
            int y,
            int width,
            int height,
            Component message,
            OnPress onPress,
            @Nullable RemixIcon icon
    ) {
        return new IconButton(x, y, width, height, message, onPress, DEFAULT_NARRATION, icon);
    }

    @Nullable
    RemixIcon icon() {
        return icon;
    }

    void setIcon(@Nullable RemixIcon icon) {
        this.icon = icon;
    }

    @Override
    public void renderString(GuiGraphics guiGraphics, Font font, int color) {
        if (this.icon == null) {
            super.renderString(guiGraphics, font, color);
            return;
        }

        int textY = this.getY() + (this.getHeight() - 8) / 2;
        int iconY = this.getY() + (this.getHeight() - ICON_SIZE) / 2;

        if (this.getMessage().getString().isEmpty()) {
            int iconX = this.getX() + (this.getWidth() - ICON_SIZE) / 2;
            VanillaWidgetRenderer.renderRemixIcon(guiGraphics, this.icon, iconX, iconY, ICON_SIZE);
            return;
        }

        int textWidth = font.width(this.getMessage());
        int totalWidth = ICON_SIZE + ICON_TEXT_GAP + textWidth;
        int contentX = this.getX() + (this.getWidth() - totalWidth) / 2;

        VanillaWidgetRenderer.renderRemixIcon(guiGraphics, this.icon, contentX, iconY, ICON_SIZE);
        guiGraphics.drawString(font, this.getMessage(), contentX + ICON_SIZE + ICON_TEXT_GAP, textY, color);
    }
}
