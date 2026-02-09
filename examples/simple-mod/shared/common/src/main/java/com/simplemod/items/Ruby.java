package com.simplemod.items;

/**
 * Custom item: Ruby
 *
 * Registration pattern for multi-loader compatibility:
 * - Fabric: Use FabricItemSettings or Item.Settings
 * - Forge/NeoForge: Use Item.Properties
 *
 * This base class provides the shared logic.
 * Loader-specific registration happens in platform code.
 */
public class Ruby {
    public static final String ID = "ruby";

    // TODO: Implement item logic
    // Example:
    // public static final Item INSTANCE = new Item(new Item.Settings());
    //
    // For tools:
    // public static final Item INSTANCE = new SwordItem(
    //     ToolMaterial.IRON, new Item.Settings().attributeModifiers(...)
    // );
    //
    // For food:
    // public static final Item INSTANCE = new Item(
    //     new Item.Settings().food(new FoodComponent.Builder()
    //         .nutrition(4).saturationModifier(0.3f).build())
    // );
}