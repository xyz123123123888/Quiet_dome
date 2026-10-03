package com.quietdome.client;

import net.minecraft.util.Mth;

/**
 * 进主菜单时放一次的转场（没视频的时候用这个）：
 *
 *   FALL  坠入黑洞，画面渐黑
 *   BLACK 全黑停一下
 *   WHITE 慢慢泛白
 *   BLOOM 白色从中间往外扩、消散，露出菜单
 *   DONE  菜单完全可见
 */
public final class DomeTransition {
    private DomeTransition() {}

    public enum Phase { FALL, BLACK, WHITE, BLOOM, DONE }

    private static final long FALL_MS  = 800;
    private static final long BLACK_MS = 300;
    private static final long WHITE_MS = 650;
    private static final long BLOOM_MS = 850;

    private static long startMs = -1;
    private static Phase phase = Phase.DONE;
    private static boolean played = false;

    public static synchronized void triggerOnce() {
        if (played) return;
        played = true;
        play();
    }

    public static synchronized void play() {
        startMs = System.currentTimeMillis();
        phase = Phase.FALL;
    }

    public static synchronized boolean isPlaying() {
        return phase() != Phase.DONE;
    }

    private static synchronized long elapsed() {
        return startMs < 0 ? Long.MAX_VALUE : System.currentTimeMillis() - startMs;
    }

    public static synchronized Phase phase() {
        long e = elapsed();
        long t1 = FALL_MS;
        long t2 = t1 + BLACK_MS;
        long t3 = t2 + WHITE_MS;
        long t4 = t3 + BLOOM_MS;
        Phase p;
        if (e < t1) p = Phase.FALL;
        else if (e < t2) p = Phase.BLACK;
        else if (e < t3) p = Phase.WHITE;
        else if (e < t4) p = Phase.BLOOM;
        else p = Phase.DONE;
        phase = p;
        return p;
    }

    public static synchronized float fall() {
        return Mth.clamp(elapsed() / (float) FALL_MS, 0f, 1f);
    }

    /** 泛白进度 0..1。 */
    public static synchronized float white() {
        long e = elapsed() - FALL_MS - BLACK_MS;
        return Mth.clamp(e / (float) WHITE_MS, 0f, 1f);
    }

    /** 扩散进度 0..1。 */
    public static synchronized float bloom() {
        long e = elapsed() - FALL_MS - BLACK_MS - WHITE_MS;
        return Mth.clamp(e / (float) BLOOM_MS, 0f, 1f);
    }

    /** 黑幕透明度 0..1（下坠那步）。 */
    public static synchronized float blackAlpha() {
        return switch (phase()) {
            case FALL -> Mth.clamp(fall() * 1.2f, 0f, 1f);
            case BLACK -> 1f;
            default -> 0f;
        };
    }

    /** 白幕透明度 0..1（泛白那步）。 */
    public static synchronized float whiteAlpha() {
        return switch (phase()) {
            case WHITE -> white();
            case BLOOM -> 1f - Mth.clamp(bloom() * 1.15f, 0f, 1f);
            default -> 0f;
        };
    }

    /** 扩散圆洞的半径比例，0 到 1.2。 */
    public static synchronized float bloomRadius() {
        return phase() == Phase.BLOOM ? Mth.clamp(bloom() * 1.25f, 0f, 1.2f)
                : (phase() == Phase.DONE ? 1.2f : 0f);
    }
}
