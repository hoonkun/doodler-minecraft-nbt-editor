package doodler.minecraft

import doodler.extension.safeListFiles
import doodler.minecraft.structures.*
import java.io.File
import java.io.FileNotFoundException

class WorldUtils {

    companion object {

        fun loadVanilla(path: String): VanillaWorldHierarchy {
            val world = File(path)

            if (!world.exists()) throw FileNotFoundException()

            val nether = "$path/${WorldDimension.Nether.ident}"
            val theEnd = "$path/${WorldDimension.TheEnd.ident}"

            return VanillaWorldHierarchy(
                File("$path/level.dat"),
                File("$path/icon.png"),
                File("$path/advancements").safeListFiles(),
                File("$path/generated/minecraft/structures").safeListFiles(),
                File("$path/stats").safeListFiles(),
                File("$path/playerdata").safeListFiles(),
                WorldDimensionHierarchy(
                    File("$path/region").safeListFiles(),
                    File("$path/entities").safeListFiles(),
                    File("$path/poi").safeListFiles(),
                    File("$path/data").safeListFiles()
                ),
                WorldDimensionHierarchy(
                    File("$nether/region").safeListFiles(),
                    File("$nether/entities").safeListFiles(),
                    File("$nether/poi").safeListFiles(),
                    File("$nether/data").safeListFiles()
                ),
                WorldDimensionHierarchy(
                    File("$theEnd/region").safeListFiles(),
                    File("$theEnd/entities").safeListFiles(),
                    File("$theEnd/poi").safeListFiles(),
                    File("$theEnd/data").safeListFiles()
                )
            )
        }

        fun loadServer(path: String): SpigotServerWorldHierarchy {
            val world = File(path)

            if (!world.exists()) throw FileNotFoundException()

            val serverProperties = File("$path/server.properties")
                .readText()
                .trim()
                .split("\n")
                .filter { !it.startsWith("#") }
                .associate { record -> record.split("=").let { it[0] to it[1] } }

            val levelName = serverProperties.getValue("level-name")

            val overworld = "$path/$levelName"
            val nether = "$path/${levelName}_${WorldDimension.Nether.displayName}"
            val theEnd = "$path/${levelName}_${WorldDimension.TheEnd.displayName}"

            return SpigotServerWorldHierarchy(
                mapOf(
                    WorldDimension.Overworld to File("$overworld/level.dat"),
                    WorldDimension.Nether to File("$nether/level.dat"),
                    WorldDimension.TheEnd to File("$theEnd/level.dat")
                ),
                File("$path/server-icon.png"),
                File("$overworld/advancements").safeListFiles(),
                File("$overworld/generated/minecraft/structures").safeListFiles(),
                File("$overworld/stats").safeListFiles(),
                File("$overworld/playerdata").safeListFiles(),
                WorldDimensionHierarchy(
                    File("$overworld/region").safeListFiles(),
                    File("$overworld/entities").safeListFiles(),
                    File("$overworld/poi").safeListFiles(),
                    File("$overworld/data").safeListFiles()
                ),
                WorldDimensionHierarchy(
                    File("$nether/region").safeListFiles(),
                    File("$nether/entities").safeListFiles(),
                    File("$nether/poi").safeListFiles(),
                    File("$nether/data").safeListFiles()
                ),
                WorldDimensionHierarchy(
                    File("$theEnd/region").safeListFiles(),
                    File("$theEnd/entities").safeListFiles(),
                    File("$theEnd/poi").safeListFiles(),
                    File("$theEnd/data").safeListFiles()
                )
            )
        }

    }

}

