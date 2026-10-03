package com.quietdome.client;

import com.quietdome.DomeTextures;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * 全屏背景的中间层。
 *
 * 现在是"视频优先，不行就退静图"。那几个 set*Provider 是留给以后接别的帧源用的，
 * 注册了就走注册的，调用点不用动。
 */
public final class DomeVideoAdapter {
    private DomeVideoAdapter() {}

    /** 画第 t 秒那一帧。 */
    @FunctionalInterface
    public interface FrameProvider {
        void draw(GuiGraphicsExtractor g, int areaW, int areaH, float t);
    }

    // ---- 注册的帧源，null 就退回静图 ----
    private static FrameProvider blackhole;
    private static FrameProvider menuBg;
    private static FrameProvider birdcage;

    public static void setBlackholeProvider(FrameProvider p) { blackhole = p; }
    public static void setMenuBgProvider(FrameProvider p) { menuBg = p; }
    public static void setBirdcageProvider(FrameProvider p) { birdcage = p; }

    /** 有没有注册过真正的帧源。 */
    public static boolean hasVideo() {
        return blackhole != null || menuBg != null || birdcage != null;
    }

    private static float now() {
        return (System.currentTimeMillis() % 3_600_000L) / 1000f;
    }

    public static void drawBlackhole(GuiGraphicsExtractor g, int areaW, int areaH, float zoom) {
        if (blackhole != null) {
            blackhole.draw(g, areaW, areaH, now());
            return;
        }
        // disk-streamed video (heidong.mp4) if present, else the still image
        VideoPlayer v = VideoManager.blackhole();
        if (v != null && v.draw(g, areaW, areaH, zoom)) {
            return;
        }
        DomeRender.cover(g, DomeTextures.BLACKHOLE, areaW, areaH, zoom, 0f, 0f);
    }

    public static void drawMenuBg(GuiGraphicsExtractor g, int areaW, int areaH, float zoom, float panX, float panY) {
        if (menuBg != null) {
            menuBg.draw(g, areaW, areaH, now());
        } else {
            DomeRender.cover(g, DomeTextures.SOLIVAULT, areaW, areaH, zoom, panX, panY);
        }
    }

    public static void drawBirdcage(GuiGraphicsExtractor g, int areaW, int areaH, float zoom, float panX) {
        if (birdcage != null) {
            birdcage.draw(g, areaW, areaH, now());
        } else {
            DomeRender.cover(g, DomeTextures.BIRDCAGE, areaW, areaH, zoom, panX, 0f);
        }
    }
}
