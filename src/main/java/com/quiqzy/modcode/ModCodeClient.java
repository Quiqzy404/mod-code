package com.quiqzy.modcode;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ModCodeClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("modcode");

    @Override
    public void onInitializeClient() {
        LOGGER.info("Mod Code is running. Add your client-side Fabric code to this project.");
    }
}
