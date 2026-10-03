package com.quietdome.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.renderer.texture.DynamicTexture;

import java.util.function.Supplier;

/**
 * 用 LINEAR 过滤的 DynamicTexture。
 *
 * 原版 DynamicTexture 写死了 NEAREST，视频帧放大到全屏就是一格一格的马赛克。
 * 换成线性过滤就顺眼了。
 */
public class LinearDynamicTexture extends DynamicTexture {
    public LinearDynamicTexture(Supplier<String> label, NativeImage image) {
        super(label, image);
        this.sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
    }
}
