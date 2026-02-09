package dev.dropper.commands

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import dev.dropper.util.FileUtil
import dev.dropper.util.Logger
import java.io.File

/**
 * Command to create a new entity in the mod
 * Generates entity class, renderer, model, texture, spawn egg, and registration code
 */
class CreateEntityCommand : DropperCommand(
    name = "entity",
    help = "Create a new entity with registration code, renderer, and assets"
) {
    private val name by argument(help = "Entity name in snake_case (e.g., custom_zombie)")
    private val type by option("--type", "-t", help = "Entity type: mob, projectile, animal, monster, villager").default("mob")
    private val spawnEgg by option("--spawn-egg", "-s", help = "Generate spawn egg item").default("true")

    override fun run() {
        val configFile = getConfigFile()

        if (!configFile.exists()) {
            Logger.error("No config.yml found. Are you in a Dropper project directory?")
            return
        }

        val modId = extractModId(configFile)
        if (modId == null) {
            Logger.error("Could not read mod ID from config.yml")
            return
        }

        // Sanitize mod ID for package names (remove hyphens and underscores)
        val sanitizedModId = FileUtil.sanitizeModId(modId)

        Logger.info("Creating entity: $name (type: $type)")

        // Generate common entity code
        generateEntityClass(projectDir, name, sanitizedModId, type)

        // Generate loader-specific implementations
        generateFabricEntity(projectDir, name, modId, sanitizedModId, type)
        generateForgeEntity(projectDir, name, modId, sanitizedModId, type)
        generateNeoForgeEntity(projectDir, name, modId, sanitizedModId, type)

        // Generate renderer classes (loader-specific)
        generateFabricRenderer(projectDir, name, modId, sanitizedModId)
        generateForgeRenderer(projectDir, name, modId, sanitizedModId)
        generateNeoForgeRenderer(projectDir, name, modId, sanitizedModId)

        // Generate entity model (if applicable)
        if (type in listOf("mob", "animal", "monster", "villager")) {
            generateEntityModel(projectDir, name, modId)
        }

        // Generate texture placeholder
        generateEntityTexture(projectDir, name, modId)

        // Generate spawn egg if requested
        if (spawnEgg == "true") {
            generateSpawnEgg(projectDir, name, modId, sanitizedModId)
        }

        // Generate lang entries
        generateLangEntries(projectDir, name, modId, spawnEgg == "true")

        Logger.success("Entity '$name' created successfully!")
        Logger.info("Next steps:")
        Logger.info("  1. Add texture: versions/shared/v1/assets/$modId/textures/entity/$name.png")
        if (spawnEgg == "true") {
            Logger.info("  2. Add spawn egg texture: versions/shared/v1/assets/$modId/textures/item/${name}_spawn_egg.png")
        }
        Logger.info("  3. Implement entity behavior in shared/common/src/main/java/com/$sanitizedModId/entities/${toClassName(name)}.java")
        Logger.info("  4. Build with: dropper build")
    }

    private fun extractModId(configFile: File): String? {
        val content = configFile.readText()
        return Regex("id:\\s*([a-z0-9-]+)").find(content)?.groupValues?.get(1)
    }

    private fun generateEntityClass(projectDir: File, entityName: String, sanitizedModId: String, type: String) {
        val className = toClassName(entityName)
        val baseClass = when (type) {
            "mob" -> "PathAwareEntity"
            "animal" -> "AnimalEntity"
            "monster" -> "HostileEntity"
            "villager" -> "VillagerEntity"
            "projectile" -> "ProjectileEntity"
            else -> "LivingEntity"
        }

        val content = """
            package com.$sanitizedModId.entities;

            /**
             * Custom entity: $className
             *
             * Registration pattern for multi-loader compatibility:
             * - Fabric: Use FabricEntityTypeBuilder or EntityType.Builder
             * - Forge/NeoForge: Use EntityType.Builder
             *
             * This base class provides the shared logic.
             * Loader-specific registration happens in platform code.
             *
             * Entity type: $type
             * Base class suggestion: $baseClass
             */
            public class $className {
                public static final String ID = "$entityName";

                // TODO: Implement entity logic
                // Example for PathAwareEntity (mob):
                // public static class ${className}Entity extends PathAwareEntity {
                //     public ${className}Entity(EntityType<? extends PathAwareEntity> entityType, World world) {
                //         super(entityType, world);
                //     }
                //
                //     @Override
                //     protected void initGoals() {
                //         this.goalSelector.add(0, new SwimGoal(this));
                //         this.goalSelector.add(1, new WanderAroundFarGoal(this, 1.0));
                //         this.goalSelector.add(2, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
                //         this.goalSelector.add(3, new LookAroundGoal(this));
                //     }
                //
                //     @Override
                //     public static DefaultAttributeContainer.Builder createAttributes() {
                //         return LivingEntity.createLivingAttributes()
                //             .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
                //             .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25);
                //     }
                // }
                //
                // For projectiles:
                // public static class ${className}Entity extends ProjectileEntity {
                //     public ${className}Entity(EntityType<? extends ProjectileEntity> entityType, World world) {
                //         super(entityType, world);
                //     }
                //
                //     @Override
                //     protected void onCollision(HitResult hitResult) {
                //         super.onCollision(hitResult);
                //         // Custom collision logic
                //     }
                // }
                //
                // For animals:
                // public static class ${className}Entity extends AnimalEntity {
                //     public ${className}Entity(EntityType<? extends AnimalEntity> entityType, World world) {
                //         super(entityType, world);
                //     }
                //
                //     @Override
                //     public @Nullable PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
                //         return null; // TODO: Implement breeding
                //     }
                // }
            }
        """.trimIndent()

        val entityFile = File(projectDir, "shared/common/src/main/java/com/$sanitizedModId/entities/$className.java")
        FileUtil.writeText(entityFile, content)

        Logger.info("  ✓ Created entity class: shared/common/src/main/java/com/$sanitizedModId/entities/$className.java")
    }

    private fun generateFabricEntity(projectDir: File, entityName: String, modId: String, sanitizedModId: String, type: String) {
        val className = toClassName(entityName)
        val content = """
            package com.$sanitizedModId.platform.fabric;

            import com.$sanitizedModId.entities.$className;
            import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
            import net.minecraft.entity.EntityDimensions;
            import net.minecraft.entity.EntityType;
            import net.minecraft.entity.SpawnGroup;
            import net.minecraft.registry.Registries;
            import net.minecraft.registry.Registry;
            import net.minecraft.util.Identifier;

            /**
             * Fabric-specific entity registration for $className
             */
            public class ${className}Fabric {
                public static void register() {
                    // Example Fabric entity registration:
                    // EntityType<${className}.${className}Entity> entityType = Registry.register(
                    //     Registries.ENTITY_TYPE,
                    //     Identifier.of("$modId", $className.ID),
                    //     FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, ${className}.${className}Entity::new)
                    //         .dimensions(EntityDimensions.fixed(0.6f, 1.8f))
                    //         .trackRangeChunks(8)
                    //         .trackedUpdateRate(3)
                    //         .build()
                    // );
                    //
                    // FabricDefaultAttributeRegistry.register(entityType, ${className}.${className}Entity.createAttributes());
                }
            }
        """.trimIndent()

        val file = File(projectDir, "shared/fabric/src/main/java/com/$sanitizedModId/platform/fabric/${className}Fabric.java")
        FileUtil.writeText(file, content)

        Logger.info("  ✓ Created Fabric entity: shared/fabric/src/main/java/com/$sanitizedModId/platform/fabric/${className}Fabric.java")
    }

    private fun generateForgeEntity(projectDir: File, entityName: String, modId: String, sanitizedModId: String, type: String) {
        val className = toClassName(entityName)
        val content = """
            package com.$sanitizedModId.platform.forge;

            import com.$sanitizedModId.entities.$className;
            import net.minecraft.world.entity.EntityType;
            import net.minecraft.world.entity.MobCategory;
            import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
            import net.minecraftforge.eventbus.api.IEventBus;
            import net.minecraftforge.registries.DeferredRegister;
            import net.minecraftforge.registries.ForgeRegistries;
            import net.minecraftforge.registries.RegistryObject;

            /**
             * Forge-specific entity registration for $className
             */
            public class ${className}Forge {
                // Example Forge entity registration:
                // public static final DeferredRegister<EntityType<?>> ENTITIES =
                //     DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, "$modId");
                //
                // public static final RegistryObject<EntityType<${className}.${className}Entity>> ${entityName.uppercase().replace("-", "_")} =
                //     ENTITIES.register($className.ID, () -> EntityType.Builder.of(
                //         ${className}.${className}Entity::new, MobCategory.CREATURE)
                //         .sized(0.6f, 1.8f)
                //         .clientTrackingRange(8)
                //         .updateInterval(3)
                //         .build($className.ID));
                //
                // public static void registerAttributes(EntityAttributeCreationEvent event) {
                //     event.put(${entityName.uppercase().replace("-", "_")}.get(), ${className}.${className}Entity.createAttributes().build());
                // }
            }
        """.trimIndent()

        val file = File(projectDir, "shared/forge/src/main/java/com/$sanitizedModId/platform/forge/${className}Forge.java")
        FileUtil.writeText(file, content)

        Logger.info("  ✓ Created Forge entity: shared/forge/src/main/java/com/$sanitizedModId/platform/forge/${className}Forge.java")
    }

    private fun generateNeoForgeEntity(projectDir: File, entityName: String, modId: String, sanitizedModId: String, type: String) {
        val className = toClassName(entityName)
        val content = """
            package com.$sanitizedModId.platform.neoforge;

            import com.$sanitizedModId.entities.$className;
            import net.minecraft.core.registries.Registries;
            import net.minecraft.world.entity.EntityType;
            import net.minecraft.world.entity.MobCategory;
            import net.neoforged.bus.api.IEventBus;
            import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
            import net.neoforged.neoforge.registries.DeferredRegister;
            import net.neoforged.neoforge.registries.DeferredHolder;

            /**
             * NeoForge-specific entity registration for $className
             */
            public class ${className}NeoForge {
                // Example NeoForge entity registration:
                // public static final DeferredRegister<EntityType<?>> ENTITIES =
                //     DeferredRegister.create(Registries.ENTITY_TYPE, "$modId");
                //
                // public static final DeferredHolder<EntityType<?>, EntityType<${className}.${className}Entity>> ${entityName.uppercase().replace("-", "_")} =
                //     ENTITIES.register($className.ID, () -> EntityType.Builder.of(
                //         ${className}.${className}Entity::new, MobCategory.CREATURE)
                //         .sized(0.6f, 1.8f)
                //         .clientTrackingRange(8)
                //         .updateInterval(3)
                //         .build($className.ID));
                //
                // public static void registerAttributes(EntityAttributeCreationEvent event) {
                //     event.put(${entityName.uppercase().replace("-", "_")}.get(), ${className}.${className}Entity.createAttributes().build());
                // }
            }
        """.trimIndent()

        val file = File(projectDir, "shared/neoforge/src/main/java/com/$sanitizedModId/platform/neoforge/${className}NeoForge.java")
        FileUtil.writeText(file, content)

        Logger.info("  ✓ Created NeoForge entity: shared/neoforge/src/main/java/com/$sanitizedModId/platform/neoforge/${className}NeoForge.java")
    }

    private fun generateFabricRenderer(projectDir: File, entityName: String, modId: String, sanitizedModId: String) {
        val className = toClassName(entityName)
        val content = """
            package com.$sanitizedModId.client.renderer.fabric;

            import com.$sanitizedModId.entities.$className;
            import net.minecraft.client.render.entity.EntityRendererFactory;
            import net.minecraft.client.render.entity.MobEntityRenderer;
            import net.minecraft.client.render.entity.model.EntityModelLayers;
            import net.minecraft.util.Identifier;

            /**
             * Fabric-specific renderer for $className
             */
            public class ${className}Renderer extends MobEntityRenderer<${className}.${className}Entity, ${className}Model> {
                private static final Identifier TEXTURE = Identifier.of("$modId", "textures/entity/$entityName.png");

                public ${className}Renderer(EntityRendererFactory.Context context) {
                    super(context, new ${className}Model(context.getPart(EntityModelLayers.PLAYER)), 0.5f);
                }

                @Override
                public Identifier getTexture(${className}.${className}Entity entity) {
                    return TEXTURE;
                }

                // Registration example (in client initializer):
                // EntityRendererRegistry.register(${className}Fabric.ENTITY_TYPE, ${className}Renderer::new);
            }
        """.trimIndent()

        val file = File(projectDir, "shared/fabric/src/main/java/com/$sanitizedModId/client/renderer/fabric/${className}Renderer.java")
        FileUtil.writeText(file, content)

        Logger.info("  ✓ Created Fabric renderer: shared/fabric/src/main/java/com/$sanitizedModId/client/renderer/fabric/${className}Renderer.java")
    }

    private fun generateForgeRenderer(projectDir: File, entityName: String, modId: String, sanitizedModId: String) {
        val className = toClassName(entityName)
        val content = """
            package com.$sanitizedModId.client.renderer.forge;

            import com.$sanitizedModId.entities.$className;
            import net.minecraft.client.renderer.entity.EntityRendererProvider;
            import net.minecraft.client.renderer.entity.MobRenderer;
            import net.minecraft.client.model.geom.ModelLayers;
            import net.minecraft.resources.ResourceLocation;

            /**
             * Forge-specific renderer for $className
             */
            public class ${className}Renderer extends MobRenderer<${className}.${className}Entity, ${className}Model> {
                private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("$modId", "textures/entity/$entityName.png");

                public ${className}Renderer(EntityRendererProvider.Context context) {
                    super(context, new ${className}Model(context.bakeLayer(ModelLayers.PLAYER)), 0.5f);
                }

                @Override
                public ResourceLocation getTextureLocation(${className}.${className}Entity entity) {
                    return TEXTURE;
                }

                // Registration example (in client setup):
                // EntityRenderers.register(${className}Forge.ENTITY_TYPE.get(), ${className}Renderer::new);
            }
        """.trimIndent()

        val file = File(projectDir, "shared/forge/src/main/java/com/$sanitizedModId/client/renderer/forge/${className}Renderer.java")
        FileUtil.writeText(file, content)

        Logger.info("  ✓ Created Forge renderer: shared/forge/src/main/java/com/$sanitizedModId/client/renderer/forge/${className}Renderer.java")
    }

    private fun generateNeoForgeRenderer(projectDir: File, entityName: String, modId: String, sanitizedModId: String) {
        val className = toClassName(entityName)
        val content = """
            package com.$sanitizedModId.client.renderer.neoforge;

            import com.$sanitizedModId.entities.$className;
            import net.minecraft.client.renderer.entity.EntityRendererProvider;
            import net.minecraft.client.renderer.entity.MobRenderer;
            import net.minecraft.client.model.geom.ModelLayers;
            import net.minecraft.resources.ResourceLocation;

            /**
             * NeoForge-specific renderer for $className
             */
            public class ${className}Renderer extends MobRenderer<${className}.${className}Entity, ${className}Model> {
                private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("$modId", "textures/entity/$entityName.png");

                public ${className}Renderer(EntityRendererProvider.Context context) {
                    super(context, new ${className}Model(context.bakeLayer(ModelLayers.PLAYER)), 0.5f);
                }

                @Override
                public ResourceLocation getTextureLocation(${className}.${className}Entity entity) {
                    return TEXTURE;
                }

                // Registration example (in client setup):
                // EntityRenderers.register(${className}NeoForge.ENTITY_TYPE.get(), ${className}Renderer::new);
            }
        """.trimIndent()

        val file = File(projectDir, "shared/neoforge/src/main/java/com/$sanitizedModId/client/renderer/neoforge/${className}Renderer.java")
        FileUtil.writeText(file, content)

        Logger.info("  ✓ Created NeoForge renderer: shared/neoforge/src/main/java/com/$sanitizedModId/client/renderer/neoforge/${className}Renderer.java")
    }

    private fun generateEntityModel(projectDir: File, entityName: String, modId: String) {
        val className = toClassName(entityName)
        val content = """
            {
              "format_version": "1.12.0",
              "minecraft:geometry": [
                {
                  "description": {
                    "identifier": "geometry.$modId.$entityName",
                    "texture_width": 64,
                    "texture_height": 64,
                    "visible_bounds_width": 2,
                    "visible_bounds_height": 3,
                    "visible_bounds_offset": [0, 1, 0]
                  },
                  "bones": [
                    {
                      "name": "head",
                      "pivot": [0, 24, 0],
                      "cubes": [
                        {
                          "origin": [-4, 24, -4],
                          "size": [8, 8, 8],
                          "uv": [0, 0]
                        }
                      ]
                    },
                    {
                      "name": "body",
                      "pivot": [0, 24, 0],
                      "cubes": [
                        {
                          "origin": [-4, 12, -2],
                          "size": [8, 12, 4],
                          "uv": [16, 16]
                        }
                      ]
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val modelFile = File(projectDir, "versions/shared/v1/assets/$modId/models/entity/$entityName.json")
        FileUtil.writeText(modelFile, content)

        Logger.info("  ✓ Created entity model: versions/shared/v1/assets/$modId/models/entity/$entityName.json")
    }

    private fun generateEntityTexture(projectDir: File, entityName: String, modId: String) {
        val textureFile = File(projectDir, "versions/shared/v1/assets/$modId/textures/entity/$entityName.png")
        textureFile.parentFile.mkdirs()
        if (!textureFile.exists()) {
            textureFile.createNewFile()
            Logger.info("  ✓ Created placeholder texture: versions/shared/v1/assets/$modId/textures/entity/$entityName.png")
        }
    }

    private fun generateSpawnEgg(projectDir: File, entityName: String, modId: String, sanitizedModId: String) {
        val className = toClassName(entityName)

        // Generate spawn egg item registration
        val itemContent = """
            package com.$sanitizedModId.items;

            /**
             * Spawn egg for $className entity
             */
            public class ${className}SpawnEgg {
                public static final String ID = "${entityName}_spawn_egg";

                // TODO: Implement spawn egg item
                // Example:
                // public static final SpawnEggItem INSTANCE = new SpawnEggItem(
                //     ${className}Fabric.ENTITY_TYPE,
                //     0x7E9680, // Primary color
                //     0x7E7E7E, // Secondary color
                //     new Item.Settings()
                // );
            }
        """.trimIndent()

        val itemFile = File(projectDir, "shared/common/src/main/java/com/$sanitizedModId/items/${className}SpawnEgg.java")
        FileUtil.writeText(itemFile, itemContent)

        // Generate item model
        val modelContent = """
            {
              "parent": "item/template_spawn_egg"
            }
        """.trimIndent()

        val modelFile = File(projectDir, "versions/shared/v1/assets/$modId/models/item/${entityName}_spawn_egg.json")
        FileUtil.writeText(modelFile, modelContent)

        // Create placeholder texture
        val textureFile = File(projectDir, "versions/shared/v1/assets/$modId/textures/item/${entityName}_spawn_egg.png")
        textureFile.parentFile.mkdirs()
        if (!textureFile.exists()) {
            textureFile.createNewFile()
        }

        Logger.info("  ✓ Created spawn egg item: shared/common/src/main/java/com/$modId/items/${className}SpawnEgg.java")
        Logger.info("  ✓ Created spawn egg model: versions/shared/v1/assets/$modId/models/item/${entityName}_spawn_egg.json")
        Logger.info("  ✓ Created placeholder spawn egg texture: versions/shared/v1/assets/$modId/textures/item/${entityName}_spawn_egg.png")
    }

    private fun generateLangEntries(projectDir: File, entityName: String, modId: String, hasSpawnEgg: Boolean) {
        val langFile = File(projectDir, "versions/shared/v1/assets/$modId/lang/en_us.json")

        val displayName = entityName.split("_").joinToString(" ") { it.capitalize() }

        val langEntries = mutableMapOf(
            "entity.$modId.$entityName" to displayName
        )

        if (hasSpawnEgg) {
            langEntries["item.$modId.${entityName}_spawn_egg"] = "$displayName Spawn Egg"
        }

        // Read existing lang file if it exists
        val existingEntries = if (langFile.exists()) {
            val content = FileUtil.readText(langFile)
            try {
                // Simple JSON parsing for demonstration
                mutableMapOf<String, String>().apply {
                    content.lines().forEach { line ->
                        val match = Regex(""""([^"]+)":\s*"([^"]+)"""").find(line)
                        if (match != null) {
                            put(match.groupValues[1], match.groupValues[2])
                        }
                    }
                }
            } catch (e: Exception) {
                mutableMapOf()
            }
        } else {
            mutableMapOf()
        }

        // Merge entries
        existingEntries.putAll(langEntries)

        // Write lang file
        val langContent = buildString {
            appendLine("{")
            existingEntries.entries.forEachIndexed { index, entry ->
                append("  \"${entry.key}\": \"${entry.value}\"")
                if (index < existingEntries.size - 1) {
                    appendLine(",")
                } else {
                    appendLine()
                }
            }
            append("}")
        }

        FileUtil.writeText(langFile, langContent)

        Logger.info("  ✓ Updated lang file: versions/shared/v1/assets/$modId/lang/en_us.json")
    }

    private fun toClassName(snakeCase: String): String {
        return snakeCase.split("_").joinToString("") { it.capitalize() }
    }
}
