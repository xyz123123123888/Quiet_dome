package com.quietdome.client;

import com.quietdome.DomeTextures;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** 画图工具集。 */
public final class DomeRender {
    private DomeRender() {}

    /** phi 背景的原设计分辨率。 */
    public static final float DESIGN_W = 1920f;
    public static final float DESIGN_H = 1080f;

    /** 拼一个 ARGB 整数。 */
    public static int argb(int a, int r, int g, int b) {
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int lerpColor(int a, int b, float t) {
        int aa = (a >>> 24), ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int ba = (b >>> 24), br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        return argb(
                (int) Mth.lerp(t, aa, ba),
                (int) Mth.lerp(t, ar, br),
                (int) Mth.lerp(t, ag, bg),
                (int) Mth.lerp(t, ab, bb));
    }

    /** 把贴图按 cover 方式铺满区域：保比例、裁边、居中，可额外放大和偏移。 */
    public static void cover(GuiGraphicsExtractor g, Identifier tex,
                             int areaW, int areaH, float zoom, float panX, float panY) {
        float base = Math.max(areaW / DESIGN_W, areaH / DESIGN_H);
        float s = base * zoom;
        float dw = DESIGN_W * s;
        float dh = DESIGN_H * s;
        float dx = (areaW - dw) / 2f + panX;
        float dy = (areaH - dh) / 2f + panY;
        g.blit(tex, (int) dx, (int) dy, (int) (dx + dw), (int) (dy + dh), 0f, 1f, 0f, 1f);
    }

    /** 用纯色铺满整个视口。 */
    public static void fillScreen(GuiGraphicsExtractor g, int color) {
        g.fill(0, 0, g.guiWidth(), g.guiHeight(), color);
    }

    /**
     * 铺一层黑，中间留个半径 radiusFrac*maxR 的圆洞，洞会变大。
     * 主菜单那个"从中间扩散出来"的转场就靠它。
     */
    public static void blackOutsideCircle(GuiGraphicsExtractor g, float radiusFrac, int blackArgb) {
        int w = g.guiWidth();
        int h = g.guiHeight();
        float cx = w / 2f;
        float cy = h / 2f;
        float maxR = (float) Math.hypot(w, h) / 2f;
        float r = Math.max(0f, radiusFrac) * maxR;
        for (int y = 0; y < h; y++) {
            float dy = y + 0.5f - cy;
            float half = r * r - dy * dy;
            if (half <= 0f) {
                g.fill(0, y, w, y + 1, blackArgb);
            } else {
                int hw = (int) Math.sqrt(half);
                int x0 = (int) (cx - hw);
                int x1 = (int) (cx + hw);
                if (x0 > 0) g.fill(0, y, x0, y + 1, blackArgb);
                if (x1 < w) g.fill(x1, y, w, y + 1, blackArgb);
            }
        }
    }

    /** 拿三个整数算个稳定的伪随机 [0,1)。 */
    public static float hash01(int a, int b, int c) {
        int h = a * 374761393 + b * 668265263 + c * 2147483647;
        h = (h ^ (h >>> 13)) * 1274126177;
        h = h ^ (h >>> 16);
        return (h & 0x7FFFFFFF) / (float) 0x7FFFFFFF;
    }
}
