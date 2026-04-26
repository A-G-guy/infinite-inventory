package com.agguy.infiniteinventory.client.screen;

import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 提供 {@link PersonalDatabaseScreen} 对受保护的原版父类方法的访问代理。
 *
 * <p>设计意图：将大量单行代理方法从屏幕主类剥离，控制类规模。
 */
final class PersonalDatabaseScreenSuperAccess {
    private final PersonalDatabaseScreen screen;

    PersonalDatabaseScreenSuperAccess(PersonalDatabaseScreen screen) {
        this.screen = screen;
    }

    boolean mouseClicked(double mouseX, double mouseY, int button) {
        return this.screen.invokeSuperMouseClicked(mouseX, mouseY, button);
    }

    boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return this.screen.invokeSuperMouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return this.screen.invokeSuperMouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    boolean mouseReleased(double mouseX, double mouseY, int button) {
        return this.screen.invokeSuperMouseReleased(mouseX, mouseY, button);
    }

    boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return this.screen.invokeSuperKeyPressed(keyCode, scanCode, modifiers);
    }

    boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return this.screen.invokeSuperKeyReleased(keyCode, scanCode, modifiers);
    }

    boolean charTyped(char codePoint, int modifiers) {
        return this.screen.invokeSuperCharTyped(codePoint, modifiers);
    }

    void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        this.screen.invokeSuperRenderTooltip(guiGraphics, mouseX, mouseY);
    }

    List<Component> containerTooltip(ItemStack stack) {
        return this.screen.containerTooltip(stack);
    }

    void focusScreen(@Nullable GuiEventListener listener) {
        this.screen.focusScreen(listener);
    }
}
