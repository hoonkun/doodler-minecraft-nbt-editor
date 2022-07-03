package doodler.minecraft.structures

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import composable.editor.world.DirectoryHierarchyItem
import composable.editor.world.FileHierarchyItem
import composable.editor.world.HierarchyItem
import doodler.exceptions.DoodleException
import doodler.minecraft.DatWorker
import doodler.minecraft.MinecraftUserProfile
import doodler.minecraft.WorldUtils
import doodler.nbt.tag.CompoundTag
import doodler.nbt.tag.DoubleTag
import doodler.nbt.tag.ListTag
import doodler.nbt.tag.StringTag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

@Immutable
class WorldHierarchy(
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

@Immutable
class WorldDimensionHierarchy(
    private val region: List<File>,
    private val entities: List<File>,
    private val poi: List<File>,
    val data: List<File>,
) {
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

@Stable
class WorldSpecification (
    worldPath: String
) {

    companion object {
        val NetworkScope = CoroutineScope(Dispatchers.IO)
    }

    val type: WorldType = type(worldPath)

    val tree: WorldHierarchy =
        when (type){
          WorldType.Vanilla -> WorldUtils.loadVanilla(worldPath)
          WorldType.SpigotServer -> WorldUtils.loadServer(worldPath)
        }

    private var levelInfo by mutableStateOf(DatWorker.read(tree.level.readBytes()))

    val name: String by derivedStateOf {
        levelInfo["Data"]
            ?.getAs<CompoundTag>()?.get("LevelName")
            ?.getAs<StringTag>()?.value
            ?: "[LOADING WORLD NAME]"
    }

    val playerPos: Pair<WorldDimension, BlockLocation>? by derivedStateOf {
        val player = levelInfo["Data"]?.getAs<CompoundTag>()?.get("Player")?.getAs<CompoundTag>()
        val dimensionId = player?.get("Dimension")?.getAs<StringTag>()?.value

        val pos = player?.get("Pos")?.getAs<ListTag>()
        val x = pos?.get(0)?.getAs<DoubleTag>()?.value?.toInt()
        val z = pos?.get(2)?.getAs<DoubleTag>()?.value?.toInt()

        if (x == null || z == null || dimensionId == null) null
        else WorldDimension.namespace(dimensionId) to BlockLocation(x, z)
    }

    val playerNames: SnapshotStateMap<String, String> = mutableStateMapOf()

    init {
        NetworkScope.launch {
            val result = MinecraftUserProfile.fetch(tree.players.map { it.nameWithoutExtension })
            result.forEach { (uuid, name) -> playerNames[uuid] = name }
        }
    }

    fun reload() {
        levelInfo = DatWorker.read(tree.level.readBytes())
    }

    private fun type(path: String): WorldType {
        val file = File(path)
        return if (file.list()?.contains("server.properties") == true) {
            WorldType.SpigotServer
        } else {
            WorldType.Vanilla
        }
    }

}

enum class WorldType {
    Vanilla, SpigotServer
}

sealed class WorldFileType

class McaFileType(val location: ChunkLocation): WorldFileType()

object DatFileType: WorldFileType()
