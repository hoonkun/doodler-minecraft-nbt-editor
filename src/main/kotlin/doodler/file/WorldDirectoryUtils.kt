package doodler.file

import java.io.File
import java.io.FileNotFoundException

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

class WorldData (
    val icon: File,
    val level: File,
    val advancements: List<File>,
    val stats: List<File>,
    val players: List<File>,
    val overworld: WorldDimension,
    val nether: WorldDimension,
    val end: WorldDimension
)

class WorldDimension (
    val region: List<File>,
    val entities: List<File>,
    val poi: List<File>,
    val data: List<File>
)
