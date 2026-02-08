package com.examplemod.fabric;

import com.examplemod.ExampleMod;
import net.fabricmc.api.ModInitializer;

/**
 * Fabric entrypoint for Example Mod.
 * Version-specific: MC 1.21.1
 */
public class ExampleModFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        // Initialize common mod code
        ExampleMod.init();
    }
}
