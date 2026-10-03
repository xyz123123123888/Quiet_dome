package com.quietdome;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** 存盘的配置：回退种子 + 显示开关。 */
public final class AliasConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH =
            FabricLoader.getInstance().getConfigDir().resolve("quiet_dome.json");

    public static final class Data {
        public boolean seedInitialized = false;
        public long seed = 0L;
        /** null 当 true。 */
        public Boolean showAlias = null;
    }

    private static Data cache;
    private static boolean loaded = false;

    private AliasConfig() {}

    private static synchronized Data data() {
        if (!loaded) {
            cache = read();
            if (!cache.seedInitialized) {
                cache.seed = AliasDeriver.freshSeed();
                cache.seedInitialized = true;
                write(cache);
            }
            loaded = true;
        }
        return cache;
    }

    /** 回退种子，第一次访问时生成。 */
    public static long seed() {
        return data().seed;
    }

    /** 水印还画不画（早就不画了，留着开关）。 */
    public static boolean showAlias() {
        Boolean v = data().showAlias;
        return v == null || v;
    }

    private static Data read() {
        try {
            if (Files.isRegularFile(PATH)) {
                Data d = GSON.fromJson(Files.readString(PATH, StandardCharsets.UTF_8), Data.class);
                if (d != null) {
                    return d;
                }
            }
        } catch (IOException | RuntimeException ignored) {
        }
        return new Data();
    }

    private static void write(Data data) {
        try {
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, GSON.toJson(data), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }
}
