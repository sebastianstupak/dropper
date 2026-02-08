package com.examplemod.items;

import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;

/**
 * Custom Ruby Sword item.
 * Shared across all versions and loaders.
 */
public class RubySword extends SwordItem {

    public RubySword() {
        super(
            Tiers.DIAMOND,  // Tier (diamond-level)
            3,              // Attack damage bonus
            -2.4F,          // Attack speed
            new Properties()
        );
    }
}
