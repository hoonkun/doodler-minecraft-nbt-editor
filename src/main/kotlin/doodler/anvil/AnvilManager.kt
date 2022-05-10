package doodler.anvil

import nbt.Tag
import nbt.TagType
import nbt.extensions.byte
import nbt.tag.CompoundTag
import java.nio.ByteBuffer
import kotlin.math.floor

data class AnvilLocation(val x: Int, val z: Int)
data class ChunkLocation(val x: Int, val z: Int) {
    fun toAnvilLocation(): AnvilLocation {
        return AnvilLocation(floor(this.x / 32.0).toInt(), floor(this.z / 32.0).toInt())
    }
}

fun Byte.u(): Int {
    return this.toUByte().toInt()
}

class AnvilManager private constructor() {
    companion object {
        val instance = AnvilManager()
    }

    fun load(location: AnvilLocation, bytes: ByteArray): Map<ChunkLocation, CompoundTag> {
        val parts = mutableMapOf<ChunkLocation, CompoundTag>()

        if (bytes.isEmpty()) return parts

        for (m in 0 until 32 * 32) {
            val x = m / 32
            val z = m % 32
            val i = 4 * ((x and 31) + (z and 31) * 32)

            val offset = (bytes[i].u() * 65536 + bytes[i + 1].u() * 256 + bytes[i + 2].u()) * 4096
            val sectors = bytes[i + 3] * 4096

            // Use below code when timestamp value is needed.
            // val timestamp = ByteBuffer.wrap(bytes.slice(4096 + i until 4096 + i + 4).toByteArray()).int

            if (offset == 0 || sectors == 0) continue

            val data = ByteBuffer.wrap(bytes.slice(offset until offset + sectors).toByteArray())
            val size = data.int

            val compressionType = data.byte
            val compressed = ByteArray(size).also { data.get(it) }

            if (compressionType.toInt() != 2) throw Exception("unsupported compression type '$compressionType'")

            val chunkBuffer = ByteBuffer.wrap(decompress(compressed))
            chunkBuffer.byte
            chunkBuffer.short

            parts[ChunkLocation(32 * location.x + x, 32 * location.z + z)] =
                Tag.read(TagType.TAG_COMPOUND, chunkBuffer, null, null).getAs()
        }

        return parts
    }
}
