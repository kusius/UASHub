package io.github.kusius.klvmp

import io.kusius.klvmp.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.InternalResourceApi
import java.lang.Exception
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.createFile
import kotlin.io.path.deleteIfExists
import kotlin.io.path.isDirectory
import kotlin.io.path.isWritable
import kotlin.io.path.name
import kotlin.io.path.pathString
import kotlin.io.path.writeBytes


private fun getLibraryExtension(): String {
    val os = System.getProperty("os.name").lowercase()
    return when {
        os.contains("win") -> {
            "dll"
        }

        os.contains("nix") || os.contains("nux") || os.contains("aix") -> {
            "so"
        }

        os.contains("mac") -> {
            "dylib"
        }

        else -> "unknown"
    }
}

/**
 *  Extract them from the resource as binary files write them
 *  in current user.dir and invoke loadlibrary to load them
 *
 */
@OptIn(ExperimentalResourceApi::class, InternalResourceApi::class)
private suspend fun createAndLoadLibraryFileFromResource(libName: String): Boolean {
    val libraryFileName = "lib$libName.${getLibraryExtension()}"
    val resourceBytes = Res.readBytes("files/$libraryFileName")
    val currentDir = System.getProperty("java.io.tmpdir")
    val path = Path("$currentDir/$libraryFileName")

    // Write them back on the java temp dir and load the from there
    if(!path.isDirectory() && path.isWritable()) {
        val deleted = path.deleteIfExists()
        if(!deleted) {
            println("Could not delete ${path.name}, assuming it was not there")
        }

        val createdPath = path.createFile()
        createdPath.writeBytes(resourceBytes)
        try {
            System.load(createdPath.pathString)
            println("Loaded library ${createdPath.absolutePathString()}")
            return true
        } catch (e: Exception) {
            println("Failed to load library${createdPath.absolutePathString()}")
        }
    }

    return false

}

@OptIn(ExperimentalResourceApi::class)
@Suppress("UnsafeDynamicallyLoadedCode")
actual suspend fun loadNativeLibs() {
    createAndLoadLibraryFileFromResource("klv")
    createAndLoadLibraryFileFromResource("tsdemux")
}