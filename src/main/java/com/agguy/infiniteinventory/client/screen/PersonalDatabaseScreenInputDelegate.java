package com.agguy.infiniteinventory.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.jetbrains.annotations.Nullable;

/**
 * 输入事件委托类，负责所有鼠标/键盘事件的捕获与分发。
 *
 * <p>设计决策：将输入处理逻辑从屏幕主类剥离，保持主类仅作为调度中心。
 */
final class PersonalDatabaseScreenInputDelegate {
    private PersonalDatabaseScreenInputDelegate() {
    }

    static boolean mouseClicked(PersonalDatabaseScreen screen, double mouseX, double mouseY, int button) {
        return PersonalDatabaseScreenInteractionHelper.mouseClicked(screen, mouseX, mouseY, button);
    }

    static boolean mouseScrolled(PersonalDatabaseScreen screen, double mouseX, double mouseY, double scrollX, double scrollY) {
        return PersonalDatabaseScreenInteractionHelper.mouseScrolled(screen, mouseX, mouseY, scrollX, scrollY);
    }

    static boolean mouseDragged(PersonalDatabaseScreen screen, double mouseX, double mouseY, int button, double dragX, double dragY) {
        return PersonalDatabaseScreenInteractionHelper.mouseDragged(screen, mouseX, mouseY, button, dragX, dragY);
    }

    static boolean mouseReleased(PersonalDatabaseScreen screen, double mouseX, double mouseY, int button) {
        return PersonalDatabaseScreenInteractionHelper.mouseReleased(screen, mouseX, mouseY, button);
    }

    static boolean keyPressed(PersonalDatabaseScreen screen, int keyCode, int scanCode, int modifiers) {
        return PersonalDatabaseScreenInteractionHelper.keyPressed(screen, keyCode, scanCode, modifiers);
    }

    static boolean keyReleased(PersonalDatabaseScreen screen, int keyCode, int scanCode, int modifiers) {
        return PersonalDatabaseScreenInteractionHelper.keyReleased(screen, keyCode, scanCode, modifiers);
    }

    static boolean charTyped(PersonalDatabaseScreen screen, char codePoint, int modifiers) {
        return PersonalDatabaseScreenInteractionHelper.charTyped(screen, codePoint, modifiers);
    }
}
