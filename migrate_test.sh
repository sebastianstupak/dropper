#!/bin/bash
# Migration script to wrap command.parse calls with context.withProjectDir

FILE="$1"

# Replace import
sed -i 's/import dev.dropper.generator.ProjectGenerator/import dev.dropper.util.TestProjectContext/' "$FILE"

# Replace field declaration
sed -i 's/private lateinit var testProjectDir: File/private lateinit var context: TestProjectContext/' "$FILE"
sed -i '/private val originalUserDir/d' "$FILE"

# Replace setup method body
sed -i 's/testProjectDir = File/context = TestProjectContext.create/' "$FILE"
sed -i 's/testProjectDir.mkdirs()//' "$FILE"
sed -i 's/val generator = ProjectGenerator()//' "$FILE"
sed -i 's/generator.generate(testProjectDir, config)/context.createProject(config)/' "$FILE"
sed -i '/System.setProperty("user.dir", testProjectDir.absolutePath)/d' "$FILE"

# Replace cleanup method body
sed -i '/System.setProperty("user.dir", originalUserDir)/d' "$FILE"
sed -i 's/if (testProjectDir.exists()) {/context.cleanup()/' "$FILE"
sed -i '/testProjectDir.deleteRecursively()/d' "$FILE"
sed -i '/^        }$/d' "$FILE"

# Replace all testProjectDir references with context.projectDir
sed -i 's/testProjectDir/context.projectDir/g' "$FILE"

echo "Migration complete for $FILE"
