package doodler.file

import doodler.anvil.GZip
import nbt.Tag
import nbt.TagType
import nbt.extensions.byte
import nbt.tag.CompoundTag
import java.io.File
import java.io.FileNotFoundException
import java.nio.ByteBuffer

class WorldDirectoryUtils {

    companion object {

        fun load(path: String): WorldData {
            val world = File(path)

            if (!world.exists()) throw FileNotFoundException()

            return (
                WorldData(
                    File("$path/icon.png"),
                    File("$path/level.dat"),
                    File("$path/advancements").listIfExists(),
                    File("$path/stats").listIfExists(),
                    File("$path/playerdata").listIfExists(),
                    WorldDimension(
                        File("$path/region").listIfExists(),
                        File("$path/entities").listIfExists(),
                        File("$path/poi").listIfExists(),
                        File("$path/data").listIfExists()
                    ),
                    WorldDimension(
                        File("$path/DIM-1/region").listIfExists(),
                        File("$path/DIM-1/entities").listIfExists(),
                        File("$path/DIM-1/poi").listIfExists(),
                        File("$path/DIM-1/data").listIfExists()
                    ),
                    WorldDimension(
                        File("$path/DIM1/region").listIfExists(),
                        File("$path/DIM1/entities").listIfExists(),
                        File("$path/DIM1/poi").listIfExists(),
                        File("$path/DIM1/data").listIfExists()
                    )
                )
            )
        }

        private fun File.listIfExists(): List<File> {
            return this.let { if (!it.exists()) null else it.listFiles()?.toList() } ?: listOf()
        }

    }

}

class LevelUtils {

    companion object {

        fun read(bytes: ByteArray): CompoundTag {
            val uncompressed = GZip.decompress(bytes)
            return Tag.read(TagType.TAG_COMPOUND, ByteBuffer.wrap(uncompressed).apply { byte; short; }, null, null).getAs()
        }

    }

}

class WorldData (
    val icon: File,
    val level: File,
    val advancements: List<File>,
    val stats: List<File>,
    val players: List<File>,
    val overworld: WorldDimension,
    val nether: WorldDimension,
    val end: WorldDimension
) {
    operator fun get(key: String): WorldDimension {
        return when (key) {
            "" -> overworld
            "DIM1" -> end
            "DIM-1" -> nether
            else -> throw Exception("Invalid dimension name: $key")
        }
    }
}

class WorldDimension (
    val region: List<File>,
    val entities: List<File>,
    val poi: List<File>,
    val data: List<File>
)
