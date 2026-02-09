package com.simplemod.platform.forge;

import com.simplemod.blocks.TestOre;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Forge-specific block registration for TestOre
 */
public class TestOreForge {
    // Example Forge registration:
    // public static final DeferredRegister<Block> BLOCKS =
    //     DeferredRegister.create(ForgeRegistries.BLOCKS, "simplemod");
    //
    // public static final DeferredRegister<Item> ITEMS =
    //     DeferredRegister.create(ForgeRegistries.ITEMS, "simplemod");
    //
    // public static final RegistryObject<Block> TEST_ORE =
    //     BLOCKS.register(TestOre.ID, () -> TestOre.INSTANCE);
    //
    // public static final RegistryObject<Item> TEST_ORE_ITEM =
    //     ITEMS.register(TestOre.ID, () -> new BlockItem(TEST_ORE.get(), new Item.Properties()));
}