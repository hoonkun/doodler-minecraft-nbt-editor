package doodler.extension

import java.io.File

fun File.safeListFiles(): List<File> {
    return this.let { if (!it.exists()) null else it.listFiles()?.toList() } ?: listOf()
}

fun fileOrNull(path: String) = File(path).let { if (it.exists()) it else null }
