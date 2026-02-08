package com.examplemod.platform;

import java.nio.file.Path;

/**
 * Platform abstraction interface.
 * Implemented by each loader in shared/{loader}/
 */
public interface PlatformHelper {

    /**
     * Gets the name of the current platform (Fabric, Forge, NeoForge).
     */
    String getPlatformName();

    /**
     * Checks if a mod with the given ID is loaded.
     */
    boolean isModLoaded(String modId);

    /**
     * Gets the config directory for this instance.
     */
    Path getConfigDirectory();

    /**
     * Gets the current Minecraft version.
     */
    String getMinecraftVersion();

    /**
     * Checks if the game is running on the client side.
     */
    boolean isClientSide();
}
