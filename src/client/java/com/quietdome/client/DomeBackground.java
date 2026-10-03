package com.quietdome.client;

import com.quietdome.DomeConfig;
import com.quietdome.DomeTextures;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

/**
 * 菜单背景。主菜单/存档/多人用循环的黑洞视频，子界面用 birdcage 图，
 * 都压一层暗角和星空。
 */
public final class DomeBackground {
    private DomeBackground() {}

    // phi 那套红色调，现在只留着给加载屏的薄雾用
    private static final int RED_TOP    = DomeRender.argb(0x33, 134, 45, 52);
    private static final int RED_MID    = DomeRender.argb(0x55, 118, 39, 39);
    private static final int RED_BOTTOM = DomeRender.argb(0x88, 82, 23, 23);

    /** 当前秒数，用来算连续漂移。 */
    private static float t() {
        return (System.currentTimeMillis() % 3_600_000L) / 1000f;
    }

    /** 菜单全屏背景（主菜单 / 存档 / 多人）。 */
    public static void draw(GuiGraphicsExtractor g) {
        DomeConfig cfg = DomeConfig.get();
        int w = g.guiWidth();
        int h = g.guiHeight();

        // 1) 底：循环的黑洞视频，和加载屏共用一个播放器实例，进来才无缝
        VideoPlayer hole = VideoManager.blackhole();
        boolean drewVideo = hole != null && hole.draw(g, w, h, 1.0f);
        if (!drewVideo) {
            // 视频没了就退静图
            DomeVideoAdapter.drawMenuBg(g, w, h, cfg.backgroundZoom, 0f, 0f);
        }

        // 2) 暗角
        vignette(g, w, h);

        // 4) starfield
        if (cfg.starfield) {
            Starfield.draw(g, 0.35f);
        }
    }

    /** 子界面背景：birdcage 图，压暗一点。 */
    public static void drawBirdcage(GuiGraphicsExtractor g) {
        int w = g.guiWidth();
        int h = g.guiHeight();
        DomeVideoAdapter.drawBirdcage(g, w, h, 1.04f, 0f);
        // darken so foreground widgets stay readable
        g.fill(0, 0, w, h, DomeRender.argb(0x88, 0, 0, 0));
        vignette(g, w, h);
        if (DomeConfig.get().starfield) {
            Starfield.draw(g, 0.28f);
        }
    }

    /** 加载屏背景：黑洞。 */
    public static void drawBlackHole(GuiGraphicsExtractor g, float zoom, float alpha) {
        int w = g.guiWidth();
        int h = g.guiHeight();
        DomeRender.fillScreen(g, DomeRender.argb(255, 2, 3, 8));
        DomeVideoAdapter.drawBlackhole(g, w, h, zoom);
        // subtle red haze like solivault
        gradientBand(g, h, 0f, 1f, DomeRender.argb(0x22, 132, 36, 40), DomeRender.argb(0x33, 82, 23, 23));
        vignette(g, w, h);
        if (alpha < 1f) {
            g.fill(0, 0, w, h, DomeRender.argb((int) ((1f - alpha) * 255), 0, 0, 0));
        }
    }

    private static void gradientBand(GuiGraphicsExtractor g, int h, float f0, float f1, int c0, int c1) {
        int y0 = (int) (f0 * h);
        int y1 = (int) (f1 * h);
        int steps = 24;
        for (int i = 0; i < steps; i++) {
            float p = i / (float) steps;
            int yA = y0 + (y1 - y0) * i / steps;
            int yB = y0 + (y1 - y0) * (i + 1) / steps;
            g.fill(0, yA, g.guiWidth(), yB, DomeRender.lerpColor(c0, c1, p));
        }
    }

    private static void vignette(GuiGraphicsExtractor g, int w, int h) {
        int steps = 18;
        int maxA = (int) (0.62f * 255);
        for (int i = 0; i < steps; i++) {
            float p = i / (float) steps;
            int a = (int) (maxA * p * p);
            int c = DomeRender.argb(a, 0, 0, 0);
            int bw = (int) (w * 0.10f * (1 - p));
            int bh = (int) (h * 0.10f * (1 - p));
            g.fill(0, 0, w, bh / 2, c);
            g.fill(0, h - bh / 2, w, h, c);
            g.fill(0, 0, bw / 2, h, c);
            g.fill(w - bw / 2, 0, w, h, c);
        }
    }
}
