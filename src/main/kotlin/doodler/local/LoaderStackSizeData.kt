package doodler.local

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File


private val LoaderStackPath = System.getProperty("user.home").plus("/.local/share/doodler/loader_stacks.json")

private val DefaultLoaderStackSize = LoaderStackSizeData()

var LoaderStackSize by mutableStateOf(readLoaderStackSize())

fun readLoaderStackSize(): LoaderStackSizeData {
    val file = File(LoaderStackPath)
    if (!file.exists()) {
        file.parentFile.mkdirs()
        file.writeText(Json.encodeToString(DefaultLoaderStackSize))
        return DefaultLoaderStackSize
    }

    val raw = file.readText()
    return try { Json.decodeFromString(raw) } catch (e: Exception) { DefaultLoaderStackSize }
}

fun saveLoaderStackSize() {
    val file = File(LoaderStackPath)
    if (!file.exists()) file.parentFile.mkdirs()

    file.writeText(Json.encodeToString(LoaderStackSize))
}

@Serializable
data class LoaderStackSizeData(
    val value: Int = 5
)
