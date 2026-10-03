package com.quietdome.client.mixin;

import com.quietdome.DomeConfig;
import com.quietdome.client.DomeBackground;
import com.quietdome.client.DomeRender;
import com.quietdome.client.DomeVideoAdapter;
import com.quietdome.client.Starfield;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 加载屏：黑洞背景 + 原版 Mojang logo + 加粗的青色进度条。
 *
 * 注意别 cancel 原方法。原版 extractRenderState 里还管着淡入计时和
 * setOverlay(null) 的移除逻辑——上次 cancel 掉，加载屏就永远卡那不消失了。
 */
@Mixin(LoadingOverlay.class)
public class LoadingOverlayMixin {
    @Shadow @Final private Minecraft minecraft;
    @Shadow @Final private ReloadInstance reload;
    @Shadow @Final private boolean fadeIn;
    @Shadow private float currentProgress;
    @Shadow private long fadeOutStart;
    @Shadow private long fadeInStart;

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void quietdome$render(GuiGraphicsExtractor g, int mouseX, int mouseY, float a, CallbackInfo ci) {
        if (!DomeConfig.get().loadingScreen) return;

        long now = Util.getMillis();
        if (this.fadeIn && this.fadeInStart == -1L) {
            this.fadeInStart = now;
        }
        float fadeOutAnim = this.fadeOutStart > -1L ? (float) (now - this.fadeOutStart) / 1000.0F : -1.0F;

        // 渲染我们自己接管了，但移除 overlay 得自己来，不然它不会消失
        if (fadeOutAnim >= 2.0F) {
            this.minecraft.gui.setOverlay(null);
            ci.cancel();
            return;
        }

        int w = g.guiWidth();
        int h = g.guiHeight();
        float t = (System.currentTimeMillis() % 3_600_000L) / 1000f;

        // 1) 黑洞背景，随加载进度放大
        float zoom = 1.05f + Mth.clamp(this.currentProgress, 0f, 1f) * 0.45f
                + (float) Math.sin(t * 0.8f) * 0.01f;
        DomeRender.fillScreen(g, DomeRender.argb(255, 2, 3, 8));
        DomeVideoAdapter.drawBlackhole(g, w, h, zoom);

        // 2) 更新并画一根加粗的青色进度条
        float actual = this.reload.getActualProgress();
        this.currentProgress = Mth.clamp(this.currentProgress * 0.95F + actual * 0.05F, 0f, 1f);

        int barW = Math.min((int) (w * 0.55f), 560);
        int barH = 10;
        int bx = (w - barW) / 2;
        int by = (int) (h * 0.80f);
        int fadeA = fadeOutAnim >= 1.0F
                ? (int) (Mth.clamp(1.0F - (fadeOutAnim - 1.0F), 0f, 1f) * 255)
                : 255;
        // 底槽
        g.fill(bx - 2, by - 2, bx + barW + 2, by + barH + 2, withA(0x66, 0x31, 0xA8, 0xCA, fadeA));
        g.fill(bx, by, bx + barW, by + barH, withA(0x33, 0x0A, 0x18, 0x24, fadeA));
        // 填充
        int fw = (int) (barW * this.currentProgress);
        g.fill(bx, by, bx + fw, by + barH, withA(0xFF, 0x31, 0xA8, 0xCA, fadeA));
        // 进度头上一道白光
        if (fw > 0) {
            g.fill(bx + fw - 2, by - 3, bx + fw + 2, by + barH + 3, withA(0xCC, 0xCF, 0xE8, 0xFF, fadeA));
        }
        Font font = this.minecraft.font;
        String pct = (int) (this.currentProgress * 100) + "%";
        g.text(font, pct, (w - font.width(pct)) / 2, by + barH + 8, withA(0xAA, 0x7E, 0x94, 0xA8, fadeA));

        Starfield.draw(g, 0.5f);

        // 3) 画原版 Mojang logo（不是别名文字）
        float logoAlpha = fadeOutAnim >= 1.0F
                ? 1.0F - Mth.clamp(fadeOutAnim - 1.0F, 0f, 1f)
                : 1.0F;
        drawMojangLogo(g, logoAlpha);

        ci.cancel();
    }

    /** 照抄原版的 Mojang logo 画法（见 LoadingOverlay.extractRenderState）。 */
    private void drawMojangLogo(GuiGraphicsExtractor g, float logoAlpha) {
        Identifier tex = LoadingOverlay.MOJANG_STUDIOS_LOGO_LOCATION;
        int contentX = (int) (g.guiWidth() * 0.5);
        int logoY = (int) (g.guiHeight() * 0.5);
        double logoHeight = Math.min(g.guiWidth() * 0.75, g.guiHeight()) * 0.25;
        int logoHeightHalf = (int) (logoHeight * 0.5);
        double contentWidth = logoHeight * 4.0;
        int logoWidthHalf = (int) (contentWidth * 0.5);
        int color = withA(0xFF, 0xFF, 0xFF, 0xFF, (int) (Mth.clamp(logoAlpha, 0f, 1f) * 255));
        g.blit(RenderPipelines.MOJANG_LOGO, tex,
                contentX - logoWidthHalf, logoY - logoHeightHalf, -0.0625F, 0.0F,
                logoWidthHalf, (int) logoHeight, 120, 60, 120, 120, color);
        g.blit(RenderPipelines.MOJANG_LOGO, tex,
                contentX, logoY - logoHeightHalf, 0.0625F, 60.0F,
                logoWidthHalf, (int) logoHeight, 120, 60, 120, 120, color);
    }

    private static int withA(int baseA, int r, int gr, int b, int fadeA) {
        int a = baseA * fadeA / 255;
        return (a << 24) | (r << 16) | (gr << 8) | b;
    }
}
