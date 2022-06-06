package doodler.minecraft

import doodler.minecraft.structures.WorldDimension
import doodler.minecraft.structures.WorldDimensionHierarchy
import doodler.minecraft.structures.WorldHierarchy
import java.io.File
import java.io.FileNotFoundException

class WorldUtils {

    companion object {

        fun load(path: String): WorldHierarchy {
            val world = File(path)

            if (!world.exists()) throw FileNotFoundException()

            val nether = "$path/${WorldDimension.NETHER.ident}"
            val theEnd = "$path/${WorldDimension.THE_END.ident}"

            return (
                WorldHierarchy(
                    File("$path/icon.png"),
                    File("$path/level.dat"),
                    File("$path/advancements").listIfExists(),
                    File("$path/stats").listIfExists(),
                    File("$path/playerdata").listIfExists(),
                    WorldDimensionHierarchy(
                        File("$path/region").listIfExists(),
                        File("$path/entities").listIfExists(),
                        File("$path/poi").listIfExists(),
                        File("$path/data").listIfExists()
                    ),
                    WorldDimensionHierarchy(
                        File("$nether/region").listIfExists(),
                        File("$nether/entities").listIfExists(),
                        File("$nether/poi").listIfExists(),
                        File("$nether/data").listIfExists()
                    ),
                    WorldDimensionHierarchy(
                        File("$theEnd/region").listIfExists(),
                        File("$theEnd/entities").listIfExists(),
                        File("$theEnd/poi").listIfExists(),
                        File("$theEnd/data").listIfExists()
                    )
                )
            )
        }

        private fun File.listIfExists(): List<File> {
            return this.let { if (!it.exists()) null else it.listFiles()?.toList() } ?: listOf()
        }

    }

}

