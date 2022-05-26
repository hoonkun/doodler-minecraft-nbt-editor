package doodler.file

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.graphics.ImageBitmap
import composables.stateful.editor.DirectoryItem
import composables.stateful.editor.FileItem
import composables.stateful.editor.WorldTreeItem
import composables.states.editor.world.DoodleException
import doodler.anvil.AnvilLocation
import doodler.anvil.GZip
import doodler.nbt.Tag
import doodler.nbt.TagType
import doodler.nbt.extensions.byte
import doodler.nbt.tag.CompoundTag
import java.io.File
import java.io.FileNotFoundException
import java.nio.ByteBuffer

class IOUtils {

    companion object {

        fun load(path: String): WorldTree {
            val world = File(path)

            if (!world.exists()) throw FileNotFoundException()

            val nether = "$path/${WorldDimension.NETHER.ident}"
            val theEnd = "$path/${WorldDimension.THE_END.ident}"

            return (
                WorldTree(
                    File("$path/icon.png"),
                    File("$path/level.dat"),
                    File("$path/advancements").listIfExists(),
                    File("$path/stats").listIfExists(),
                    File("$path/playerdata").listIfExists(),
                    WorldDimensionTree(
                        File("$path/region").listIfExists(),
                        File("$path/entities").listIfExists(),
                        File("$path/poi").listIfExists(),
                        File("$path/data").listIfExists()
                    ),
                    WorldDimensionTree(
                        File("$nether/region").listIfExists(),
                        File("$nether/entities").listIfExists(),
                        File("$nether/poi").listIfExists(),
                        File("$nether/data").listIfExists()
                    ),
                    WorldDimensionTree(
                        File("$theEnd/region").listIfExists(),
                        File("$theEnd/entities").listIfExists(),
                        File("$theEnd/poi").listIfExists(),
                        File("$theEnd/data").listIfExists()
                    )
                )
            )
        }

        fun readLevel(bytes: ByteArray): CompoundTag {
            val uncompressed = GZip.decompress(bytes)
            return Tag.read(TagType.TAG_COMPOUND, ByteBuffer.wrap(uncompressed).apply { byte; short; }, null, null).getAs()
        }

        private fun File.listIfExists(): List<File> {
            return this.let { if (!it.exists()) null else it.listFiles()?.toList() } ?: listOf()
        }

    }

}

class WorldTree (
    val icon: File,
    val level: File,
    val advancements: List<File>,
    val stats: List<File>,
    val players: List<File>,
    private val overworld: WorldDimensionTree,
    private val nether: WorldDimensionTree,
    private val end: WorldDimensionTree
) {
    operator fun get(key: WorldDimension): WorldDimensionTree {
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

data class CachedTerrainInfo(val yLimit: Int, val location: AnvilLocation)

class WorldDimensionTree (
    private val region: List<File>,
    private val entities: List<File>,
    private val poi: List<File>,
    val data: List<File>,
    val cachedTerrains: SnapshotStateMap<CachedTerrainInfo, ImageBitmap> = mutableStateMapOf()
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
    enum class McaType(val pathName: String) {
        TERRAIN("region"), ENTITY("entities"), POI("poi");

        companion object {

            operator fun get(pathName: String): McaType =
                values().find { it.pathName == pathName } ?: throw DoodleException(
                    "Internal Error",
                    null,
                    "Cannot find McaInfo.Type with pathName '$pathName'"
                )

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
