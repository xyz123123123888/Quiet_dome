package com.quietdome.client;

import net.minecraft.client.gui.screens.Screen;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * 记录每个控件的"侵蚀度"（Phigros 第九章那个红/蓝抹除），配合黑洞粒子用。
 *
 * 规则：
 *  - 只有控件被悬停/聚焦时才涨。
 *  - 光标移开就暂停，进度留着不清零。
 *  - 每个界面能设上限（主菜单不让按钮彻底淹没，不然玩家点不动）。
 *  - 状态按 (Screen 实例, 控件) 存；界面一关就没了。
 */
public final class Corruption {
    private Corruption() {}

    /**
     * 从 0 涨到满侵蚀需要一直悬停多久。
     * 比原来 45s 慢了 89%（45s / 0.11 ≈ 409s）。
     * 同样侵蚀值下的粒子密度没变，只是涨得慢。
     */
    public static final long DECAY_MS = 409_091L;

    private static final class State {
        long accumulatedMs = 0L;
        long lastUpdateMs = -1L;
    }

    private static final Map<Screen, Map<Object, State>> BY_SCREEN = new WeakHashMap<>();

    private static State state(Screen screen, Object widget) {
        Map<Object, State> map = BY_SCREEN.computeIfAbsent(screen, s -> new IdentityHashMap<>());
        return map.computeIfAbsent(widget, w -> new State());
    }

    /** 按距上次调用的时间推进侵蚀，返回 0..cap。 */
    public static float touch(Screen screen, Object widget, float cap) {
        long now = System.currentTimeMillis();
        State st = state(screen, widget);
        if (st.lastUpdateMs >= 0L) {
            st.accumulatedMs += now - st.lastUpdateMs;
        }
        st.lastUpdateMs = now;
        return progress(st, cap);
    }

    /** 不推进（光标走了），只返回已存的侵蚀值 0..cap。 */
    public static float peek(Screen screen, Object widget, float cap) {
        State st = state(screen, widget);
        st.lastUpdateMs = -1L; // pause: the next touch must not add the idle gap
        return progress(st, cap);
    }

    private static float progress(State st, float cap) {
        float p = st.accumulatedMs / (float) DECAY_MS;
        if (p < 0f) p = 0f;
        if (p > cap) p = cap;
        return p;
    }

    /** 控件是否已经侵蚀满（不含 cap）。 */
    public static boolean isErased(Screen screen, Object widget) {
        return peek(screen, widget, 1f) >= 1f;
    }
}
