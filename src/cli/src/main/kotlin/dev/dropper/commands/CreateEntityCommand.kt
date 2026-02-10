package dev.dropper.commands

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import dev.dropper.util.FileUtil
import dev.dropper.util.Logger
import dev.dropper.util.StringUtil
import dev.dropper.util.ValidationUtil
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
        val nameValidation = ValidationUtil.validateName(name, "Entity name")
        if (!nameValidation.isValid) {
            ValidationUtil.exitWithError(nameValidation)
            return
        }

        // Validate we're in a Dropper project
        val projectValidation = ValidationUtil.validateDropperProject(projectDir)
        if (!projectValidation.isValid) {
            ValidationUtil.exitWithError(projectValidation)
            return
        }

        val configFile = getConfigFile()

        val modId = extractModId(configFile)
        if (modId == null) {
            Logger.error("Could not read mod ID from config.yml")
            return
        }

        val sanitizedModId = FileUtil.sanitizeModId(modId)

        // Check for duplicates
        val duplicateCheck = ValidationUtil.checkDuplicate(
            projectDir, "entity", name,
            listOf("shared/common/src/main/java")
        )
        if (!duplicateCheck.isValid) {
            ValidationUtil.exitWithError(duplicateCheck)
            Logger.warn("Entity was not created to avoid overwriting existing files")
            return
        }

        Logger.info("Creating entity: $name (type: $type)")

        // Generate common entity class (Mojang mappings, Architectury remaps for Fabric)
        generateEntityClass(projectDir, name, sanitizedModId, type)

        // Generate/update Architectury registry
        generateOrUpdateModEntities(projectDir, name, modId, sanitizedModId, type)

        // Update main mod class init() to call ModEntities.init()
        updateMainModInit(projectDir, sanitizedModId, "ModEntities")

        // Generate common renderer (Mojang mappings)
        generateCommonRenderer(projectDir, name, modId, sanitizedModId)

        // Generate common entity model (Mojang mappings)
        if (type in listOf("mob", "animal", "monster", "villager")) {
            generateCommonEntityModel(projectDir, name, sanitizedModId)
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

    /**
     * Returns entity type metadata for the given type string.
     */
    private data class EntityTypeInfo(
        val baseClass: String,
        val importFqn: String,
        val spawnGroup: String,
        val mobCategory: String
    )

    private fun getEntityTypeInfo(type: String): EntityTypeInfo {
        // All names use Mojang mappings (Architectury Loom remaps for Fabric)
        return when (type) {
            "animal" -> EntityTypeInfo(
                "Animal", "net.minecraft.world.entity.animal.Animal",
                "SpawnGroup.CREATURE", "MobCategory.CREATURE"
            )
            "monster" -> EntityTypeInfo(
                "Monster", "net.minecraft.world.entity.monster.Monster",
                "SpawnGroup.MONSTER", "MobCategory.MONSTER"
            )
            "villager" -> EntityTypeInfo(
                "AbstractVillager", "net.minecraft.world.entity.npc.AbstractVillager",
                "SpawnGroup.CREATURE", "MobCategory.CREATURE"
            )
            "projectile" -> EntityTypeInfo(
                "ThrowableProjectile", "net.minecraft.world.entity.projectile.ThrowableProjectile",
                "SpawnGroup.MISC", "MobCategory.MISC"
            )
            else -> EntityTypeInfo(
                "PathfinderMob", "net.minecraft.world.entity.PathfinderMob",
                "SpawnGroup.CREATURE", "MobCategory.CREATURE"
            )
        }
    }

    private fun generateEntityClass(projectDir: File, entityName: String, sanitizedModId: String, type: String) {
        val className = toClassName(entityName)
        val info = getEntityTypeInfo(type)

        val entityBody = when (type) {
            "animal" -> generateAnimalEntityBody(className, info)
            "monster" -> generateMonsterEntityBody(className, info)
            "projectile" -> generateProjectileEntityBody(className, info)
            else -> generateMobEntityBody(className, info, type)
        }

        val content = """
            package com.$sanitizedModId.entities;

            import ${info.importFqn};
            import net.minecraft.world.entity.EntityType;
            import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
            import net.minecraft.world.entity.ai.attributes.Attributes;
            import net.minecraft.world.level.Level;

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
             * Base class suggestion: ${info.baseClass}
             */
            $entityBody
        """.trimIndent()

        val entityFile = File(projectDir, "shared/common/src/main/java/com/$sanitizedModId/entities/$className.java")
        FileUtil.writeText(entityFile, content)

        Logger.info("  ✓ Created entity class: shared/common/src/main/java/com/$sanitizedModId/entities/$className.java")
    }

    private fun generateMobEntityBody(className: String, info: EntityTypeInfo, type: String): String {
        return """public class $className extends ${info.baseClass} {
    public static final String ID = "${toSnakeCase(className)}";

    public $className(EntityType<? extends ${info.baseClass}> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new net.minecraft.world.entity.ai.goal.FloatGoal(this));
        this.goalSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.LookAtPlayerGoal(this, net.minecraft.world.entity.player.Player.class, 8.0F));
        this.goalSelector.addGoal(3, new net.minecraft.world.entity.ai.goal.RandomLookAroundGoal(this));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return ${info.baseClass}.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 20.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.25D);
    }
}"""
    }

    private fun generateAnimalEntityBody(className: String, info: EntityTypeInfo): String {
        return """public class $className extends ${info.baseClass} {
    public static final String ID = "${toSnakeCase(className)}";

    /**
     * The registered EntityType for this entity.
     * Set during platform-specific registration (Fabric/Forge/NeoForge).
     */
    public static EntityType<$className> ENTITY_TYPE;

    public $className(EntityType<? extends ${info.baseClass}> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new net.minecraft.world.entity.ai.goal.FloatGoal(this));
        this.goalSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.PanicGoal(this, 1.25D));
        this.goalSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.BreedGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new net.minecraft.world.entity.ai.goal.LookAtPlayerGoal(this, net.minecraft.world.entity.player.Player.class, 6.0F));
        this.goalSelector.addGoal(5, new net.minecraft.world.entity.ai.goal.RandomLookAroundGoal(this));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return ${info.baseClass}.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 10.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.2D);
    }

    @Override
    public $className getBreedOffspring(net.minecraft.server.level.ServerLevel serverLevel, net.minecraft.world.entity.AgeableMob otherParent) {
        return new $className(ENTITY_TYPE, serverLevel);
    }
}"""
    }

    private fun generateMonsterEntityBody(className: String, info: EntityTypeInfo): String {
        return """public class $className extends ${info.baseClass} {
    public static final String ID = "${toSnakeCase(className)}";

    public $className(EntityType<? extends ${info.baseClass}> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new net.minecraft.world.entity.ai.goal.FloatGoal(this));
        this.goalSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new net.minecraft.world.entity.ai.goal.LookAtPlayerGoal(this, net.minecraft.world.entity.player.Player.class, 8.0F));
        this.goalSelector.addGoal(4, new net.minecraft.world.entity.ai.goal.RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(this, net.minecraft.world.entity.player.Player.class, true));
        this.targetSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal(this));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return ${info.baseClass}.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 20.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.3D)
            .add(Attributes.ATTACK_DAMAGE, 3.0D);
    }
}"""
    }

    private fun generateProjectileEntityBody(className: String, info: EntityTypeInfo): String {
        return """public class $className extends ${info.baseClass} {
    public static final String ID = "${toSnakeCase(className)}";

    public $className(EntityType<? extends ${info.baseClass}> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void onHit(net.minecraft.world.phys.HitResult hitResult) {
        super.onHit(hitResult);
        // Custom onCollision logic
    }

    protected void onCollision(net.minecraft.world.phys.HitResult hitResult) {
        onHit(hitResult);
    }
}"""
    }

    /**
     * Generate or update the common ModEntities registry using Architectury DeferredRegister.
     */
    private fun generateOrUpdateModEntities(
        projectDir: File,
        entityName: String,
        modId: String,
        sanitizedModId: String,
        type: String
    ) {
        val className = toClassName(entityName)
        val constantName = entityName.uppercase()
        val info = getEntityTypeInfo(type)
        val registryFile = File(projectDir, "shared/common/src/main/java/com/$sanitizedModId/registry/ModEntities.java")

        if (registryFile.exists()) {
            val existingContent = registryFile.readText()

            val newField = """    public static final RegistrySupplier<EntityType<$className>> $constantName = ENTITIES.register("$entityName", () ->
            EntityType.Builder.of($className::new, ${info.mobCategory}).sized(0.6f, 1.8f).clientTrackingRange(8).updateInterval(3).build());"""
            val newImport = "import com.$sanitizedModId.entities.$className;"

            val updatedContent = existingContent
                .replace("    public static void init()", "$newField\n\n    public static void init()")
                .let { content ->
                    if (!content.contains(newImport)) {
                        content.replace("import net.minecraft.world.entity.EntityType;", "import net.minecraft.world.entity.EntityType;\nimport com.$sanitizedModId.entities.$className;")
                    } else content
                }

            FileUtil.writeText(registryFile, updatedContent)
            Logger.info("  ✓ Added $className to registry/ModEntities.java")
        } else {
            val content = """
                package com.$sanitizedModId.registry;

                import com.$sanitizedModId.entities.$className;
                import dev.architectury.registry.registries.DeferredRegister;
                import dev.architectury.registry.registries.RegistrySupplier;
                import net.minecraft.core.registries.Registries;
                import net.minecraft.world.entity.EntityType;
                import net.minecraft.world.entity.MobCategory;

                public class ModEntities {
                    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create("$modId", Registries.ENTITY_TYPE);

                    public static final RegistrySupplier<EntityType<$className>> $constantName = ENTITIES.register("$entityName", () ->
                        EntityType.Builder.of($className::new, ${info.mobCategory}).sized(0.6f, 1.8f).clientTrackingRange(8).updateInterval(3).build());

                    public static void init() {
                        ENTITIES.register();
                    }
                }
            """.trimIndent()

            FileUtil.writeText(registryFile, content)
            Logger.info("  ✓ Created registry/ModEntities.java with $className")
        }
    }

    /**
     * Generate common renderer using Mojang mappings (Architectury Loom remaps for Fabric).
     */
    private fun generateCommonRenderer(projectDir: File, entityName: String, modId: String, sanitizedModId: String) {
        val className = toClassName(entityName)

        val content = """
            package com.$sanitizedModId.client.renderer;

            import com.$sanitizedModId.entities.$className;
            import com.$sanitizedModId.client.model.${className}Model;
            import net.minecraft.client.renderer.entity.EntityRendererProvider;
            import net.minecraft.client.renderer.entity.MobRenderer;
            import net.minecraft.client.model.geom.ModelLayers;
            import net.minecraft.resources.ResourceLocation;

            public class ${className}Renderer extends MobRenderer<$className, ${className}Model> {
                private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("$modId", "textures/entity/$entityName.png");

                public ${className}Renderer(EntityRendererProvider.Context context) {
                    super(context, new ${className}Model(context.bakeLayer(ModelLayers.PLAYER)), 0.5f);
                }

                @Override
                public ResourceLocation getTextureLocation($className entity) {
                    return TEXTURE;
                }
            }
        """.trimIndent()

        val file = File(projectDir, "shared/common/src/main/java/com/$sanitizedModId/client/renderer/${className}Renderer.java")
        FileUtil.writeText(file, content)

        Logger.info("  ✓ Created renderer: shared/common/src/main/java/com/$sanitizedModId/client/renderer/${className}Renderer.java")
    }

    /**
     * Generate common entity model using Mojang mappings.
     */
    private fun generateCommonEntityModel(projectDir: File, entityName: String, sanitizedModId: String) {
        val className = toClassName(entityName)

        val content = """
            package com.$sanitizedModId.client.model;

            import com.$sanitizedModId.entities.$className;
            import com.mojang.blaze3d.vertex.PoseStack;
            import com.mojang.blaze3d.vertex.VertexConsumer;
            import net.minecraft.client.model.EntityModel;
            import net.minecraft.client.model.geom.ModelPart;

            public class ${className}Model extends EntityModel<$className> {
                private final ModelPart root;

                public ${className}Model(ModelPart root) {
                    this.root = root;
                }

                @Override
                public void setupAnim($className entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
                    // Animate model parts here
                }

                @Override
                public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
                    root.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
                }
            }
        """.trimIndent()

        val file = File(projectDir, "shared/common/src/main/java/com/$sanitizedModId/client/model/${className}Model.java")
        FileUtil.writeText(file, content)

        Logger.info("  ✓ Created entity model: shared/common/src/main/java/com/$sanitizedModId/client/model/${className}Model.java")
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

            import net.minecraft.world.item.SpawnEggItem;
            import net.minecraft.world.item.Item;

            /**
             * Spawn egg for $className entity
             */
            public class ${className}SpawnEgg {
                public static final String ID = "${entityName}_spawn_egg";

                /**
                 * Primary egg color (background)
                 */
                public static final int PRIMARY_COLOR = 0x7E9680;

                /**
                 * Secondary egg color (spots)
                 */
                public static final int SECONDARY_COLOR = 0x7E7E7E;

                /**
                 * Create the spawn egg item instance.
                 * Call this from your loader-specific registration code,
                 * passing the registered EntityType.
                 *
                 * Example (Fabric):
                 *   SpawnEggItem egg = ${className}SpawnEgg.create(${className}Fabric.ENTITY_TYPE);
                 */
                public static SpawnEggItem create(net.minecraft.world.entity.EntityType<?> entityType) {
                    return new SpawnEggItem(entityType, PRIMARY_COLOR, SECONDARY_COLOR, new Item.Properties());
                }
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

        val displayName = entityName.split("_").joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }

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

    private fun toClassName(snakeCase: String): String = StringUtil.toClassName(snakeCase)

    private fun toSnakeCase(className: String): String = StringUtil.toSnakeCase(className)
}
