package doodler.local

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import doodler.application.structure.DoodlerEditorType
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

private val DataPath = System.getProperty("user.home").plus("/.local/share/doodler/kiwi.json")

private val DefaultLocalData = LocalData(
    recent = emptyList()
)

private fun readLocalData(): LocalData {
    val file = File(DataPath)
    if (!file.exists()) return DefaultLocalData

    val raw = file.readText()
    return try { Json.decodeFromString(raw) } catch (e: Exception) { DefaultLocalData }
}

private fun saveLocalData(data: LocalData) {
    val file = File(DataPath)
    if (!file.parentFile.exists()) file.parentFile.mkdirs()

    file.writeText(Json.encodeToString(data))
}

@Stable
class LocalDataState {

    val recent = mutableStateListOf<RecentOpen>()

    init {

        val data = readLocalData()
        recent.addAll(data.recent)

    }

    fun save() {
        saveLocalData(LocalData(recent))
    }

}

@Serializable
data class LocalData(
    val recent: List<RecentOpen>
)

@Serializable
data class RecentOpen(
    val type: DoodlerEditorType,
    val name: String,
    val path: String
)
