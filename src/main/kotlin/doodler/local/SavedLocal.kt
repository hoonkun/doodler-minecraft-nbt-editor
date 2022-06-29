package doodler.local

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import doodler.application.structure.DoodlerEditorType
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File


private val LocalPath = System.getProperty("user.home").plus("/.local/share/doodler/kiwi_bird.json")

private val DefaultSavedLocal = SavedLocal()

private val initialSavedLocal = read()

var GlobalScale: Float = initialSavedLocal.globalScale
val UserSavedLocalState = SavedLocalState(initialSavedLocal)

private fun read(): SavedLocal {
    val file = File(LocalPath)
    if (!file.exists()) return DefaultSavedLocal

    val raw = file.readText()
    return try { Json.decodeFromString(raw) } catch (e: Exception) { DefaultSavedLocal }
}

private fun save(newSavedLocal: SavedLocal) {
    val file = File(LocalPath)
    if (!file.parentFile.exists()) file.parentFile.mkdirs()

    file.writeText(Json.encodeToString(newSavedLocal))
}

fun editSavedLocal(
    globalScale: Float? = null,
    recent: List<Recent>? = null,
    loaderStackSize: Int? = null
) {
    val prev = read()
    val prevGlobalScale = prev.globalScale
    val prevRecent = prev.recent
    val prevLoaderStackSize = prev.loaderStackSize

    save(
        SavedLocal(
            globalScale = globalScale ?: prevGlobalScale,
            recent = recent ?: prevRecent,
            loaderStackSize = loaderStackSize ?: prevLoaderStackSize
        )
    )
}

@Serializable
data class SavedLocal(
    val globalScale: Float = 1.0f,
    val recent: List<Recent> = emptyList(),
    val loaderStackSize: Int = 5
)

@Serializable
data class Recent(
    val type: DoodlerEditorType,
    val name: String,
    val path: String
)

class SavedLocalState(from: SavedLocal) {
    var recent = mutableStateListOf(*from.recent.toTypedArray())
    var loaderStackSize by mutableStateOf(from.loaderStackSize)

    fun save() {
        val local = read()
        save(local.copy(recent = recent, loaderStackSize = loaderStackSize))
    }
}
