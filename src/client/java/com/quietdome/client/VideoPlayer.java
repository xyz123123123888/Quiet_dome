package com.quietdome.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;

import java.awt.image.BufferedImage;
import java.nio.IntBuffer;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 从磁盘流式读视频，把帧喂给 DynamicTexture。
 *
 * 解码丢后台线程，内存里同时只留一帧，其余留在磁盘。渲染线程只取最新那张。
 * 循环靠读完重开流实现——抄的 Wallpaper Engine 那套思路。
 */
public final class VideoPlayer {
    private final Path file;
    private final Identifier id;
    private volatile int targetW;
    private volatile int targetH;
    private final int targetFps;

    private DynamicTexture texture;
    private NativeImage image;
    private Thread decoder;
    private volatile boolean running;
    private final boolean loop;
    private volatile boolean finished = false;
    private volatile boolean started = false;

    /** Newest decoded frame as ABGR ints, or null if already consumed. */
    private final AtomicReference<int[]> latest = new AtomicReference<>();

    public VideoPlayer(String name, Path file, int targetW, int targetH, int targetFps) {
        this(name, file, targetW, targetH, targetFps, true);
    }

    public VideoPlayer(String name, Path file, int targetW, int targetH, int targetFps, boolean loop) {
        this.file = file;
        this.id = Identifier.fromNamespaceAndPath("quiet_dome", "video/" + name);
        this.targetW = targetW;
        this.targetH = targetH;
        this.targetFps = targetFps;
        this.loop = loop;
    }

    /** 非循环播放器播完没有。 */
    public boolean isFinished() {
        return finished;
    }

    /** 出过至少一帧没有。 */
    public boolean hasStarted() {
        return started;
    }

    /** 从头再放一遍。 */
    public synchronized void restart() {
        finished = false;
        started = false;
        latest.set(null);
    }

    public Identifier id() {
        return id;
    }

    /** 建 GPU 纹理，只能在渲染线程调。 */
    public void init() {
        image = new NativeImage(NativeImage.Format.RGBA, targetW, targetH, false);
        texture = new LinearDynamicTexture(() -> "quiet_dome_video_" + id.getPath(), image);
        Minecraft.getInstance().getTextureManager().register(id, texture);
        running = true;
        decoder = new Thread(this::decodeLoop, "quiet_dome-video-" + id.getPath());
        decoder.setDaemon(true);
        decoder.start();
    }

    private void decodeLoop() {
        Java2DFrameConverter conv = new Java2DFrameConverter();
        long frameInterval = (long) (1000.0 / targetFps);
        while (running) {
            try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(file.toFile())) {
                grabber.setImageWidth(targetW);
                grabber.setImageHeight(targetH);
                grabber.setFrameRate(targetFps);
                grabber.start();

                while (running) {
                    long t0 = System.currentTimeMillis();
                    BufferedImage img = conv.convert(grabber.grabImage());
                    if (img == null) {
                        break; // end of stream
                    }
                    started = true;
                    latest.set(toAbgr(img));
                    long sleep = frameInterval - (System.currentTimeMillis() - t0);
                    if (sleep > 0) {
                        Thread.sleep(sleep);
                    }
                }
                if (!loop) {
                    finished = true;
                    return;
                }
            } catch (InterruptedException ie) {
                return;
            } catch (Throwable t) {
                return; // decoding failure: caller falls back to the still image
            }
        }
    }

    /**
     * 转成 targetW*targetH 的 ABGR 数组。
     *
     * 必须一次 getRGB 批量读。JavaCV 返回的是非标准 BufferedImage，
     * 逐像素 getRGB 能慢几十倍——之前卡顿就是栽在这。
     */
    private int[] toAbgr(BufferedImage img) {
        int iw = img.getWidth();
        int ih = img.getHeight();
        int[] argb = img.getRGB(0, 0, iw, ih, null, 0, iw);
        int[] px = new int[targetW * targetH];

        if (iw == targetW && ih == targetH) {
            for (int i = 0; i < px.length; i++) {
                int c = argb[i];
                px[i] = (c & 0xFF000000) | ((c & 0xFF) << 16) | (c & 0xFF00) | ((c >> 16) & 0xFF);
            }
            return px;
        }

        for (int y = 0; y < targetH; y++) {
            int srow = (y * ih / targetH) * iw;
            int row = y * targetW;
            for (int x = 0; x < targetW; x++) {
                int c = argb[srow + (x * iw / targetW)];
                px[row + x] = (c & 0xFF000000) | ((c & 0xFF) << 16) | (c & 0xFF00) | ((c >> 16) & 0xFF);
            }
        }
        return px;
    }

    /** 渲染线程：把最新帧写进 NativeImage 再传 GPU。有帧可画返回 true。 */
    public boolean uploadLatest() {
        if (texture == null || image == null) return false;
        int[] px = latest.getAndSet(null);
        if (px == null) return false;
        IntBuffer buf = image.getPixelBytes().asIntBuffer();
        buf.clear();
        buf.put(px);
        texture.upload();
        return true;
    }

    public int width() { return targetW; }
    public int height() { return targetH; }

    /** 上传最新帧并 cover 铺满：保比例、居中裁边、不变形。zoom > 1 再放大。 */
    public boolean draw(net.minecraft.client.gui.GuiGraphicsExtractor g, int areaW, int areaH, float zoom) {
        if (!uploadLatest() && texture == null) return false;
        if (texture == null) return false;

        float scale = Math.max(areaW / (float) targetW, areaH / (float) targetH) * zoom;
        int dw = Math.round(targetW * scale);
        int dh = Math.round(targetH * scale);
        int dx = (areaW - dw) / 2;
        int dy = (areaH - dh) / 2;

        g.blit(texture.getTextureView(), texture.getSampler(),
                dx, dy, dx + dw, dy + dh, 0f, 1f, 0f, 1f);
        return true;
    }

    /**
     * 换解码分辨率：停线程、重建纹理和 NativeImage、重开。只能渲染线程调。
     */
    public synchronized void setResolution(int w, int h) {
        if (w == targetW && h == targetH) return;
        boolean wasRunning = running;

        // stop decoder
        running = false;
        if (decoder != null) {
            decoder.interrupt();
            decoder = null;
        }

        // release GPU + native resources
        try {
            if (texture != null) {
                Minecraft.getInstance().getTextureManager().release(id);
                texture.close();
                texture = null;
            }
        } catch (Throwable ignored) {
        }
        if (image != null) {
            image.close();
            image = null;
        }

        // rebuild at the new size
        targetW = w;
        targetH = h;
        image = new NativeImage(NativeImage.Format.RGBA, targetW, targetH, false);
        texture = new LinearDynamicTexture(() -> "quiet_dome_video_" + id.getPath(), image);
        Minecraft.getInstance().getTextureManager().register(id, texture);
        latest.set(null);
        started = false;
        finished = false;

        if (wasRunning) {
            running = true;
            decoder = new Thread(this::decodeLoop, "quiet_dome-video-" + id.getPath());
            decoder.setDaemon(true);
            decoder.start();
        }
    }

    public void close() {
        running = false;
        if (decoder != null) decoder.interrupt();
        if (texture != null) {
            Minecraft.getInstance().getTextureManager().release(id);
            texture.close();
        }
        if (image != null) image.close();
    }
}
