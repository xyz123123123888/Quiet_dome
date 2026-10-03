package com.quietdome.client.mixin;

import com.quietdome.DomeConfig;
import com.quietdome.client.DomeBackground;
import com.quietdome.client.DomeRender;
import com.quietdome.client.SelectionGlow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 背景路由 + 入场黑幕。
 *
 * 故意挂在 Screen 的 final 方法上（added、extractRenderStateWithTooltipAndSubtitles），
 * 因为不少子类会重写 init/extractRenderState 且不调 super，挂普通方法根本不触发。
 */
@Mixin(Screen.class)
public class ScreenMixin {
    // ---- 入场注册：added() 每次 setScreen 都走，返回也算 ----
    @Inject(method = "added", at = @At("HEAD"))
    private void quietdome$entranceAdded(CallbackInfo ci) {
        com.quietdome.client.EntranceAnim.onShow((Screen) (Object) this);
    }

    // ---- backdrop ----
    @Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true)
    private void quietdome$backdrop(GuiGraphicsExtractor g, int mx, int my, float a, CallbackInfo ci) {
        Object self = this;
        if (self instanceof SelectWorldScreen || self instanceof JoinMultiplayerScreen) {
            if (DomeConfig.get().menuBackground) {
                DomeBackground.draw(g);
                ci.cancel();
            }
            return;
        }
        if (DomeConfig.get().subScreens
                && !(self instanceof TitleScreen)
                && Minecraft.getInstance().level == null) {
            DomeBackground.drawBirdcage(g);
            ci.cancel();
        }
    }

    @Inject(method = "extractPanorama", at = @At("HEAD"), cancellable = true)
    private void quietdome$panorama(GuiGraphicsExtractor g, float a, CallbackInfo ci) {
        if ((Object) this instanceof TitleScreen && DomeConfig.get().menuBackground) {
            DomeBackground.draw(g);
            ci.cancel();
        }
    }

    // ---- overlay: entrance curtain + badge + selection glow (final method) ----
    @Inject(method = "extractRenderStateWithTooltipAndSubtitles", at = @At("TAIL"))
    private void quietdome$overlay(GuiGraphicsExtractor g, int mx, int my, float d, CallbackInfo ci) {
        Object self = this;
        int w = g.guiWidth();
        int h = g.guiHeight();

        // 入场黑幕：从黑淡入。
        // 主菜单放大转场的时候先不叠这层。
        boolean introOwns = self instanceof TitleScreen
                && com.quietdome.client.DomeTransition.isPlaying();
        if (!introOwns) {
            float p = com.quietdome.client.EntranceAnim.progress((Screen) (Object) this);
            if (p < 1f) {
                int a = com.quietdome.client.EntranceAnim.curtainAlpha(p, 220);
                g.fill(0, 0, w, h, DomeRender.argb(a, 0, 0, 0));
            }
        }

        if (DomeConfig.get().starfield
                && Minecraft.getInstance().level == null
                && SelectionGlow.isMenuScreen((Screen) (Object) this)) {
            // 主菜单按钮不许被侵蚀满（不然玩家点不动卡死），
            // 所以封顶 72%。其它菜单界面可以到 100%。
            float cap = (self instanceof TitleScreen) ? 0.72f : 1.0f;
            SelectionGlow.draw((Screen) (Object) this, g, cap);
        }
    }
}
