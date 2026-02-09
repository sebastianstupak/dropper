package com.simplemod.platform.forge;

import com.simplemod.blocks.RubyBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Forge-specific block registration for RubyBlock
 */
public class RubyBlockForge {
    // Example Forge registration:
    // public static final DeferredRegister<Block> BLOCKS =
    //     DeferredRegister.create(ForgeRegistries.BLOCKS, "simplemod");
    //
    // public static final DeferredRegister<Item> ITEMS =
    //     DeferredRegister.create(ForgeRegistries.ITEMS, "simplemod");
    //
    // public static final RegistryObject<Block> RUBY_BLOCK =
    //     BLOCKS.register(RubyBlock.ID, () -> RubyBlock.INSTANCE);
    //
    // public static final RegistryObject<Item> RUBY_BLOCK_ITEM =
    //     ITEMS.register(RubyBlock.ID, () -> new BlockItem(RUBY_BLOCK.get(), new Item.Properties()));
}