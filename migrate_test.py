#!/usr/bin/env python3
"""
Script to migrate integration tests to use TestProjectContext pattern.
"""

import re
import sys

def migrate_test_file(content):
    """Migrate a test file to use TestProjectContext."""

    # Step 1: Add TestProjectContext import if not present
    if 'import dev.dropper.util.TestProjectContext' not in content:
        content = content.replace(
            'import org.junit.jupiter.api.AfterEach',
            'import dev.dropper.util.TestProjectContext\nimport org.junit.jupiter.api.AfterEach'
        )

    # Step 2: Replace testProjectDir declaration and originalUserDir
    content = re.sub(
        r'    private lateinit var testProjectDir: File\n    private val originalUserDir = System\.getProperty\("user\.dir"\)',
        '    private lateinit var context: TestProjectContext',
        content
    )

    # Step 3: Update setup() method - remove ProjectGenerator usage
    # This is complex, so we'll do pattern matching
    setup_pattern = r'@BeforeEach\s+fun setup\(\) \{[^}]+\}'
    def replace_setup(match):
        return """@BeforeEach
    fun setup() {
        // Create a test project context
        context = TestProjectContext.create("test-project")

        // Generate a minimal project using context
        context.createDefaultProject(
            id = "testmod",
            name = "Test Mod",
            minecraftVersions = listOf("1.20.1"),
            loaders = listOf("fabric", "forge")
        )
    }"""

    # Step 4: Update cleanup() method
    cleanup_pattern = r'@AfterEach\s+fun cleanup\(\) \{[^}]+\}'
    def replace_cleanup(match):
        return """@AfterEach
    fun cleanup() {
        context.cleanup()
    }"""

    content = re.sub(cleanup_pattern, replace_cleanup, content, flags=re.DOTALL)

    # Step 5: Replace all testProjectDir with context.projectDir
    content = content.replace('testProjectDir', 'context.projectDir')

    # Step 6: Fix command calls - add projectDir before parse()
    # Pattern: CreateItemCommand().parse(
    content = re.sub(
        r'CreateItemCommand\(\)\.parse\(',
        'CreateItemCommand().apply { projectDir = context.projectDir }.parse(',
        content
    )

    content = re.sub(
        r'CreateBlockCommand\(\)\.parse\(',
        'CreateBlockCommand().apply { projectDir = context.projectDir }.parse(',
        content
    )

    # Pattern for list commands
    for cmd in ['ListItemsCommand', 'ListBlocksCommand', 'ListEntitiesCommand',
                'ListRecipesCommand', 'ListAllCommand']:
        content = re.sub(
            rf'{cmd}\(\)\.parse\(',
            f'{cmd}().apply {{ projectDir = context.projectDir }}.parse(',
            content
        )

    return content

if __name__ == '__main__':
    if len(sys.argv) != 2:
        print("Usage: python migrate_test.py <test_file.kt>")
        sys.exit(1)

    file_path = sys.argv[1]

    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    migrated = migrate_test_file(content)

    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(migrated)

    print(f"✓ Migrated {file_path}")
