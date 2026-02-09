#!/usr/bin/env python3
"""
Wrap all command.parse calls with context.withProjectDir
"""

import sys
import re

def wrap_command_calls(content):
    """Wrap command.parse calls that aren't already wrapped"""
    lines = content.split('\n')
    result = []
    skip_until = -1

    for i, line in enumerate(lines):
        # Skip lines we've already processed
        if i < skip_until:
            continue

        # Check if this is a command declaration followed by parse
        if 'val command =' in line and 'Command()' in line:
            # Check if next line has parse
            if i + 1 < len(lines) and 'command.parse' in lines[i + 1]:
                # Check if already wrapped (look back 2 lines)
                if i >= 2 and 'withProjectDir' in lines[i - 1]:
                    result.append(line)
                    continue

                # Get indentation
                indent = len(line) - len(line.lstrip())
                indent_str = ' ' * indent

                # Add wrapper
                result.append(f"{indent_str}context.withProjectDir {{")
                result.append('    ' + line)
                result.append('    ' + lines[i + 1])
                result.append(f"{indent_str}}}")
                result.append("")
                skip_until = i + 2
                continue

        result.append(line)

    return '\n'.join(result)

def main():
    if len(sys.argv) != 2:
        print("Usage: python wrap_commands.py <file>")
        sys.exit(1)

    filename = sys.argv[1]

    with open(filename, 'r', encoding='utf-8') as f:
        content = f.read()

    content = wrap_command_calls(content)

    with open(filename, 'w', encoding='utf-8') as f:
        f.write(content)

    print(f"Wrapped commands in {filename}")

if __name__ == '__main__':
    main()
