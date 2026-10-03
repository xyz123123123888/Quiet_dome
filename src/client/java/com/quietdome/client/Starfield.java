package com.quietdome.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** 背景上那层会慢慢往下飘的星星。 */
public final class Starfield {
    private Starfield() {}

    private static final int COUNT = 120;

    /** intensity 0..1，整体亮度。 */
    public static void draw(GuiGraphicsExtractor g, float intensity) {
        int w = g.guiWidth();
        int h = g.guiHeight();
        float t = (System.currentTimeMillis() % 600_000L) / 1000f;

        for (int i = 0; i < COUNT; i++) {
            float rx = DomeRender.hash01(i, 11, 3);
            float ry = DomeRender.hash01(i, 29, 7);
            float speed = 0.5f + DomeRender.hash01(i, 41, 13) * 1.5f;
            float tw = DomeRender.hash01(i, 53, 17);

            int x = (int) (rx * w);
            float baseY = ry * h;
            // slow downward drift, wrapping
            int y = (int) ((baseY + t * speed * 6f) % h);

            float flicker = 0.55f + 0.45f * (float) Math.sin(t * (0.6f + tw) + i);
            int a = (int) (flicker * intensity * 210f);
            if (a <= 3) continue;
            int size = tw > 0.92f ? 2 : 1;
            int color = tw > 0.7f
                    ? DomeRender.argb(a, 0xCF, 0xE8, 0xFF)
                    : DomeRender.argb(a, 0xDB, 0xE8, 0xF5);
            g.fill(x, y, x + size, y + size, color);
        }
    }
}
