package com.examplemod.neoforge;

import com.examplemod.ExampleMod;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/**
 * NeoForge entrypoint for Example Mod.
 * Version-specific: MC 1.21.1
 */
@Mod(ExampleMod.MOD_ID)
public class ExampleModNeoforge {

    public ExampleModNeoforge(IEventBus modEventBus) {
        // Initialize common mod code
        ExampleMod.init();
    }
}
