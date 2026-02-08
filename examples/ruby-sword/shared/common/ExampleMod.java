package com.examplemod;

import com.examplemod.platform.Services;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main mod class - platform agnostic.
 * Works across all loaders and versions.
 */
public class ExampleMod {
    public static final String MOD_ID = "example-mod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /**
     * Called by platform-specific entrypoints.
     */
    public static void init() {
        LOGGER.info("Initializing Example Mod on {}", Services.PLATFORM.getPlatformName());
        LOGGER.info("Running on MC {}", Services.PLATFORM.getMinecraftVersion());

        // Register items, blocks, etc.
        // This code works the same on all platforms
    }
}
