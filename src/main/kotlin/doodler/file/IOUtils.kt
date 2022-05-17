package doodler.file

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
    val overworld: WorldDimensionTree,
    val nether: WorldDimensionTree,
    val end: WorldDimensionTree
) {
    operator fun get(key: String): WorldDimensionTree {
        return when (key) {
            "" -> overworld
            "DIM1" -> end
            "DIM-1" -> nether
            else -> throw Exception("Invalid dimension name: $key")
        }
    }
}

class WorldDimensionTree (
    val region: List<File>,
    val entities: List<File>,
    val poi: List<File>,
    val data: List<File>
)
