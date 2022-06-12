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
            WorldDimension.Overworld -> overworld
            WorldDimension.TheEnd -> end
            WorldDimension.Nether -> nether
        }
    }

    fun listWorldFiles(dimension: WorldDimension): List<HierarchyItem> {
        val world = get(dimension)
        val list = listOf("data", "region", "poi", "entities").sorted()

        return list.mapNotNull {
            if (world[it].isNotEmpty()) {
                val files = world[it]
                    .sortedBy { file -> file.name }
                    .map { file -> FileHierarchyItem(file, file.name, 3) }
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

    @Deprecated("this properties are moved to ")
    val cachedTerrains: SnapshotStateMap<CachedTerrainInfo, ImageBitmap> = mutableStateMapOf()
    @Deprecated("this properties are moved to ")
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

    operator fun get(type: McaType): List<File> = get(type.pathName)
}

enum class WorldDimension(
    val ident: String,
    val namespaceId: String,
    val displayName: String
) {
    Overworld("", "minecraft:overworld", "overworld"),
    Nether("DIM-1", "minecraft:the_nether", "nether"),
    TheEnd("DIM1", "minecraft:the_end", "the_end");

    companion object {
        operator fun get(pathName: String): WorldDimension = values().find { it.ident == pathName } ?: Overworld

        fun fromMcaPath(path: String): WorldDimension {
            val segments = path.split("/").toMutableList()
            val pathName = segments.slice(0 until segments.size - 2).last()
            return WorldDimension[pathName]
        }

        fun namespace(namespaceId: String): WorldDimension = values().find { it.namespaceId == namespaceId }
            ?: throw DoodleException("Internal Error", null, "unknown dimension id: $namespaceId")
    }
}

@Deprecated("deprecated.")
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

    val playerPos: Pair<WorldDimension, BlockLocation>?
        get() {
            val player = levelInfo["Data"]?.getAs<CompoundTag>()?.get("Player")?.getAs<CompoundTag>()
            val dimensionId = player?.get("Dimension")?.getAs<StringTag>()?.value

            val pos = player?.get("Pos")?.getAs<ListTag>()
            val x = pos?.get(0)?.getAs<DoubleTag>()?.value?.toInt()
            val z = pos?.get(2)?.getAs<DoubleTag>()?.value?.toInt()

            return if (x == null || z == null || dimensionId == null) null
                else WorldDimension.namespace(dimensionId) to BlockLocation(x, z)
        }

    fun reload() {
        levelInfo = DatWorker.read(tree.level.readBytes())
    }

}

sealed class WorldFileType

class McaFileType(val location: ChunkLocation): WorldFileType()

object DatFileType: WorldFileType()
