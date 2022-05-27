package doodler.minecraft

import doodler.minecraft.structures.AnvilLocation
import doodler.minecraft.structures.ChunkLocation
import doodler.nbt.Tag
import doodler.nbt.TagType
import doodler.nbt.extensions.byte
import doodler.nbt.tag.CompoundTag
import java.nio.ByteBuffer

private fun Byte.u(): Int {
    return this.toUByte().toInt()
}

class McaWorker {

    companion object {

        fun loadChunkList(location: AnvilLocation, bytes: ByteArray): List<ChunkLocation> {
            val chunks = mutableListOf<ChunkLocation>()

            if (bytes.isEmpty()) return chunks

            for (m in 0 until 32 * 32) {
                val x = m / 32
                val z = m % 32

                val (offset, sectors) = parseHeader(parseIndex(x, z), bytes)

                if (offset == 0 || sectors == 0) continue

                chunks.add(ChunkLocation(32 * location.x + x, 32 * location.z + z))
            }

            return chunks
        }

        fun loadChunk(chunkLocation: ChunkLocation, bytes: ByteArray): CompoundTag? {
            val location = chunkLocation.toAnvilLocation()

            if (bytes.isEmpty()) return null

            val x = chunkLocation.x - 32 * location.x
            val z = chunkLocation.z - 32 * location.z

            val (offset, sectors) = parseHeader(parseIndex(x, z), bytes)

            // Use below code when timestamp value is needed.
            // val timestamp = ByteBuffer.wrap(bytes.slice(4096 + i until 4096 + i + 4).toByteArray()).int

            if (offset == 0 || sectors == 0) return null

            val chunkBuffer = getChunkBuffer(bytes, offset, sectors)

            return Tag.read(TagType.TAG_COMPOUND, chunkBuffer, null, null).getAs()
        }

        fun <R>loadChunksWith(bytes: ByteArray, callback: (ChunkLocation, CompoundTag) -> R): List<R> {
            if (bytes.isEmpty()) return listOf()

            val result = mutableListOf<R>()

            for (m in 0 until 32 * 32) {
                val x = m / 32
                val z = m % 32

                val (offset, sectors) = parseHeader(parseIndex(x, z), bytes)

                if (offset == 0 || sectors == 0) continue

                val chunkBuffer = getChunkBuffer(bytes, offset, sectors)

                result.add(callback(
                    ChunkLocation(x, z),
                    Tag.read(TagType.TAG_COMPOUND, chunkBuffer, null, null).getAs()
                ))
            }

            return result
        }

        private fun getChunkBuffer(bytes: ByteArray, offset: Int, sectors: Int): ByteBuffer {
            val data = ByteBuffer.wrap(bytes.slice(offset until offset + sectors).toByteArray())
            val size = data.int

            val compressionType = data.byte
            val compressed = ByteArray(size - 1).also { data.get(it) }

            if (compressionType.toInt() != 2) throw Exception("unsupported compression type '$compressionType'")

            val chunkBuffer = ByteBuffer.wrap(CompressUtils.Zlib.decompress(compressed))
            chunkBuffer.byte
            chunkBuffer.short

            return chunkBuffer
        }

        private fun parseIndex(x: Int, z: Int) = 4 * ((x and 31) + (z and 31) * 32)

        private fun parseHeader(index: Int, bytes: ByteArray): Pair<Int, Int> {
            val offset = (bytes[index].u() * 65536 + bytes[index + 1].u() * 256 + bytes[index + 2].u()) * 4096
            val sectors = bytes[index + 3] * 4096

            return Pair(offset, sectors)
        }

    }

}
