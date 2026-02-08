package com.examplemod.platform;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

/**
 * Fabric implementation of PlatformHelper.
 * Registered via META-INF/services/ (auto-generated).
 */
public class FabricPlatformHelper implements PlatformHelper {

    @Override
    public String getPlatformName() {
        return "Fabric";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public Path getConfigDirectory() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public String getMinecraftVersion() {
        return FabricLoader.getInstance()
            .getModContainer("minecraft")
            .map(mod -> mod.getMetadata().getVersion().getFriendlyString())
            .orElse("unknown");
    }

    @Override
    public boolean isClientSide() {
        return FabricLoader.getInstance().getEnvironmentType().name().equals("CLIENT");
    }
}
