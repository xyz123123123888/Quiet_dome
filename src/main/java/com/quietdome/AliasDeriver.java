package com.quietdome;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Random;

/**
 * 给每台机器算一个固定的别名下标。
 *
 * 先拿主机名做哈希；主机名拿不到就退回一个首次生成的随机种子，写进配置存着。
 * 结果落在 [0, 七个)。
 */
public final class AliasDeriver {
    private AliasDeriver() {}

    /** 算出下标；主机名读不到就返回 -1，让调用方走种子。 */
    public static int fromHostname() {
        try {
            String host = InetAddress.getLocalHost().getHostName();
            if (host == null || host.isBlank()) {
                return -1;
            }
            return hashToIndex(host);
        } catch (Throwable t) {
            return -1;
        }
    }

    /** 用存下来的种子算下标。 */
    public static int fromSeed(long seed) {
        return Math.floorMod((int) (seed ^ (seed >>> 32)), AliasRegistry.ALL.size());
    }

    private static int hashToIndex(String host) {
        byte[] bytes = host.getBytes(StandardCharsets.UTF_8);
        long h = 1125899906842597L; // 借 FNV 的起始值
        for (byte b : bytes) {
            h = 31 * h + b;
        }
        return Math.floorMod((int) (h ^ (h >>> 32)), AliasRegistry.ALL.size());
    }

    /** 生成一个新种子，只在第一次用。 */
    public static long freshSeed() {
        return new Random().nextLong();
    }
}
