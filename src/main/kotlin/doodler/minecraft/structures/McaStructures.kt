package doodler.minecraft.structures

import doodler.exceptions.DoodleException

enum class McaType(
    val pathName: String,
    val displayName: String,
) {
    Terrain("region", "Terrains"),
    Entity("entities", "Entities"),
    Poi("poi", "PointOfIntersts");

    companion object {

        operator fun get(pathName: String): McaType =
            values().find { it.pathName == pathName } ?: throw DoodleException(
                "Internal Error",
                null,
                "Cannot find McaInfo.Type with pathName '$pathName'"
            )

        fun fromMcaPath(path: String): McaType {
            val segments = path.split("/").toMutableList()
            val pathName = segments.slice(0 until segments.size - 1).last()
            return McaType[pathName]
        }

    }
}
