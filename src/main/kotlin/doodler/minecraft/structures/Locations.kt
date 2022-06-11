package doodler.minecraft.structures

import kotlin.math.floor

data class BlockLocation(val x: Int, val z: Int) {
    fun toChunkLocation(): ChunkLocation {
        return ChunkLocation(floor(this.x / 16.0).toInt(), floor(this.z / 16.0).toInt())
    }
}

data class AnvilLocation(val x: Int, val z: Int) {
    fun nullIfNotExists(where: List<AnvilLocation>) = if (where.contains(this)) this else null

    companion object {
        fun fromFileName(name: String) = name.split(".").let { AnvilLocation(it[1].toInt(), it[2].toInt()) }
    }
}

data class ChunkLocation(val x: Int, val z: Int) {
    fun normalize(anvilLocation: AnvilLocation): ChunkLocation {
        return ChunkLocation(x - 32 * anvilLocation.x, z - 32 * anvilLocation.z)
    }
    fun toAnvilLocation(): AnvilLocation {
        return AnvilLocation(floor(this.x / 32.0).toInt(), floor(this.z / 32.0).toInt())
    }
    fun toStringPair(): Pair<String, String> = Pair("$x", "$z")
}

data class AnvilLocationSurroundings(
    val base: AnvilLocation,
    val left: AnvilLocation?,
    val right: AnvilLocation?,
    val above: AnvilLocation?,
    val below: AnvilLocation?
) {
    companion object {

        fun fromBase(base: AnvilLocation, available: List<AnvilLocation>) =
            AnvilLocationSurroundings(
                base = base,
                left = AnvilLocation(base.x, base.z - 1).nullIfNotExists(available),
                right = AnvilLocation(base.x, base.z + 1).nullIfNotExists(available),
                above = AnvilLocation(base.x + 1, base.z).nullIfNotExists(available),
                below = AnvilLocation(base.x - 1, base.z).nullIfNotExists(available),
            )

    }
}