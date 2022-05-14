package doodler.anvil

import doodler.anvil.Zlib.Companion.decompress
import nbt.Tag
import nbt.TagType
import nbt.extensions.byte
import nbt.tag.CompoundTag
import java.nio.ByteBuffer
import kotlin.math.floor

data class BlockLocation(val x: Int, val z: Int) {
    fun toChunkLocation(): ChunkLocation {
        return ChunkLocation(floor(this.x / 16.0).toInt(), floor(this.z / 16.0).toInt())
    }
}
data class AnvilLocation(val x: Int, val z: Int)
data class ChunkLocation(val x: Int, val z: Int) {
    fun toAnvilLocation(): AnvilLocation {
        return AnvilLocation(floor(this.x / 32.0).toInt(), floor(this.z / 32.0).toInt())
    }
}

fun Byte.u(): Int {
    return this.toUByte().toInt()
}

class AnvilWorker {

    companion object {

        fun loadChunkList(location: AnvilLocation, bytes: ByteArray): List<ChunkLocation> {
            val chunks = mutableListOf<ChunkLocation>()

            if (bytes.isEmpty()) return chunks

            for (m in 0 until 32 * 32) {
                val x = m / 32
                val z = m % 32
                val i = 4 * ((x and 31) + (z and 31) * 32)

                val offset = (bytes[i].u() * 65536 + bytes[i + 1].u() * 256 + bytes[i + 2].u()) * 4096
                val sectors = bytes[i + 3] * 4096

                if (offset == 0 || sectors == 0) continue

                chunks.add(ChunkLocation(32 * location.x + x, 32 * location.z + z))
            }

            return chunks
        }

        fun loadChunk(chunkLocation: ChunkLocation, bytes: ByteArray): CompoundTag? {
            val location = chunkLocation.toAnvilLocation()
            var result: CompoundTag? = null

            if (bytes.isEmpty()) return null

            for (m in 0 until 32 * 32) {
                val x = m / 32
                val z = m % 32
                val i = 4 * ((x and 31) + (z and 31) * 32)

                if (ChunkLocation(32 * location.x + x, 32 * location.z + z) != chunkLocation) continue

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

                result = Tag.read(TagType.TAG_COMPOUND, chunkBuffer, null, null).getAs()
            }

            return result
        }

    }

}
