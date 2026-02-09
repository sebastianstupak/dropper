package dev.dropper.commands

import com.github.ajalt.clikt.core.CliktCommand
import java.io.File

/**
 * Base class for all Dropper commands.
 * Provides configurable project directory for testability.
 */
abstract class DropperCommand(
    name: String,
    help: String = ""
) : CliktCommand(name = name, help = help) {

    /**
     * Project directory - can be set for testing.
     * Defaults to current working directory.
     */
    var projectDir: File = File(System.getProperty("user.dir"))

    /**
     * Get the project config file
     */
    protected fun getConfigFile(): File = File(projectDir, "config.yml")
}
