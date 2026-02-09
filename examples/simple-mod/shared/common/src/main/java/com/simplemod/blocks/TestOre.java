package com.simplemod.blocks;

/**
 * Custom block: TestOre
 *
 * Registration pattern for multi-loader compatibility:
 * - Fabric: Use FabricBlockSettings or AbstractBlock.Settings
 * - Forge/NeoForge: Use BlockBehaviour.Properties
 *
 * This base class provides the shared logic.
 * Loader-specific registration happens in platform code.
 */
public class TestOre {
    public static final String ID = "test_ore";

    // TODO: Implement block logic
    // Example for basic block:
    // public static final Block INSTANCE = new Block(
    //     AbstractBlock.Settings.create()
    //         .strength(3.0f, 3.0f)
    //         .sounds(BlockSoundGroup.STONE)
    // );
    //
    // For ore blocks:
    // public static final Block INSTANCE = new Block(
    //     AbstractBlock.Settings.create()
    //         .strength(3.0f, 3.0f)
    //         .requiresTool()
    //         .sounds(BlockSoundGroup.STONE)
    // );
}