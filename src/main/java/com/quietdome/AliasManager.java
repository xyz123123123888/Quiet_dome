package com.quietdome;

/** 算一次本机别名，整个会话都缓存着。 */
public final class AliasManager {
    private static Alias resolved;

    private AliasManager() {}

    /** 算一次就缓存：先主机名，不行走种子。 */
    public static synchronized Alias get() {
        if (resolved == null) {
            int idx = AliasDeriver.fromHostname();
            if (idx < 0) {
                idx = AliasDeriver.fromSeed(AliasConfig.seed());
            }
            resolved = AliasRegistry.byIndex(idx);
        }
        return resolved;
    }
}
