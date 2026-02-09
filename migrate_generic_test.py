#!/usr/bin/env python3
"""
Generic test migration script - updates imports, fields, setup/cleanup methods
"""

import sys
import re

def migrate_test(content, test_name):
    """Generic migration for test files"""

    # 1. Update imports - add TestProjectContext, remove ProjectGenerator
    if 'import dev.dropper.generator.ProjectGenerator' in content:
        content = content.replace(
            'import dev.dropper.generator.ProjectGenerator',
            'import dev.dropper.util.TestProjectContext\nimport dev.dropper.generator.ProjectGenerator'
        )
    else:
        # Add after config import
        content = content.replace(
            'import dev.dropper.config.ModConfig',
            'import dev.dropper.config.ModConfig\nimport dev.dropper.util.TestProjectContext'
        )

    # 2. Update field declarations
    content = re.sub(
        r'private lateinit var testProjectDir: File\s*\n\s*private val originalUserDir = System.getProperty\("user.dir"\)',
        'private lateinit var context: TestProjectContext',
        content
    )

    # Also handle cases without originalUserDir
    content = re.sub(
        r'private lateinit var testProjectDir: File',
        'private lateinit var context: TestProjectContext',
        content
    )

    # 3. Update setup method - handle both patterns
    # Pattern 1: with System.setProperty at end
    setup_pattern1 = re.compile(
        r'(@BeforeEach\s+fun setup\(\) \{[^}]*)'
        r'testProjectDir = File\([^)]+\)\s*\n\s*testProjectDir\.mkdirs\(\)'
        r'([^}]*)'
        r'(ProjectGenerator\(\)\.generate\(testProjectDir, config\)|generator\.generate\(testProjectDir, config\))'
        r'([^}]*)'
        r'System\.setProperty\("user\.dir", testProjectDir\.absolutePath\)'
        r'([^}]*\})',
        re.DOTALL
    )

    def replace_setup(match):
        before = match.group(1)
        middle1 = match.group(2)
        middle2 = match.group(4)
        after = match.group(5)

        # Extract test name from path
        test_subname = test_name.replace('E2ETest', '').replace('Test', '').lower()

        return (f'{before}context = TestProjectContext.create("test-{test_subname}")\n'
                f'{middle1}'
                f'context.createProject(config)'
                f'{middle2}'
                f'{after}')

    content = setup_pattern1.sub(replace_setup, content)

    # Pattern 2: simpler pattern without System.setProperty
    content = re.sub(
        r'testProjectDir = File\("build/test-[^"]+/\$\{System\.currentTimeMillis\(\)\}/[^"]+"\)\s*\n\s*testProjectDir\.mkdirs\(\)',
        lambda m: f'context = TestProjectContext.create("test-{test_name.lower()}")',
        content
    )

    # Replace ProjectGenerator().generate calls
    content = re.sub(
        r'(val generator = )?ProjectGenerator\(\)\.generate\(testProjectDir, config\)',
        'context.createProject(config)',
        content
    )
    content = re.sub(
        r'generator\.generate\(testProjectDir, config\)',
        'context.createProject(config)',
        content
    )

    # Remove System.setProperty("user.dir") lines
    content = re.sub(
        r'\s*System\.setProperty\("user\.dir", testProjectDir\.absolutePath\)\n',
        '',
        content
    )

    # 4. Update cleanup method
    content = re.sub(
        r'@AfterEach\s+fun cleanup\(\) \{\s*\n\s*System\.setProperty\("user\.dir", originalUserDir\)\s*\n\s*if \(testProjectDir\.exists\(\)\) \{\s*\n\s*testProjectDir\.deleteRecursively\(\)\s*\n\s*\}\s*\n\s*\}',
        '@AfterEach\n    fun cleanup() {\n        context.cleanup()\n    }',
        content,
        flags=re.DOTALL
    )

    # Simpler cleanup pattern
    content = re.sub(
        r'@AfterEach\s+fun cleanup\(\) \{[^}]*testProjectDir\.deleteRecursively\(\)[^}]*\}',
        '@AfterEach\n    fun cleanup() {\n        context.cleanup()\n    }',
        content,
        flags=re.DOTALL
    )

    # 5. Replace testProjectDir with context.projectDir
    content = content.replace('testProjectDir', 'context.projectDir')

    # 6. Remove originalUserDir references
    content = re.sub(r'private val originalUserDir[^\n]*\n', '', content)
    content = re.sub(r'\s*val originalUserDir[^\n]*\n', '', content)

    return content

def main():
    if len(sys.argv) != 2:
        print("Usage: python migrate_generic_test.py <test_file>")
        sys.exit(1)

    filename = sys.argv[1]

    # Extract test name from filename
    import os
    test_name = os.path.basename(filename).replace('.kt', '')

    with open(filename, 'r', encoding='utf-8') as f:
        content = f.read()

    content = migrate_test(content, test_name)

    with open(filename, 'w', encoding='utf-8') as f:
        f.write(content)

    print(f"Migrated {filename}")

if __name__ == '__main__':
    main()
