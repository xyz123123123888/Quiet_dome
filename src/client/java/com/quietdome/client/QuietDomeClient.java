package com.quietdome.client;

import com.quietdome.Alias;
import com.quietdome.AliasManager;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuietDomeClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("quiet_dome");

    @Override
    public void onInitializeClient() {
        Alias a = AliasManager.get();
        LOGGER.info("[Quiet Dome] This machine presents as: {} ({})", a.enName(), a.modId());

        // 先在后台线程把内置视频解出来，
        // 免得第一个界面卡在磁盘 IO 上。
        Thread warm = new Thread(() -> {
            try {
                VideoManager.ensureExtracted();
            } catch (Throwable t) {
                LOGGER.warn("[Quiet Dome] video pre-extract failed: {}", t.toString());
            }
        }, "quiet_dome-video-extract");
        warm.setDaemon(true);
        warm.start();
    }
}
