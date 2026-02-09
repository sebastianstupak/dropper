#!/usr/bin/env python3
"""
Script to migrate SyncCommandE2ETest to use TestProjectContext
Wraps all command.parse() calls with context.withProjectDir { }
"""

import re

def wrap_command_parse(content):
    """Wrap standalone command.parse calls with context.withProjectDir"""
    lines = content.split('\n')
    result = []
    i = 0

    while i < len(lines):
        line = lines[i]

        # Check if this is a command.parse line that's NOT already wrapped
        if 'command.parse(' in line and 'withProjectDir' not in lines[max(0, i-5):i]:
            # Find the command definition
            j = i - 1
            while j >= 0 and 'val command' not in lines[j]:
                j -= 1

            if j >= 0 and 'val command' in lines[j]:
                # Wrap from command definition to parse call
                indent = len(lines[j]) - len(lines[j].lstrip())
                indent_str = ' ' * indent

                # Add wrapping
                result.append(f"{indent_str}context.withProjectDir {{")
                # Add command lines
                for k in range(j, i + 1):
                    result.append('    ' + lines[k])
                result.append(f"{indent_str}}}")
                result.append("")

                # Skip the lines we just added
                i += 1
                continue

        result.append(line)
        i += 1

    return '\n'.join(result)

def migrate_file(input_file, output_file):
    with open(input_file, 'r', encoding='utf-8') as f:
        content = f.read()

    # Apply transformation
    content = wrap_command_parse(content)

    with open(output_file, 'w', encoding='utf-8') as f:
        f.write(content)

    print(f"Migrated {input_file} -> {output_file}")

if __name__ == '__main__':
    import sys
    if len(sys.argv) != 3:
        print("Usage: python migrate_sync_test.py <input_file> <output_file>")
        sys.exit(1)

    migrate_file(sys.argv[1], sys.argv[2])
