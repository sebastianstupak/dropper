package com.examplemod.forge;

import com.examplemod.ExampleMod;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Forge entrypoint for Example Mod.
 * Version-specific: MC 1.20.4
 */
@Mod(ExampleMod.MOD_ID)
public class ExampleModForge {

    public ExampleModForge() {
        // Initialize common mod code
        ExampleMod.init();

        // Get mod event bus
        var modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register Forge-specific listeners
        // modEventBus.addListener(this::commonSetup);
    }
}
