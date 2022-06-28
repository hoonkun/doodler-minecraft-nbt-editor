package doodler.minecraft.structures


data class Surface(
    val position: ChunkLocation,
    val blocks: List<SurfaceBlock>,
    val validY: Set<Int>
)

data class SurfaceSubChunk(
    val data: LongArray,
    val palette: List<String>,
    val y: Byte
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SurfaceSubChunk

        if (!data.contentEquals(other.data)) return false
        if (palette != other.palette) return false

        return true
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + palette.hashCode()
        return result
    }
}

data class SurfaceBlock(
    val color: ByteArray,
    val y: Int,
    val isWater: Boolean = false,
    var depth: Short = -99
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SurfaceBlock

        if (!color.contentEquals(other.color)) return false
        if (y != other.y) return false

        return true
    }

    override fun hashCode(): Int {
        var result = color.contentHashCode()
        result = 31 * result + y
        return result
    }
}
