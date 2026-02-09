package com.simplemod.platform.fabric;

import com.simplemod.blocks.TestOre;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * Fabric-specific block registration for TestOre
 */
public class TestOreFabric {
    public static void register() {
        // Example Fabric registration:
        // Block block = Registry.register(
        //     Registries.BLOCK,
        //     Identifier.of("simplemod", TestOre.ID),
        //     TestOre.INSTANCE
        // );
        //
        // Registry.register(
        //     Registries.ITEM,
        //     Identifier.of("simplemod", TestOre.ID),
        //     new BlockItem(block, new Item.Settings())
        // );
    }
}