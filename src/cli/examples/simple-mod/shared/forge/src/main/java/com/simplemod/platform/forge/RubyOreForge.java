package com.simplemod.platform.forge;

import com.simplemod.blocks.RubyOre;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Forge-specific block registration for RubyOre
 */
public class RubyOreForge {
    // Example Forge registration:
    // public static final DeferredRegister<Block> BLOCKS =
    //     DeferredRegister.create(ForgeRegistries.BLOCKS, "simplemod");
    //
    // public static final DeferredRegister<Item> ITEMS =
    //     DeferredRegister.create(ForgeRegistries.ITEMS, "simplemod");
    //
    // public static final RegistryObject<Block> RUBY_ORE =
    //     BLOCKS.register(RubyOre.ID, () -> RubyOre.INSTANCE);
    //
    // public static final RegistryObject<Item> RUBY_ORE_ITEM =
    //     ITEMS.register(RubyOre.ID, () -> new BlockItem(RUBY_ORE.get(), new Item.Properties()));
}