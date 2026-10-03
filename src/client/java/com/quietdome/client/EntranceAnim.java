package com.quietdome.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.Mth;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * 每个界面显示时从黑幕淡入。
 *
 * 记下显示时刻，接下来 {@link #DURATION_MS} 毫秒里盖一层慢慢变透明的黑，
 * 免得界面硬切看着突兀。
 */
public final class EntranceAnim {
    private EntranceAnim() {}

    public static final long DURATION_MS = 380L;

    private static final Map<Screen, Long> START = new IdentityHashMap<>();

    /**
     * 从 Screen.added() 调——每次 setScreen 都会走到，包括"返回"到之前建好的界面
     * （返回时复用的还是同一个实例）。
     * 时间戳是覆盖写，所以每次显示都会重放淡入。
     */
    public static synchronized void onShow(Screen screen) {
        if (START.size() > 64) {
            START.clear();
        }
        START.put(screen, System.currentTimeMillis());
    }

    /** 0..1，1 表示淡入结束。 */
    public static synchronized float progress(Screen screen) {
        Long start = START.get(screen);
        if (start == null) return 1f;
        long e = System.currentTimeMillis() - start;
        if (e >= DURATION_MS) {
            START.remove(screen);
            return 1f;
        }
        float p = e / (float) DURATION_MS;
        // ease-out
        return Mth.clamp(1f - (1f - p) * (1f - p), 0f, 1f);
    }

    /** 还盖在界面上的黑幕透明度 0..255。 */
    public static int curtainAlpha(float progress, int max) {
        return (int) ((1f - Mth.clamp(progress, 0f, 1f)) * max);
    }
}
