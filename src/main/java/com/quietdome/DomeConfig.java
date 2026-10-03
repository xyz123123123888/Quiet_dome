package com.quietdome;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** 视觉开关。文件不在就用默认值建一个。 */
public final class DomeConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("quiet_dome_visual.json");

    public boolean loadingScreen = true;      // 黑洞加载屏
    public boolean menuBackground = true;     // solivault 背景
    public boolean transitions = true;        // 转场动画
    public boolean starfield = true;          // 繁星 UI
    public boolean redWaterfall = true;       // 红色瀑布色调
    public boolean subScreens = true;         // birdcage 子界面
    public float blurStrength = 0.6f;         // 预留
    public float backgroundZoom = 1.08f;      // 静态背景放大倍率
    /** 背景视频的解码宽度，高度按原片比例推。 */
    public int videoResolution = 1280;        // 640 / 960 / 1280 / 1920

    private static DomeConfig cache;

    private DomeConfig() {}

    public static synchronized DomeConfig get() {
        if (cache == null) {
            cache = read();
        }
        return cache;
    }

    private static DomeConfig read() {
        try {
            if (Files.isRegularFile(PATH)) {
                DomeConfig c = GSON.fromJson(Files.readString(PATH, StandardCharsets.UTF_8), DomeConfig.class);
                if (c != null) return c;
            }
        } catch (IOException | RuntimeException ignored) {
        }
        DomeConfig c = new DomeConfig();
        c.write();
        return c;
    }

    /** 把当前值写回文件（改完字段记得调）。 */
    public void save() {
        write();
    }

    private void write() {
        try {
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, GSON.toJson(this), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }
}
