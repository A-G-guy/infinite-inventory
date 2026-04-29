#!/usr/bin/env python3
"""
生成 Infinite Inventory 模组 Hero Banner (1280x640)
平面设计风格，深色渐变背景 + 像素风物品展示
"""

from PIL import Image, ImageDraw, ImageFont, ImageFilter
import math

# 画布尺寸
W, H = 1280, 640

# 配色方案
BG_TOP = (26, 26, 46)       # #1a1a2e
BG_MID = (22, 33, 62)       # #16213e
BG_BOT = (15, 52, 96)       # #0f3460
ACCENT_CYAN = (78, 205, 196)     # #4ecdc4
ACCENT_GOLD = (255, 195, 0)      # #ffc300
TEXT_WHITE = (255, 255, 255)
TEXT_GRAY = (200, 200, 210)
TEXT_MUTED = (140, 140, 160)
PILL_BLUE = (41, 128, 185)
PILL_GREEN = (39, 174, 96)
PILL_PURPLE = (142, 68, 173)

# 加载字体
def load_font(size, bold=False):
    """加载支持中英文的字体"""
    try:
        # 尝试加载文泉驿微米黑
        return ImageFont.truetype("/usr/share/fonts/truetype/wqy/wqy-microhei.ttc", size)
    except:
        try:
            return ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf" if bold else "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", size)
        except:
            return ImageFont.load_default()

def load_font_en(size, bold=False):
    """加载英文字体（Lato更现代）"""
    try:
        if bold:
            return ImageFont.truetype("/usr/share/fonts/truetype/lato/Lato-Black.ttf", size)
        else:
            return ImageFont.truetype("/usr/share/fonts/truetype/lato/Lato-Semibold.ttf", size)
    except:
        try:
            return ImageFont.truetype("/usr/share/fonts/truetype/liberation/LiberationSans-Bold.ttf" if bold else "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf", size)
        except:
            return load_font(size, bold)

def create_gradient_background(width, height):
    """创建从上到下的深色渐变背景"""
    img = Image.new('RGB', (width, height))
    draw = ImageDraw.Draw(img)

    for y in range(height):
        ratio = y / height
        if ratio < 0.5:
            r = int(BG_TOP[0] + (BG_MID[0] - BG_TOP[0]) * (ratio * 2))
            g = int(BG_TOP[1] + (BG_MID[1] - BG_TOP[1]) * (ratio * 2))
            b = int(BG_TOP[2] + (BG_MID[2] - BG_TOP[2]) * (ratio * 2))
        else:
            r = int(BG_MID[0] + (BG_BOT[0] - BG_MID[0]) * ((ratio - 0.5) * 2))
            g = int(BG_MID[1] + (BG_BOT[1] - BG_MID[1]) * ((ratio - 0.5) * 2))
            b = int(BG_MID[2] + (BG_BOT[2] - BG_MID[2]) * ((ratio - 0.5) * 2))
        draw.line([(0, y), (width, y)], fill=(r, g, b))

    return img

def add_grid_pattern(img, spacing=40, opacity=15):
    """添加微妙的网格纹理"""
    overlay = Image.new('RGBA', img.size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(overlay)
    w, h = img.size

    for x in range(0, w, spacing):
        draw.line([(x, 0), (x, h)], fill=(255, 255, 255, opacity))
    for y in range(0, h, spacing):
        draw.line([(0, y), (w, y)], fill=(255, 255, 255, opacity))

    return Image.alpha_composite(img.convert('RGBA'), overlay)

def add_glow_circle(img, center, radius, color, intensity=80):
    """在指定位置添加发光圆形效果"""
    glow = Image.new('RGBA', img.size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(glow)

    for r in range(radius, 0, -2):
        alpha = int(intensity * (1 - r / radius) * 0.5)
        if alpha > 0:
            draw.ellipse(
                [center[0] - r, center[1] - r, center[0] + r, center[1] + r],
                fill=(color[0], color[1], color[2], alpha)
            )

    return Image.alpha_composite(img, glow)

def draw_rounded_pill(draw, xy, text, font, bg_color, text_color=TEXT_WHITE, radius=20):
    """绘制圆角pill标签"""
    x1, y1, x2, y2 = xy
    # 绘制圆角矩形背景
    draw.rounded_rectangle([x1, y1, x2, y2], radius=radius, fill=bg_color)
    # 绘制文字（居中）
    bbox = draw.textbbox((0, 0), text, font=font)
    text_w = bbox[2] - bbox[0]
    text_h = bbox[3] - bbox[1]
    text_x = x1 + (x2 - x1 - text_w) // 2
    text_y = y1 + (y2 - y1 - text_h) // 2 - 2
    draw.text((text_x, text_y), text, font=font, fill=text_color)

def draw_text_with_shadow(draw, pos, text, font, color, shadow_color=(0, 0, 0, 100), offset=(2, 2)):
    """绘制带阴影的文本"""
    x, y = pos
    ox, oy = offset
    # 阴影
    draw.text((x + ox, y + oy), text, font=font, fill=shadow_color)
    # 主文字
    draw.text((x, y), text, font=font, fill=color)

def create_particle_overlay(size, count=30):
    """创建粒子装饰层"""
    particles = Image.new('RGBA', size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(particles)
    import random
    random.seed(42)  # 固定种子保证可复现

    for _ in range(count):
        x = random.randint(0, size[0])
        y = random.randint(0, size[1])
        r = random.randint(1, 3)
        alpha = random.randint(20, 60)
        color = random.choice([
            (ACCENT_CYAN[0], ACCENT_CYAN[1], ACCENT_CYAN[2], alpha),
            (ACCENT_GOLD[0], ACCENT_GOLD[1], ACCENT_GOLD[2], alpha),
            (255, 255, 255, alpha // 2),
        ])
        draw.ellipse([x - r, y - r, x + r, y + r], fill=color)

    return particles

def main():
    # 创建渐变背景
    img = create_gradient_background(W, H)
    img = img.convert('RGBA')

    # 添加网格纹理
    img = add_grid_pattern(img, spacing=50, opacity=10)

    # 添加右侧大型发光圆
    img = add_glow_circle(img, (W * 0.72, H * 0.45), 280, ACCENT_CYAN, intensity=60)
    img = add_glow_circle(img, (W * 0.72, H * 0.45), 200, ACCENT_GOLD, intensity=40)

    draw = ImageDraw.Draw(img)

    # 加载字体
    font_title = load_font_en(72, bold=True)
    font_subtitle = load_font(36)
    font_tagline = load_font_en(22)
    font_pill = load_font_en(16, bold=True)
    font_bottom = load_font_en(14)
    font_version = load_font_en(13)

    # ===== 左侧文字内容 =====
    left_x = 80
    current_y = 140

    # 主标题: Infinite Inventory
    title_text = "Infinite Inventory"
    draw_text_with_shadow(draw, (left_x, current_y), title_text, font_title, TEXT_WHITE)
    title_bbox = draw.textbbox((left_x, current_y), title_text, font=font_title)
    current_y += (title_bbox[3] - title_bbox[1]) + 15

    # 副标题: / 无限库存
    subtitle_text = "/ 无限库存"
    draw_text_with_shadow(draw, (left_x, current_y), subtitle_text, font_subtitle, ACCENT_CYAN)
    subtitle_bbox = draw.textbbox((left_x, current_y), subtitle_text, font=font_subtitle)
    current_y += (subtitle_bbox[3] - subtitle_bbox[1]) + 30

    # 标语
    tagline_text = "Never run out of inventory space again."
    draw_text_with_shadow(draw, (left_x, current_y), tagline_text, font_tagline, TEXT_GRAY)
    tagline_bbox = draw.textbbox((left_x, current_y), tagline_text, font=font_tagline)
    current_y += (tagline_bbox[3] - tagline_bbox[1]) + 50

    # 分隔线
    draw.line([(left_x, current_y), (left_x + 400, current_y)], fill=(ACCENT_CYAN[0], ACCENT_CYAN[1], ACCENT_CYAN[2], 120), width=2)
    current_y += 30

    # 特性标签区域
    pills = [
        ("Infinite Storage", PILL_BLUE),
        ("Bilingual Search", PILL_GREEN),
        ("Tab Management", PILL_PURPLE),
    ]

    pill_x = left_x
    pill_y = current_y
    pill_h = 38
    pill_gap = 12

    for text, color in pills:
        bbox = draw.textbbox((0, 0), text, font=font_pill)
        pill_w = (bbox[2] - bbox[0]) + 28
        draw_rounded_pill(draw, (pill_x, pill_y, pill_x + pill_w, pill_y + pill_h),
                          text, font_pill, color)
        pill_x += pill_w + pill_gap

    current_y = pill_y + pill_h + 40

    # 简短描述
    desc_text = "A database-style storage system for NeoForge 1.21.1"
    draw.text((left_x, current_y), desc_text, font=font_version, fill=TEXT_MUTED)
    current_y += 30
    desc_text2 = "Smart search · Pinyin lookup · Auto-backup · Public & Private DB"
    draw.text((left_x, current_y), desc_text2, font=font_version, fill=TEXT_MUTED)

    # ===== 右侧物品纹理 =====
    try:
        item_img = Image.open("database_access_item.png").convert("RGBA")
        # 放大到 192x192（保持像素风格）
        item_large = item_img.resize((192, 192), Image.NEAREST)

        # 计算位置：右侧居中偏上
        item_x = int(W * 0.72) - 96
        item_y = int(H * 0.45) - 96

        # 添加物品背后的光晕层
        glow_layer = Image.new('RGBA', (192, 192), (0, 0, 0, 0))
        glow_draw = ImageDraw.Draw(glow_layer)
        # 绘制发光矩形背景
        for i in range(30, 0, -3):
            alpha = int(15 * (1 - i / 30))
            glow_draw.rectangle([i, i, 192 - i, 192 - i],
                                fill=(ACCENT_CYAN[0], ACCENT_CYAN[1], ACCENT_CYAN[2], alpha))

        # 将发光层粘贴到主图
        img.paste(glow_layer, (item_x, item_y), glow_layer)

        # 粘贴物品纹理
        img.paste(item_large, (item_x, item_y), item_large)

    except Exception as e:
        print(f"Warning: Could not load item texture: {e}")

    # ===== 底部信息栏 =====
    bottom_y = H - 55
    bottom_items = [
        ("Minecraft 1.21.1", ACCENT_CYAN),
        ("NeoForge", ACCENT_GOLD),
        ("Apache-2.0", TEXT_GRAY),
    ]

    # 计算总宽度以居中
    total_width = 0
    item_widths = []
    for text, _ in bottom_items:
        bbox = draw.textbbox((0, 0), text, font=font_bottom)
        w = bbox[2] - bbox[0] + 20  # 10px padding each side
        item_widths.append(w)
        total_width += w + 15  # gap
    total_width -= 15

    start_x = (W - total_width) // 2
    bx = start_x

    for (text, color), iw in zip(bottom_items, item_widths):
        # 绘制圆角背景
        draw.rounded_rectangle([bx, bottom_y, bx + iw, bottom_y + 32], radius=16,
                               fill=(30, 30, 50, 180))
        # 绘制文字
        bbox = draw.textbbox((0, 0), text, font=font_bottom)
        tw = bbox[2] - bbox[0]
        th = bbox[3] - bbox[1]
        tx = bx + (iw - tw) // 2
        ty = bottom_y + (32 - th) // 2 - 2
        draw.text((tx, ty), text, font=font_bottom, fill=color)
        bx += iw + 15

    # ===== 粒子装饰 =====
    particles = create_particle_overlay((W, H), count=40)
    img = Image.alpha_composite(img, particles)

    # ===== 角落装饰线条 =====
    decor = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    decor_draw = ImageDraw.Draw(decor)
    # 左上角
    decor_draw.line([(30, 60), (30, 30), (60, 30)], fill=(ACCENT_CYAN[0], ACCENT_CYAN[1], ACCENT_CYAN[2], 100), width=2)
    # 右下角
    decor_draw.line([(W - 30, H - 60), (W - 30, H - 30), (W - 60, H - 30)], fill=(ACCENT_CYAN[0], ACCENT_CYAN[1], ACCENT_CYAN[2], 100), width=2)
    # 左下角
    decor_draw.line([(30, H - 60), (30, H - 30), (60, H - 30)], fill=(ACCENT_GOLD[0], ACCENT_GOLD[1], ACCENT_GOLD[2], 80), width=2)
    # 右上角
    decor_draw.line([(W - 30, 60), (W - 30, 30), (W - 60, 30)], fill=(ACCENT_GOLD[0], ACCENT_GOLD[1], ACCENT_GOLD[2], 80), width=2)

    img = Image.alpha_composite(img, decor)

    # 保存结果
    output_path = "infiniteinventory-hero-v2.png"
    img_final = img.convert('RGB')
    img_final.save(output_path, 'PNG', quality=95)
    print(f"Hero banner saved to: {output_path}")
    print(f"Dimensions: {img_final.size}")

if __name__ == "__main__":
    main()
