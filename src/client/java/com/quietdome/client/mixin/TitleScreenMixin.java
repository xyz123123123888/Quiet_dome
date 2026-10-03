package com.quietdome.client.mixin;

import com.quietdome.DomeConfig;
import com.quietdome.client.DomeRender;
import com.quietdome.client.DomeTransition;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class TitleScreenMixin {
    @Inject(method = "init", at = @At("HEAD"))
    private void quietdome$intro(CallbackInfo ci) {
        if (!DomeConfig.get().transitions) return;
        DomeTransition.triggerOnce();
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void quietdome$overlay(GuiGraphicsExtractor g, int mx, int my, float d, CallbackInfo ci) {
        if (DomeConfig.get().transitions && DomeTransition.isPlaying()) {
            int w = g.guiWidth();
            int h = g.guiHeight();
            switch (DomeTransition.phase()) {
                case FALL, BLACK -> {
                    float black = DomeTransition.blackAlpha();
                    if (black > 0.001f) {
                        g.fill(0, 0, w, h, DomeRender.argb((int) (black * 255), 0, 0, 0));
                    }
                }
                case WHITE -> {
                    float wa = DomeTransition.whiteAlpha();
                    // 底下全黑，白在上面慢慢压上来
                    g.fill(0, 0, w, h, DomeRender.argb(0xFF, 0, 0, 0));
                    g.fill(0, 0, w, h, DomeRender.argb((int) (wa * 255), 0xFF, 0xFF, 0xFF));
                }
                case BLOOM -> {
                    // 白色从中心往外扩、消散，露出菜单
                    DomeRender.blackOutsideCircle(g, DomeTransition.bloomRadius(),
                            DomeRender.argb((int) (DomeTransition.whiteAlpha() * 255), 0xFF, 0xFF, 0xFF));
                }
                default -> { }
            }
        }
        // 选中特效在 ScreenMixin 里统一处理
    }
}
