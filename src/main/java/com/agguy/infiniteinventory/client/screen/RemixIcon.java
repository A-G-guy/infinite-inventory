package com.agguy.infiniteinventory.client.screen;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Remix Icon 图标枚举，定义模组 GUI 中使用的所有自定义图标。
 *
 * <p>图标资源位于 {@code assets/infiniteinventory/textures/gui/sprites/icon/} 目录下，
 * 由 Minecraft 的 GuiSpriteManager 自动打包进 GUI 纹理图集，通过 {@link #location()} 获取的
 * {@link ResourceLocation} 调用 {@code guiGraphics.blitSprite()} 进行高性能渲染。
 *
 * <p>尺寸约定：
 * <ul>
 *   <li>右键菜单 / 标签页上下文菜单图标：16x16 像素
 *   <li>按钮图标：12x12 像素
 * </ul>
 */
enum RemixIcon {
    // 右键菜单图标 (16x16)
    TAKE_SINGLE("icon/take_single"),
    TAKE_HALF_STACK("icon/take_half_stack"),
    TAKE_STACK("icon/take_stack"),
    TAKE_HALF_ENTRY("icon/take_half_entry"),
    TAKE_ALL("icon/take_all"),
    TAKE_CUSTOM("icon/take_custom"),
    EDIT_NOTE("icon/edit_note"),
    TOGGLE_STAR("icon/toggle_star"),
    ADD_STAR("icon/add_star"),
    REMOVE_STAR("icon/remove_star"),
    STAR_MIXED("icon/star_mixed"),
    STAR_ALL("icon/star_all"),
    UNSTAR_ALL("icon/unstar_all"),
    TRANSFER("icon/transfer"),

    // 标签页上下文菜单图标 (16x16)
    JOIN_VIEW("icon/join_view"),
    REMOVE_VIEW("icon/remove_view"),
    SINGLE_VIEW("icon/single_view"),
    MOVE_LEFT("icon/move_left"),
    MOVE_RIGHT("icon/move_right"),
    RENAME("icon/rename"),
    CHANGE_ICON("icon/change_icon"),
    TOGGLE_VISIBILITY("icon/toggle_visibility"),

    // 按钮图标 (12x12)
    SETTINGS("icon/settings"),
    VIEWS("icon/views"),
    STATISTICS("icon/statistics"),
    DEPOSIT("icon/deposit"),
    DEPOSIT_EXISTING("icon/deposit_existing"),
    SCOPE_PERSONAL("icon/scope_personal"),
    SCOPE_PUBLIC("icon/scope_public"),
    ACCESSORIES("icon/accessories"),
    SORT("icon/sort"),
    PAGE_PREVIOUS("icon/page_previous"),
    PAGE_NEXT("icon/page_next"),
    PAGE_INDICATOR("icon/page_indicator"),
    SEARCH_TOGGLE("icon/search_toggle"),
    SEARCH_WEIGHT("icon/search_weight"),
    ENHANCEMENT("icon/enhancement");

    private final ResourceLocation location;

    RemixIcon(String path) {
        this.location = ResourceLocation.fromNamespaceAndPath("infiniteinventory", path);
    }

    ResourceLocation location() {
        return location;
    }

    /**
     * 根据右键菜单项的翻译键查找对应图标。
     *
     * @param translationKey 菜单项翻译键
     * @return 对应图标；若未映射则返回 null（渲染代码将跳过图标绘制）
     */
    @Nullable
    static RemixIcon forContextMenuKey(String translationKey) {
        return switch (translationKey) {
            case "screen.infiniteinventory.context.take_single",
                 "screen.infiniteinventory.selection.take_one_each" -> TAKE_SINGLE;
            case "screen.infiniteinventory.context.take_half_stack_to_inventory",
                 "screen.infiniteinventory.selection.take_half_stack_each" -> TAKE_HALF_STACK;
            case "screen.infiniteinventory.context.take_stack",
                 "screen.infiniteinventory.selection.take_stack_each" -> TAKE_STACK;
            case "screen.infiniteinventory.context.take_half_entry_to_inventory",
                 "screen.infiniteinventory.selection.take_half_entry_each" -> TAKE_HALF_ENTRY;
            case "screen.infiniteinventory.context.take_all_to_inventory",
                 "screen.infiniteinventory.selection.take_all_each" -> TAKE_ALL;
            case "screen.infiniteinventory.context.take_custom_to_inventory",
                 "screen.infiniteinventory.selection.take_custom_each" -> TAKE_CUSTOM;
            case "screen.infiniteinventory.context.edit_note" -> EDIT_NOTE;
            case "screen.infiniteinventory.context.toggle_star" -> TOGGLE_STAR;
            case "screen.infiniteinventory.context.add_star" -> ADD_STAR;
            case "screen.infiniteinventory.context.remove_star" -> REMOVE_STAR;
            case "screen.infiniteinventory.context.toggle_star_mixed" -> STAR_MIXED;
            case "screen.infiniteinventory.context.star_all" -> STAR_ALL;
            case "screen.infiniteinventory.context.unstar_all" -> UNSTAR_ALL;
            case "screen.infiniteinventory.selection.transfer" -> TRANSFER;
            default -> null;
        };
    }

    /**
     * 根据标签页上下文菜单动作查找对应图标。
     *
     * @param action 标签页上下文菜单动作
     * @return 对应图标；若未映射则返回 null
     */
    @Nullable
    static RemixIcon forTabContextMenuAction(
            PersonalDatabaseScreenTabContextMenuItem.TabContextMenuAction action
    ) {
        return switch (action) {
            case JOIN_CURRENT_VIEW -> JOIN_VIEW;
            case REMOVE_FROM_VIEW -> REMOVE_VIEW;
            case SINGLE_VIEW -> SINGLE_VIEW;
            case MOVE_LEFT -> MOVE_LEFT;
            case MOVE_RIGHT -> MOVE_RIGHT;
            case RENAME -> RENAME;
            case CHANGE_ICON -> CHANGE_ICON;
            case TOGGLE_TOP_VISIBILITY -> TOGGLE_VISIBILITY;
        };
    }
}
