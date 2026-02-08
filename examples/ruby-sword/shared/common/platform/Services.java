package com.examplemod.platform;

import java.util.ServiceLoader;

/**
 * Service loader registry for platform services.
 * Automatically discovers platform implementations via META-INF/services/
 */
public class Services {

    /**
     * Platform helper service - provides platform-specific implementations.
     */
    public static final PlatformHelper PLATFORM = load(PlatformHelper.class);

    /**
     * Loads a service using Java ServiceLoader.
     */
    private static <T> T load(Class<T> clazz) {
        return ServiceLoader.load(clazz)
            .findFirst()
            .orElseThrow(() -> new NullPointerException(
                "Failed to load service for " + clazz.getName() +
                ". Make sure META-INF/services/ is configured correctly."
            ));
    }
}
