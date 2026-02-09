#!/usr/bin/env python3
"""
Migrate integration tests to use TestProjectContext
"""

import sys
import re

def migrate_sync_command_test(content):
    """Migrate SyncCommandE2ETest"""

    # 1. Update imports
    content = content.replace(
        'import dev.dropper.generator.ProjectGenerator',
        'import dev.dropper.util.TestProjectContext'
    )

    # 2. Update field declarations
    content = content.replace(
        'private lateinit var testProjectDir: File\n    private val originalUserDir = System.getProperty("user.dir")',
        'private lateinit var context: TestProjectContext'
    )

    # 3. Update setup method
    old_setup = """    @BeforeEach
    fun setup() {
        // Create a test project with multiple asset packs
        testProjectDir = File("build/test-sync/${System.currentTimeMillis()}/test-mod")
        testProjectDir.mkdirs()

        // Generate project with asset packs
        val config = ModConfig(
            id = "testsync",
            name = "Test Sync Mod",
            version = "1.0.0",
            description = "Test mod for sync commands",
            author = "Test",
            license = "MIT",
            minecraftVersions = listOf("1.20.1", "1.21.1"),
            loaders = listOf("fabric", "forge", "neoforge")
        )

        val generator = ProjectGenerator()
        generator.generate(testProjectDir, config)

        // Create v2 asset pack
        createAssetPack("v2")

        // Change working directory
        System.setProperty("user.dir", testProjectDir.absolutePath)
    }"""

    new_setup = """    @BeforeEach
    fun setup() {
        context = TestProjectContext.create("test-sync")

        // Generate project with asset packs
        val config = ModConfig(
            id = "testsync",
            name = "Test Sync Mod",
            version = "1.0.0",
            description = "Test mod for sync commands",
            author = "Test",
            license = "MIT",
            minecraftVersions = listOf("1.20.1", "1.21.1"),
            loaders = listOf("fabric", "forge", "neoforge")
        )

        context.createProject(config)

        // Create v2 asset pack
        createAssetPack("v2")
    }"""

    content = content.replace(old_setup, new_setup)

    # 4. Update cleanup method
    old_cleanup = """    @AfterEach
    fun cleanup() {
        System.setProperty("user.dir", originalUserDir)
        if (testProjectDir.exists()) {
            testProjectDir.deleteRecursively()
        }
    }"""

    new_cleanup = """    @AfterEach
    fun cleanup() {
        context.cleanup()
    }"""

    content = content.replace(old_cleanup, new_cleanup)

    # 5. Replace all testProjectDir with context.projectDir
    content = content.replace('testProjectDir', 'context.projectDir')

    # 6. Wrap command.parse calls with context.withProjectDir
    # Pattern: find standalone command.parse that isn't already wrapped
    lines = content.split('\n')
    result = []
    i = 0
    while i < len(lines):
        line = lines[i]

        # Check if this line has command.parse and next 5 lines don't have withProjectDir
        if ('Command().parse' in line or 'Command().parse' in line) and i > 0:
            # Look back to see if already wrapped
            lookback = '\n'.join(lines[max(0, i-3):i])
            if 'withProjectDir' not in lookback:
                # Find the command declaration line
                cmd_line_idx = i - 1
                while cmd_line_idx >= 0 and 'val command' not in lines[cmd_line_idx]:
                    cmd_line_idx -= 1

                if cmd_line_idx >= 0:
                    # Get indent
                    indent = len(lines[cmd_line_idx]) - len(lines[cmd_line_idx].lstrip())
                    indent_str = ' ' * indent

                    # Add wrapper before command
                    result.append(f"{indent_str}context.withProjectDir {{")
                    # Add command and parse lines with extra indent
                    result.append('    ' + lines[cmd_line_idx])
                    result.append('    ' + line)
                    result.append(f"{indent_str}}}")
                    result.append("")
                    i += 1
                    continue

        result.append(line)
        i += 1

    return '\n'.join(result)

def main():
    if len(sys.argv) != 2:
        print("Usage: python migrate_tests.py <test_file>")
        sys.exit(1)

    filename = sys.argv[1]

    with open(filename, 'r', encoding='utf-8') as f:
        content = f.read()

    # Detect which test and apply appropriate migration
    if 'SyncCommandE2ETest' in content:
        content = migrate_sync_command_test(content)

    with open(filename, 'w', encoding='utf-8') as f:
        f.write(content)

    print(f"Migrated {filename}")

if __name__ == '__main__':
    main()
