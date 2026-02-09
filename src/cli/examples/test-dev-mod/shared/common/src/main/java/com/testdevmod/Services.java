package com.testdevmod;

import com.testdevmod.platform.PlatformHelper;
import java.util.ServiceLoader;

/**
 * Service loader for platform-specific implementations
 */
public class Services {
    public static final PlatformHelper PLATFORM = load(PlatformHelper.class);

    private static <T> T load(Class<T> clazz) {
        return ServiceLoader.load(clazz).findFirst()
            .orElseThrow(() -> new NullPointerException("Failed to load " + clazz));
    }
}