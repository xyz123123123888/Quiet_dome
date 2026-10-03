package com.quietdome.client.mixin;

import com.quietdome.DomeConfig;
import com.quietdome.client.VideoManager;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在原版视频设置里加一根"静穹视频分辨率"滑条。
 *
 * 特意整个包了 try/catch：出错顶多打条日志、原版界面照旧。
 * 之前就因为这里把设置界面搞崩过，所以宁可它废掉也不能让设置打不开。
 */
@Mixin(VideoSettingsScreen.class)
public class VideoSettingsScreenMixin {
    private static final Logger LOGGER = LoggerFactory.getLogger("quiet_dome");

    @Inject(method = "addOptions", at = @At("TAIL"))
    private void quietdome$resolution(CallbackInfo ci) {
        try {
            int current = DomeConfig.get().videoResolution;

            OptionInstance<Integer> res = new OptionInstance<>(
                    "options.quiet_dome.videoResolution",
                    OptionInstance.noTooltip(),
                    (caption, value) -> Options.genericValueLabel(caption, Component.literal(value + "p")),
                    new OptionInstance.IntRange(240, 2160, true),
                    current,
                    value -> {
                        try {
                            DomeConfig cfg = DomeConfig.get();
                            cfg.videoResolution = value;
                            cfg.save();
                            VideoManager.applyResolution(value);
                        } catch (Throwable t) {
                            LOGGER.warn("[Quiet Dome] applying resolution failed: {}", t.toString());
                        }
                    }
            );

            ((OptionsSubScreenAccessor) (Object) this).quietdome$getList().addBig(res);
        } catch (Throwable t) {
            LOGGER.error("[Quiet Dome] could not add video resolution slider", t);
        }
    }
}
