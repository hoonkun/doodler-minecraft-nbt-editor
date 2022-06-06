package doodler.minecraft.structures

import activator.doodler.doodle.DoodleException

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
