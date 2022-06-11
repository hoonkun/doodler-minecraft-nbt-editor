package doodler.minecraft.structures

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.graphics.ImageBitmap
import composable.editor.world.DirectoryHierarchyItem
import composable.editor.world.FileHierarchyItem
import composable.editor.world.HierarchyItem
import doodler.exceptions.DoodleException
import doodler.minecraft.DatWorker
import doodler.minecraft.WorldUtils
import doodler.nbt.tag.CompoundTag
import doodler.nbt.tag.DoubleTag
import doodler.nbt.tag.ListTag
import doodler.nbt.tag.StringTag
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

    fun listWorldFiles(dimension: WorldDimension): List<HierarchyItem> {
        val world = get(dimension)
        val list = listOf("data", "region", "poi", "entities").sorted()

        return list.mapNotNull {
            if (world[it].isNotEmpty()) {
                val files = world[it].map { file -> FileHierarchyItem(file, file.name, 3) }
                DirectoryHierarchyItem(files, it, 2)
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
    val namespaceId: String,
    val displayName: String
) {
    OVERWORLD("", "minecraft:overworld", "overworld"),
    NETHER("DIM-1", "minecraft:the_nether", "nether"),
    THE_END("DIM1", "minecraft:the_end", "the_end");

    companion object {
        operator fun get(pathName: String): WorldDimension = values().find { it.ident == pathName } ?: OVERWORLD

        fun fromMcaPath(path: String): WorldDimension {
            val segments = path.split("/").toMutableList()
            val pathName = segments.slice(0 until segments.size - 2).last()
            return WorldDimension[pathName]
        }

        fun namespace(namespaceId: String): WorldDimension = values().find { it.namespaceId == namespaceId }
            ?: throw DoodleException("Internal Error", null, "unknown dimension id: $namespaceId")
    }
}

data class CachedTerrainInfo(val yLimit: Short, val location: AnvilLocation)

class WorldSpecification (
    worldPath: String
) {
    val tree: WorldHierarchy = WorldUtils.load(worldPath)

    private var levelInfo = DatWorker.read(tree.level.readBytes())

    private val _name: String?
        get() {
            return levelInfo["Data"]
                ?.getAs<CompoundTag>()?.get("LevelName")
                ?.getAs<StringTag>()?.value
        }

    val name = _name!!

    val playerPos: Pair<String, BlockLocation>?
        get() {
            val player = levelInfo["Player"]?.getAs<CompoundTag>()
            val dimensionId = player?.get("Dimension")?.getAs<StringTag>()?.value

            val pos = player?.get("Pos")?.getAs<ListTag>()
            val x = pos?.get(0)?.getAs<DoubleTag>()?.value?.toInt()
            val z = pos?.get(2)?.getAs<DoubleTag>()?.value?.toInt()

            return if (x == null || z == null || dimensionId == null) null else dimensionId to BlockLocation(x, z)
        }

    fun reload() {
        levelInfo = DatWorker.read(tree.level.readBytes())
    }

}

sealed class WorldFileType

class McaFileType(val location: ChunkLocation): WorldFileType()

object DatFileType: WorldFileType()
