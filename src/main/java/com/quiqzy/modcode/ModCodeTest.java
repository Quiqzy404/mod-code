package com.quiqzy.modcode;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(ModCodeTest.MOD_ID)
public final class ModCodeTest {
    public static final String MOD_ID = "modcode";
    private static final Logger LOGGER = LogUtils.getLogger();

    public ModCodeTest() {
        LOGGER.info("Hello from Quiqzy's first mod! Your code is running inside Minecraft Forge.");
    }
}