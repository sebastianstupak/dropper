package com.simplemod.platform.fabric;

import com.simplemod.items.TestSword;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * Fabric-specific item registration for TestSword
 */
public class TestSwordFabric {
    public static void register() {
        // Example Fabric registration:
        // Registry.register(
        //     Registries.ITEM,
        //     Identifier.of("simplemod", TestSword.ID),
        //     TestSword.INSTANCE
        // );
    }
}