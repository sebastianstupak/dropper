package com.simplemod.platform;

/**
 * Platform-specific helper interface
 */
public interface PlatformHelper {
    String getPlatformName();
    boolean isModLoaded(String modId);
}