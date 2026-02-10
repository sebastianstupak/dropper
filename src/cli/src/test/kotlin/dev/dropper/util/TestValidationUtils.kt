package dev.dropper.util

import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Shared test validation utilities for verifying generated code quality.
 *
 * Provides real parsing-based validation instead of simple substring checks.
 * All methods throw AssertionError on failure with descriptive messages.
 */
object TestValidationUtils {

    // ========================================================================
    // JSON Validation
    // ========================================================================

    /**
     * Parse JSON using Gson and verify it is valid.
     * Throws AssertionError on invalid JSON with details about the parse error.
     *
     * @param content The JSON string to validate
     * @param context Optional context string for error messages (e.g., file name)
     */
    fun assertValidJson(content: String, context: String = "") {
        val ctx = if (context.isNotEmpty()) " ($context)" else ""
        val trimmed = content.trim()

        assertTrue(trimmed.isNotEmpty(), "JSON content should not be empty$ctx")
        assertTrue(
            trimmed.startsWith("{") || trimmed.startsWith("["),
            "JSON should start with '{' or '['$ctx, but starts with '${trimmed.firstOrNull()}'$ctx"
        )

        try {
            JsonParser.parseString(content)
        } catch (e: JsonSyntaxException) {
            fail("Invalid JSON$ctx: ${e.message}\nContent:\n${content.take(500)}")
        }

        // Additional check: no trailing commas (Gson is lenient by default on some cases)
        assertNoTrailingCommas(content, context)

        // Check no template variables remain
        assertNoTemplateVariables(content, context)
    }

    /**
     * Verify JSON contains all required top-level keys.
     *
     * @param content The JSON string
     * @param keys List of required key names
     * @param context Optional context for error messages
     */
    fun assertJsonHasKeys(content: String, keys: List<String>, context: String = "") {
        val ctx = if (context.isNotEmpty()) " ($context)" else ""

        val jsonElement = try {
            JsonParser.parseString(content)
        } catch (e: JsonSyntaxException) {
            fail("Cannot check keys - invalid JSON$ctx: ${e.message}")
            return
        }

        assertTrue(jsonElement.isJsonObject, "JSON should be an object to check keys$ctx")
        val jsonObject = jsonElement.asJsonObject

        keys.forEach { key ->
            assertTrue(
                jsonObject.has(key),
                "JSON should contain key '$key'$ctx. Found keys: ${jsonObject.keySet()}"
            )
        }
    }

    /**
     * Check for trailing commas which are invalid in JSON but may not
     * be caught by lenient parsers.
     */
    private fun assertNoTrailingCommas(content: String, context: String = "") {
        val ctx = if (context.isNotEmpty()) " ($context)" else ""
        // Pattern: comma followed by optional whitespace then closing brace/bracket
        val trailingCommaPattern = Regex(",\\s*[}\\]]")
        val match = trailingCommaPattern.find(content)
        if (match != null) {
            val lineNumber = content.substring(0, match.range.first).count { it == '\n' } + 1
            fail("Trailing comma found at line $lineNumber$ctx: '${match.value}'")
        }
    }

    // ========================================================================
    // Java Syntax Validation
    // ========================================================================

    /**
     * Verify Java source has valid syntax structure.
     *
     * Checks:
     * - Balanced braces, parentheses, brackets
     * - Valid package declaration
     * - At least one class or interface declaration
     * - Valid class name (Java identifier)
     * - No unresolved template variables
     * - Syntactically valid imports
     * - Semicolons after import/package statements
     *
     * @param content The Java source code
     * @param context Optional context for error messages
     */
    fun assertValidJavaSyntax(content: String, context: String = "") {
        val ctx = if (context.isNotEmpty()) " ($context)" else ""

        assertTrue(content.isNotBlank(), "Java source should not be blank$ctx")

        // Check no template variables
        assertNoTemplateVariables(content, context)

        // Check balanced delimiters (ignoring those in string literals and comments)
        assertBalancedDelimiters(content, context)

        // Check package declaration
        assertValidPackageDeclaration(content, context)

        // Check class or interface declaration exists
        assertHasClassOrInterface(content, context)

        // Check imports are syntactically valid
        assertValidImports(content, context)
    }

    /**
     * Verify balanced braces, parentheses, and brackets.
     * Skips characters inside string literals and comments.
     */
    private fun assertBalancedDelimiters(content: String, context: String = "") {
        val ctx = if (context.isNotEmpty()) " ($context)" else ""
        val stack = mutableListOf<Char>()
        var i = 0
        val len = content.length

        while (i < len) {
            val c = content[i]

            // Skip string literals
            if (c == '"') {
                i++
                while (i < len && content[i] != '"') {
                    if (content[i] == '\\') i++ // skip escaped char
                    i++
                }
                i++ // skip closing quote
                continue
            }

            // Skip char literals
            if (c == '\'') {
                i++
                while (i < len && content[i] != '\'') {
                    if (content[i] == '\\') i++
                    i++
                }
                i++
                continue
            }

            // Skip line comments
            if (c == '/' && i + 1 < len && content[i + 1] == '/') {
                while (i < len && content[i] != '\n') i++
                i++
                continue
            }

            // Skip block comments
            if (c == '/' && i + 1 < len && content[i + 1] == '*') {
                i += 2
                while (i + 1 < len && !(content[i] == '*' && content[i + 1] == '/')) i++
                i += 2
                continue
            }

            when (c) {
                '{', '(', '[' -> stack.add(c)
                '}' -> {
                    if (stack.isEmpty() || stack.removeLast() != '{') {
                        fail("Unmatched '}' at position $i$ctx")
                    }
                }
                ')' -> {
                    if (stack.isEmpty() || stack.removeLast() != '(') {
                        fail("Unmatched ')' at position $i$ctx")
                    }
                }
                ']' -> {
                    if (stack.isEmpty() || stack.removeLast() != '[') {
                        fail("Unmatched ']' at position $i$ctx")
                    }
                }
            }
            i++
        }

        if (stack.isNotEmpty()) {
            val unclosed = stack.joinToString(", ") { "'$it'" }
            fail("Unclosed delimiters: $unclosed$ctx")
        }
    }

    /**
     * Verify the package declaration is present and syntactically valid.
     */
    private fun assertValidPackageDeclaration(content: String, context: String = "") {
        val ctx = if (context.isNotEmpty()) " ($context)" else ""
        val packagePattern = Regex("""^\s*package\s+([a-zA-Z_]\w*(?:\.[a-zA-Z_]\w*)*)\s*;""", RegexOption.MULTILINE)
        val match = packagePattern.find(content)

        assertTrue(match != null, "Java source should have a valid package declaration$ctx")

        val packageName = match!!.groupValues[1]
        assertTrue(
            packageName.isNotBlank(),
            "Package name should not be blank$ctx"
        )

        // Verify no Java reserved words are used as package components
        val reservedWords = setOf(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
            "class", "const", "continue", "default", "do", "double", "else", "enum",
            "extends", "final", "finally", "float", "for", "goto", "if", "implements",
            "import", "instanceof", "int", "interface", "long", "native", "new",
            "package", "private", "protected", "public", "return", "short", "static",
            "strictfp", "super", "switch", "synchronized", "this", "throw", "throws",
            "transient", "try", "void", "volatile", "while"
        )
        packageName.split(".").forEach { component ->
            assertTrue(
                component !in reservedWords,
                "Package component '$component' is a reserved word$ctx"
            )
        }
    }

    /**
     * Verify at least one class or interface declaration exists.
     */
    private fun assertHasClassOrInterface(content: String, context: String = "") {
        val ctx = if (context.isNotEmpty()) " ($context)" else ""
        val classPattern = Regex("""(public\s+|abstract\s+|final\s+)*class\s+[A-Z]\w*""")
        val interfacePattern = Regex("""(public\s+)?interface\s+[A-Z]\w*""")
        val enumPattern = Regex("""(public\s+)?enum\s+[A-Z]\w*""")

        assertTrue(
            classPattern.containsMatchIn(content) ||
                interfacePattern.containsMatchIn(content) ||
                enumPattern.containsMatchIn(content),
            "Java source should contain at least one class, interface, or enum declaration$ctx"
        )
    }

    /**
     * Verify import statements are syntactically valid.
     */
    private fun assertValidImports(content: String, context: String = "") {
        val ctx = if (context.isNotEmpty()) " ($context)" else ""
        val importLines = content.lines().filter { it.trim().startsWith("import ") }

        importLines.forEach { line ->
            val trimmed = line.trim()
            assertTrue(
                trimmed.endsWith(";"),
                "Import statement should end with semicolon$ctx: '$trimmed'"
            )
            // Check import path pattern: import [static] qualified.name[.*];
            val importPattern = Regex("""import\s+(static\s+)?([a-zA-Z_]\w*\.)+[a-zA-Z_*]\w*\s*;""")
            assertTrue(
                importPattern.matches(trimmed),
                "Import statement should be syntactically valid$ctx: '$trimmed'"
            )
        }
    }

    // ========================================================================
    // Package / File Path Validation
    // ========================================================================

    /**
     * Verify the package declaration in Java source matches the file path.
     *
     * @param content The Java source code
     * @param filePath The absolute or relative file path
     * @param projectRoot The project root path (to make path relative)
     */
    fun assertPackageMatchesPath(content: String, filePath: String, projectRoot: String) {
        val packagePattern = Regex("""package\s+([\w.]+)\s*;""")
        val match = packagePattern.find(content)
            ?: fail("No package declaration found in file: $filePath")

        val declaredPackage = match.groupValues[1]

        // Convert package to path: com.testmod.items -> com/testmod/items
        val expectedPathPart = declaredPackage.replace(".", "/")

        // Normalize path separators
        val normalizedFilePath = filePath.replace("\\", "/")

        assertTrue(
            normalizedFilePath.contains(expectedPathPart),
            "Package '$declaredPackage' should match file path. " +
                "Expected path to contain '$expectedPathPart', but path was '$normalizedFilePath'"
        )
    }

    /**
     * Verify the Java class name matches the file name (without extension).
     *
     * @param content The Java source code
     * @param fileName The file name (e.g., "MyClass.java")
     */
    fun assertClassNameMatchesFile(content: String, fileName: String) {
        val expectedClassName = fileName.removeSuffix(".java")
        assertTrue(
            expectedClassName.isNotEmpty(),
            "File name should have .java extension: $fileName"
        )

        // Look for class, interface, or enum declaration with the expected name
        val classPattern = Regex("""(public\s+|abstract\s+|final\s+)*class\s+($expectedClassName)\b""")
        val interfacePattern = Regex("""(public\s+)?interface\s+($expectedClassName)\b""")
        val enumPattern = Regex("""(public\s+)?enum\s+($expectedClassName)\b""")
        assertTrue(
            classPattern.containsMatchIn(content) ||
                interfacePattern.containsMatchIn(content) ||
                enumPattern.containsMatchIn(content),
            "File '$fileName' should contain class, interface, or enum '$expectedClassName'"
        )
    }

    // ========================================================================
    // Template Variable Checks
    // ========================================================================

    /**
     * Verify no Mustache template variables remain ({{ or }}).
     *
     * @param content The content to check
     * @param context Optional context for error messages
     */
    fun assertNoTemplateVariables(content: String, context: String = "") {
        val ctx = if (context.isNotEmpty()) " ($context)" else ""

        // Direct check for {{ and }}
        if (content.contains("{{")) {
            val lineNum = content.substring(0, content.indexOf("{{")).count { it == '\n' } + 1
            val variables = Regex("\\{\\{([^}]*)}}").findAll(content).map { it.value }.toList()
            fail("Unresolved template variables found at line $lineNum$ctx: $variables")
        }

        if (content.contains("}}")) {
            // Only flag if there's a standalone }} not part of a valid construct
            // Check it's not inside a string literal by doing a simple heuristic
            val idx = content.indexOf("}}")
            val lineNum = content.substring(0, idx).count { it == '\n' } + 1
            fail("Unresolved template closing '}}' found at line $lineNum$ctx")
        }
    }

    // ========================================================================
    // Composite Validation Helpers
    // ========================================================================

    /**
     * Validate all JSON files in a directory tree.
     * Returns the count of validated files.
     */
    fun validateAllJsonFiles(rootDir: java.io.File): Int {
        val jsonFiles = rootDir.walkTopDown()
            .filter { it.isFile && it.extension == "json" }
            .toList()

        jsonFiles.forEach { file ->
            val content = file.readText()
            assertValidJson(content, file.name)
        }

        return jsonFiles.size
    }

    /**
     * Validate all Java files in a directory tree.
     * Returns the count of validated files.
     */
    fun validateAllJavaFiles(rootDir: java.io.File): Int {
        val javaFiles = rootDir.walkTopDown()
            .filter { it.isFile && it.extension == "java" }
            .toList()

        javaFiles.forEach { file ->
            val content = file.readText()
            assertValidJavaSyntax(content, file.name)
            assertClassNameMatchesFile(content, file.name)
        }

        return javaFiles.size
    }
}
