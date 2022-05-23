package doodler.file

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.graphics.ImageBitmap
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
                        File("$path/DIM-1/region").listIfExists(),
                        File("$path/DIM-1/entities").listIfExists(),
                        File("$path/DIM-1/poi").listIfExists(),
                        File("$path/DIM-1/data").listIfExists()
                    ),
                    WorldDimensionTree(
                        File("$path/DIM1/region").listIfExists(),
                        File("$path/DIM1/entities").listIfExists(),
                        File("$path/DIM1/poi").listIfExists(),
                        File("$path/DIM1/data").listIfExists()
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
}

class WorldDimensionTree (
    val region: List<File>,
    val entities: List<File>,
    val poi: List<File>,
    val data: List<File>,
    val cachedTerrains: SnapshotStateMap<AnvilLocation, ImageBitmap> = mutableStateMapOf()
)

enum class WorldDimension(
    val ident: String,
    val namespaceId: String,
    val displayName: String
) {
    OVERWORLD("", "minecraft:overworld", "Overworld"),
    NETHER("DIM-1", "minecraft:the_nether", "Nether"),
    THE_END("DIM1", "minecraft:the_end", "The End")
}
