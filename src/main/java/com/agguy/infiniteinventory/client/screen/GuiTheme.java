package com.agguy.infiniteinventory.client.screen;

/**
 * UI 主题颜色常量集中定义类。
 *
 * <p>设计意图：将分散在各 Helper 类中的颜色常量统一收拢，按功能分层管理，
 * 便于样式统一维护、bug 定位和后续主题扩展。
 *
 * <p>分类体系：
 * <ul>
 *   <li>覆盖层（OVERLAY_*）：弹窗、下拉、菜单等暗色背景面板</li>
 *   <li>框架层（FRAME_*）：主数据库界面深色背景上的文字与装饰</li>
 *   <li>控件层（TEXT_FIELD_* / SCROLLBAR_* / NAV_* / CHIP_*）：可复用组件</li>
 *   <li>数据层（LOG_* / CHART_* / STATISTICS_*）：图表与语义色彩</li>
 * </ul>
 */
final class GuiTheme {

    // ========== 覆盖层面板 ==========
    static final int OVERLAY_SHADOW = 0x70000000;
    static final int OVERLAY_OUTLINE = 0xFF333333;
    static final int OVERLAY_BACKGROUND = 0xFF1A1A1A;
    static final int OVERLAY_TOP_EDGE = 0x20FFFFFF;
    static final int OVERLAY_BOTTOM_EDGE = 0x15000000;

    // ========== 覆盖层行 ==========
    static final int OVERLAY_ROW = 0xFF222222;
    static final int OVERLAY_ROW_HOVERED = 0xFF2A2A2A;
    static final int OVERLAY_ROW_SELECTED = 0xFF3D3D20;
    static final int OVERLAY_ROW_DIVIDER = 0x30FFFFFF;

    // ========== 覆盖层芯片（开关按钮） ==========
    static final int OVERLAY_CHIP_ACTIVE = 0xFF2A2A2A;
    static final int OVERLAY_CHIP_HOVERED = 0xFF3A3A3A;
    static final int OVERLAY_CHIP_SELECTED = 0xFF4A4A20;
    static final int OVERLAY_CHIP_DISABLED = 0xFF1A1A1A;

    // ========== 覆盖层文字 ==========
    static final int OVERLAY_TEXT = 0xFFFFFFFF;
    static final int OVERLAY_MUTED_TEXT = 0xFFAAAAAA;
    static final int OVERLAY_ACCENT_TEXT = 0xFFFFD700;

    // ========== 框架层文字 ==========
    static final int FRAME_TEXT = 0xFFF5F1E6;
    static final int FRAME_MUTED_TEXT = 0xFFE6DCC2;
    static final int FRAME_ACCENT_TEXT = 0xFFFFE0A6;
    static final int FRAME_TEXT_BACKDROP = 0x6A16120D;
    static final int FRAME_TEXT_OUTLINE = 0x90765B3B;

    // ========== 文本框 ==========
    static final int TEXT_FIELD_OUTLINE_FOCUSED = 0xFFFFFFFF;
    static final int TEXT_FIELD_INNER_FOCUSED = 0xFF555555;
    static final int TEXT_FIELD_FILL_FOCUSED = 0xFF000000;
    static final int TEXT_FIELD_TOP_FOCUSED = 0x40FFFFFF;
    static final int TEXT_FIELD_OUTLINE_UNFOCUSED = 0xFF444444;
    static final int TEXT_FIELD_INNER_UNFOCUSED = 0xFF333333;
    static final int TEXT_FIELD_FILL_UNFOCUSED = 0xFF0A0A0A;
    static final int TEXT_FIELD_TOP_UNFOCUSED = 0x20FFFFFF;

    // ========== 滚动条 ==========
    static final int SCROLLBAR_TRACK = 0x30FFFFFF;
    static final int SCROLLBAR_THUMB = 0xCC888888;
    static final int SCROLLBAR_THUMB_HOVERED = 0xE0AAAAAA;

    // ========== 顶部标签 ==========
    static final int TOP_TAB_ACTIVE_TEXT = 0xFFF4D58A;
    static final int TOP_TAB_INACTIVE_TEXT = 0xFFF9F4EA;

    // ========== 导航栏（设置/统计面板共用） ==========
    static final int NAV_BACKGROUND = 0xFF222222;
    static final int NAV_ACTIVE_BACKGROUND = 0xFF2A2A2A;
    static final int NAV_HOVER_BACKGROUND = 0xFF333333;
    static final int NAV_ACTIVE_INDICATOR = 0xFFF4D58A;

    // ========== 分隔线 ==========
    static final int DIVIDER = 0x30FFFFFF;
    static final int TITLE_DIVIDER = 0x30FFFFFF;

    // ========== 内容区背景 ==========
    static final int CONTENT_BACKGROUND = 0xFF1A1A1A;
    static final int TITLE_BAR_BACKGROUND = 0xFF222222;

    // ========== 日志操作语义色 ==========
    static final int LOG_DEPOSIT = 0xFF4CAF50;
    static final int LOG_EXTRACT = 0xFFFF8C00;
    static final int LOG_TRANSFER = 0xFF64B5F6;
    static final int LOG_DELETE = 0xFFE57373;

    // ========== 统计图表颜色（低饱和彩虹色，适配暗色背景） ==========
    static final int[] CHART_COLORS = {
            0xFFEF5350, 0xFF42A5F5, 0xFF66BB6A, 0xFFFFA726,
            0xFFAB47BC, 0xFF26A69A, 0xFFFF7043, 0xFF29B6F6
    };

    // ========== 统计语义色 ==========
    static final int STATISTICS_DEPOSIT = 0xFF4CAF50;
    static final int STATISTICS_TOTAL = 0xFF64B5F6;
    static final int STATISTICS_POSITIVE = 0xFF4CAF50;
    static final int STATISTICS_NEGATIVE = 0xFFE57373;

    // ========== 其他通用颜色 ==========
    static final int EMPTY_STATE_TEXT = 0xFF888888;
    static final int DROPDOWN_TEXT = 0xFFFFFFFF;
    static final int SORT_TEXT = 0xFFFFFFFF;
    static final int TOOLTIP_SCOPE = 0xFFAAAAAA;
    static final int MORE_TABS_HOVER_TEXT = 0xFFFFFFFF;
    static final int MORE_TABS_TEXT = 0xFFAAAAAA;
    static final int NOTE_MIXED_HINT = 0xFFFFD700;

    // ========== 槽位选中/高亮（保持金黄风格） ==========
    static final int SLOT_SELECTION_FILL = 0x40E3D0A4;
    static final int SLOT_SELECTION_BORDER_LIGHT = 0xFFD6B86E;
    static final int SLOT_SELECTION_BORDER_DARK = 0xFF8D6F28;
    static final int SLOT_HOVER_HIGHLIGHT = 0x48000000;

    // ========== 星标指示器（保持金黄） ==========
    static final int STAR_BORDER = 0xFF8B6914;
    static final int STAR_FILL = 0xFFFFD700;
    static final int STAR_HIGHLIGHT = 0xFFFFEC8B;

    // ========== 饰品面板 ==========
    static final int ACCESSORY_PANEL_HOVER = 0x52000000;
    static final int ACCESSORY_ROW_EVEN = 0x2A222222;
    static final int ACCESSORY_ROW_ODD = 0x201A1A1A;
    static final int ACCESSORY_BORDER_TOP = 0x70444444;
    static final int ACCESSORY_BORDER_BOTTOM = 0x60333333;
    static final int ACCESSORY_BORDER_LEFT = 0x70444444;
    static final int ACCESSORY_BORDER_RIGHT = 0x50333333;
    static final int ACCESSORY_GRID_VERTICAL = 0x12FFFFFF;
    static final int ACCESSORY_GRID_HORIZONTAL = 0x18FFFFFF;

    // ========== 饰品槽外部装饰边框 ==========
    static final int ACCESSORY_SLOT_OUTER_LIGHT = 0xB0606060;
    static final int ACCESSORY_SLOT_OUTER_DARK = 0x90404040;

    // ========== 数据库槽位悬停遮罩 ==========
    static final int DATABASE_SLOT_HOVER_MASK = 0x12FFFFFF;

    // ========== 统计面板行悬停 ==========
    static final int STATISTICS_ROW_HOVER = 0x20FFFFFF;

    // ========== 条形图轨道 ==========
    static final int BAR_CHART_TRACK = 0x30FFFFFF;

    // ========== 趋势网格线 ==========
    static final int TREND_GRID = 0x20FFFFFF;

    // ========== 统计日志行悬停 ==========
    static final int STATISTICS_LOG_ROW_HOVER = 0x20FFFFFF;

    private GuiTheme() {
    }
}
