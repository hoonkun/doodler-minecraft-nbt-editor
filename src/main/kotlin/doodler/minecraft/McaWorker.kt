package doodler.minecraft

import doodler.minecraft.structures.AnvilLocation
import doodler.minecraft.structures.ChunkLocation
import doodler.nbt.Tag
import doodler.nbt.TagType
import doodler.nbt.extensions.byte
import doodler.nbt.tag.CompoundTag
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import kotlin.math.ceil

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

        fun writeChunk(file: File, root: CompoundTag, location: ChunkLocation) {
            val mcaLocation = location.toAnvilLocation()
            val normalized = location.normalize(mcaLocation)

            val originalFileBytes = file.readBytes()

            val newCompressedBytes = compressChunk(root)

            val locationHeaderBuffer = ByteBuffer.allocate(4096)
            val dataStream = ByteArrayOutputStream()
            var allocatedSectors = 2

            for (m in 0 until 32 * 32) {
                val x = m % 32
                val z = m / 32

                val (offset, sectors) = parseHeader(parseIndex(x, z), originalFileBytes)

                val data =
                    if (normalized.x == x && normalized.z == z) newCompressedBytes
                    else originalFileBytes.slice(offset until offset + sectors).toByteArray()

                dataStream.writeBytes(data)

                val newOffset = allocatedSectors
                val newSectors = ceil(data.size / 4096f).toInt()
                allocatedSectors += newSectors

                val locationHeaderValue = (newOffset shl Byte.SIZE_BITS) or (newSectors and 0b11111111)
                locationHeaderBuffer.putInt(locationHeaderValue)
            }

            val locationHeader = locationHeaderBuffer.array()
            val timestampHeader = originalFileBytes.slice(4096 until 8192).toByteArray()

            val fileBytes = ByteArrayOutputStream().apply {
                writeBytes(locationHeader)
                writeBytes(timestampHeader)
                writeBytes(dataStream.toByteArray())
            }.toByteArray()

            file.writeBytes(fileBytes)
        }


        private fun compressChunk(tag: CompoundTag): ByteArray {
            val buffer = ByteBuffer.allocate(Byte.SIZE_BYTES + Short.SIZE_BYTES + tag.sizeInBytes)
            tag.ensureName(null).getAs<CompoundTag>().writeAsRoot(buffer)

            val compressedData = CompressUtils.Zlib.compress(buffer.array())
            val compressionScheme = 2

            val length = compressedData.size + 1

            val result = ByteBuffer.allocate(padding(4 + length))
            result.putInt(length)
            result.put(compressionScheme.toByte())
            result.put(compressedData)

            return result.array()
        }

        private fun padding(size: Int): Int {
            var index = 0
            while (size > index * 4096) index++

            return index * 4096
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
