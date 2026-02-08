package com.examplemod.platform;

import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLLoader;

import java.nio.file.Path;

/**
 * NeoForge implementation of PlatformHelper.
 * Registered via META-INF/services/ (auto-generated).
 */
public class NeoforgePlatformHelper implements PlatformHelper {

    @Override
    public String getPlatformName() {
        return "NeoForge";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return LoadingModList.get().getModFileById(modId) != null;
    }

    @Override
    public Path getConfigDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public String getMinecraftVersion() {
        return "1.20.1"; // Could be dynamic from FMLLoader if needed
    }

    @Override
    public boolean isClientSide() {
        return FMLLoader.getDist() == Dist.CLIENT;
    }
}
