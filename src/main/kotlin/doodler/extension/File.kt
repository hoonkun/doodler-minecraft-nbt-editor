package doodler.extension

import java.io.File

fun File.safeListFiles(): List<File> {
    return this.let { if (!it.exists()) null else it.listFiles()?.toList() } ?: listOf()
}