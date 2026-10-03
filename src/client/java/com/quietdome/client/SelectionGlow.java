package com.quietdome.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;

/**
 * 悬停/聚焦的反馈：青蓝边框 + 黑洞粒子侵蚀（Phigros 第九章那味）。
 *
 *  - 蓝框只在悬停/聚焦时出现，平时不画。
 *  - 悬停期间粒子按侵蚀度往按钮上堆，越多越糊。
 *  - 光标移开就暂停（见 {@link Corruption}）。
 *  - cap 限制一个界面的控件最多侵蚀到哪；主菜单不许满，不然玩家点不动。
 */
public final class SelectionGlow {
    private SelectionGlow() {}

    /**
     * 只给这三个菜单界面加特效。功能界面（创建世界、资源包、语言、统计、
     * 设置……）一律不碰——那些里面有列表、输入框、滑条，加了特效就废了。
     */
    public static boolean isMenuScreen(Screen screen) {
        return screen instanceof TitleScreen
                || screen instanceof SelectWorldScreen
                || screen instanceof JoinMultiplayerScreen;
    }

    public static void draw(Screen screen, GuiGraphicsExtractor g, float cap) {
        float pulse = 0.55f + 0.45f * (float) Math.sin((System.currentTimeMillis() % 4000L) / 1000f * 3.0f);

        for (GuiEventListener child : screen.children()) {
            if (!(child instanceof AbstractWidget w) || !w.visible) continue;

            boolean touched = w.isHoveredOrFocused();
            float c = touched
                    ? Corruption.touch(screen, w, cap)
                    : Corruption.peek(screen, w, cap);

            if (!touched && c <= 0f) continue;

            int x = w.getX();
            int y = w.getY();
            int ww = w.getWidth();
            int hh = w.getHeight();

            // 蓝框只画在悬停/聚焦时
            if (touched) {
                int a = (int) (0x66 * pulse) + 0x44;
                int glow = DomeRender.argb(a, 0x31, 0xA8, 0xCA);
                frame(g, x, y, ww, hh, glow);
                spark(g, x - 2, y - 2, pulse);
                spark(g, x + ww + 1, y - 2, pulse);
                spark(g, x - 2, y + hh + 1, pulse);
                spark(g, x + ww + 1, y + hh + 1, pulse);
            }

            // 粒子侵蚀
            if (c > 0.01f) {
                drawParticles(g, x, y, ww, hh, c);
            }

            // 侵蚀满了就点不动了（cap 得是 1 才可能到这一步）
            if (c >= 1f) {
                w.active = false;
            }
        }
    }

    private static void frame(GuiGraphicsExtractor g, int x, int y, int ww, int hh, int color) {
        g.fill(x - 2, y - 2, x + ww + 2, y - 1, color);
        g.fill(x - 2, y + hh + 1, x + ww + 2, y + hh + 2, color);
        g.fill(x - 2, y - 1, x - 1, y + hh + 1, color);
        g.fill(x + ww + 1, y - 1, x + ww + 2, y + hh + 1, color);
    }

    /**
     * 黑洞粒子场：粒子数随侵蚀度涨，涨满就糊住按钮。
     * 位置用 (index, epoch) 算伪随机，所以看着稳但当时间片一变就会闪，
     * 不至于像静止贴图。
     */
    private static void drawParticles(GuiGraphicsExtractor g, int x, int y, int ww, int hh, float c) {
        // 整块蒙一层，随侵蚀加深
        int veil = (int) (c * 0.55f * 255);
        if (veil > 0) {
            g.fill(x, y, x + ww, y + hh, DomeRender.argb(veil, 0x0A, 0x18, 0x24));
        }

        int maxParticles = (ww * hh) / 45;          // 密度上限
        int count = (int) (maxParticles * c * 1.15f);
        long epoch = System.currentTimeMillis() / 220;   // 闪烁步长

        for (int i = 0; i < count; i++) {
            float rx = DomeRender.hash01(i, (int) epoch, 5);
            float ry = DomeRender.hash01(i, (int) epoch, 13);
            int px = x + (int) (rx * ww);
            int py = y + (int) (ry * hh);

            // 大部分是深色，偶尔蹦个亮点的
            float sparkle = DomeRender.hash01(i, (int) epoch, 31);
            int col;
            int size;
            if (sparkle > 0.93f) {
                col = DomeRender.argb(220, 0x6F, 0xE8, 0xFF);
                size = 2;
            } else {
                int shade = (int) (sparkle * 0x50);
                col = DomeRender.argb(230, 0x08, 0x30 + shade, 0x48 + shade);
                size = 1;
            }
            g.fill(px, py, px + size, py + size, col);
        }
    }

    private static void spark(GuiGraphicsExtractor g, int x, int y, float pulse) {
        int a = (int) (0xAA * pulse) + 0x33;
        g.fill(x, y, x + 1, y + 1, DomeRender.argb(a, 0xCF, 0xE8, 0xFF));
    }
}
