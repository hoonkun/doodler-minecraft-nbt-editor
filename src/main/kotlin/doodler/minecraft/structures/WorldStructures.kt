package doodler.minecraft.structures

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.graphics.ImageBitmap
import composables.editor.world.DirectoryItem
import composables.editor.world.FileItem
import composables.editor.world.WorldTreeItem
import doodler.doodle.DoodleException
import java.io.File

class WorldHierarchy (
    val icon: File,
    val level: File,
    val advancements: List<File>,
    val stats: List<File>,
    val players: List<File>,
    private val overworld: WorldDimensionHierarchy,
    private val nether: WorldDimensionHierarchy,
    private val end: WorldDimensionHierarchy
) {
    operator fun get(key: WorldDimension): WorldDimensionHierarchy {
        return when (key) {
            WorldDimension.OVERWORLD -> overworld
            WorldDimension.THE_END -> end
            WorldDimension.NETHER -> nether
        }
    }

    fun listWorldFiles(dimension: WorldDimension): List<WorldTreeItem> {
        val world = get(dimension)
        val list = listOf("data", "region", "poi", "entities").sorted()

        return list.mapNotNull {
            if (world[it].isNotEmpty()) {
                val files = world[it].map { file -> FileItem(file.name, 3, file) }
                DirectoryItem(it, 2, files)
            } else null
        }
    }
}

class WorldDimensionHierarchy (
    private val region: List<File>,
    private val entities: List<File>,
    private val poi: List<File>,
    val data: List<File>,
) {

    val cachedTerrains: SnapshotStateMap<CachedTerrainInfo, ImageBitmap> = mutableStateMapOf()
    val cachedValidY: MutableMap<AnvilLocation, List<IntRange>> = mutableMapOf()

    operator fun get(key: String): List<File> {
        return when (key) {
            "entities" -> entities
            "region" -> region
            "poi" -> poi
            "data" -> data
            else -> listOf()
        }
    }
}

enum class WorldDimension(
    val ident: String,
    val namespaceId: String
) {
    OVERWORLD("", "minecraft:overworld"),
    NETHER("DIM-1", "minecraft:the_nether"),
    THE_END("DIM1", "minecraft:the_end");

    companion object {
        operator fun get(pathName: String): WorldDimension = values().find { it.ident == pathName } ?: OVERWORLD
        fun namespace(namespaceId: String): WorldDimension = values().find { it.namespaceId == namespaceId }
            ?: throw DoodleException("Internal Error", null, "unknown dimension id: $namespaceId")
    }
}

data class CachedTerrainInfo(val yLimit: Short, val location: AnvilLocation)

class WorldSpecification (
    tree: MutableState<WorldHierarchy?> = mutableStateOf(null),
    name: MutableState<String?> = mutableStateOf(null)
) {
    var tree: WorldHierarchy? by tree
    val requireTree get() = tree!!

    var name by name
    val requireName get() = name!!
}