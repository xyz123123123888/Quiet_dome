package com.quietdome.client;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 管黑洞视频播放器，顺便负责找文件。
 *
 * 视频是打进 jar 的（assets/quiet_dome/videos/*.mp4），首次用的时候解到磁盘上，
 * 这样播放能走文件流，不用整个读进内存。
 * 玩家自己在 config/quiet_dome/videos/ 放了同名文件就用玩家的，不覆盖。
 */
public final class VideoManager {
    private VideoManager() {}

    private static final Logger LOGGER = LoggerFactory.getLogger("quiet_dome");

    public static final int FPS = 15;

    /** 原片宽高比：heidong 16:9。 */
    private static final float HEIDONG_ASPECT = 16f / 9f;

    private static int heightFor(int width, float aspect) {
        int h = Math.round(width / aspect);
        return (h + 1) & ~1; // even
    }

    private static VideoPlayer blackhole;

    public static Path videoDir() {
        return FabricLoader.getInstance().getConfigDir().resolve("quiet_dome").resolve("videos");
    }

    /** 找视频：先看 config 里玩家有没有自己放，没有就从 jar 解一份出来。 */
    private static Path resolve(String name) {
        Path target = videoDir().resolve(name);
        if (Files.isRegularFile(target)) {
            return target;
        }
        return extract(name, target);
    }

    private static Path extract(String name, Path target) {
        String resource = "/assets/quiet_dome/videos/" + name;
        try (InputStream in = VideoManager.class.getResourceAsStream(resource)) {
            if (in == null) {
                return null; // not bundled
            }
            Files.createDirectories(target.getParent());
            Path tmp = target.resolveSibling(name + ".part");
            try (OutputStream out = Files.newOutputStream(tmp)) {
                in.transferTo(out);
            }
            Files.move(tmp, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            LOGGER.info("[Quiet Dome] Extracted bundled video {}", name);
            return target;
        } catch (Throwable t) {
            LOGGER.warn("[Quiet Dome] Failed to extract bundled video {}: {}", name, t.toString());
            return null;
        }
    }

    /** 把内置视频解到磁盘（重复调用没事）。 */
    public static void ensureExtracted() {
        resolve("heidong.mp4");
    }

    public static synchronized VideoPlayer blackhole() {
        if (blackhole == null) {
            Path p = resolve("heidong.mp4");
            if (p != null) {
                int w = com.quietdome.DomeConfig.get().videoResolution;
                blackhole = new VideoPlayer("heidong", p, w, heightFor(w, HEIDONG_ASPECT), FPS);
                blackhole.init();
            }
        }
        return blackhole;
    }

    private static volatile int pendingWidth = -1;
    private static volatile long lastChangeMs = 0;
    private static Thread applyThread;

    /**
     * 请求换解码宽度。做了防抖：滑条拖的时候值一直在变，
     * 等它稳定约 150ms 才真正应用一次，不然每 tick 都重启解码线程，
     * 拖两下就把机器拖死了。实际应用在渲染线程。
     */
    public static void applyResolution(int width) {
        pendingWidth = width;
        lastChangeMs = System.currentTimeMillis();
        if (applyThread == null || !applyThread.isAlive()) {
            applyThread = new Thread(VideoManager::coalesceLoop, "quiet_dome-res-apply");
            applyThread.setDaemon(true);
            applyThread.start();
        }
    }

    private static void coalesceLoop() {
        while (true) {
            try {
                Thread.sleep(120);
            } catch (InterruptedException e) {
                return;
            }
            if (System.currentTimeMillis() - lastChangeMs < 150) {
                continue; // still changing
            }
            int w = pendingWidth;
            pendingWidth = -1;
            if (w > 0) {
                net.minecraft.client.Minecraft.getInstance().execute(() -> applyNow(w));
            }
            return;
        }
    }

    /** 渲染线程上真正把宽度应用下去。 */
    private static synchronized void applyNow(int width) {
        if (blackhole != null) {
            blackhole.setResolution(width, heightFor(width, HEIDONG_ASPECT));
        }
    }

    public static synchronized void closeAll() {
        if (blackhole != null) { blackhole.close(); blackhole = null; }
    }
}
