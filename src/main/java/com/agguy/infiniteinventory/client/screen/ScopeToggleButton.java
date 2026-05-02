package com.agguy.infiniteinventory.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * 透明点击按钮：仅消费点击事件，不自行渲染背景与文字。
 * 背景与标签由外部的 renderScopeToggles 统一绘制，确保样式一致。
 */
final class ScopeToggleButton extends Button {
    ScopeToggleButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 不渲染任何内容，依赖外部手动绘制
    }
}
