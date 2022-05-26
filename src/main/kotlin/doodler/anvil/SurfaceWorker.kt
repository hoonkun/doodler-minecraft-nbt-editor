package doodler.anvil

import doodler.anvil.ArrayPacker.Companion.unpack
import doodler.nbt.tag.*

class SurfaceWorker {

    companion object {

        private val blockColors: Map<String, String> =
            SurfaceWorker::class.java.getResource("/minecraft/block_colors/blocks.json")!!.readText()
                .replace(Regex("[{}\"#]"), "")
                .split(",\n")
                .associate { it.split(":").let { pair -> "${pair[0].trim()}:${pair[1]}" to pair[2].trim() } }

        fun createSubChunk(tag: CompoundTag): List<SubChunk> {
            val sections = tag["sections"]?.getAs<ListTag>()?.value ?: return listOf()
            return sections.map {
                val blockStates = it.getAs<CompoundTag>()["block_states"]?.getAs<CompoundTag>() ?: return listOf()
                val data = blockStates["data"]?.getAs<LongArrayTag>()?.value ?: longArrayOf()
                val palette = blockStates["palette"]?.getAs<ListTag>()?.value?.map { paletteEach ->
                    paletteEach.getAs<CompoundTag>()["Name"]?.getAs<StringTag>()?.value ?: return listOf()
                } ?: throw Exception("Cannot create palette")
                SubChunk(data, palette, it.getAs<CompoundTag>()["Y"]?.getAs<ByteTag>()?.value ?: return listOf())
            }.reversed()
        }

        fun createSurface(location: ChunkLocation, input: List<SubChunk>, yLimit: Short, createValidY: Boolean = false): Surface {
            val resultBlocks = arrayOfNulls<SurfaceBlock>(256)
            val validYList = mutableSetOf<Short>()

            run {
                input.forEach { subChunk ->
                    if (subChunk.palette.size == 1 && subChunk.palette[0] == "minecraft:air") return@forEach

                    val blocks = subChunk.data.unpack(subChunk.palette.size).reversed()

                    blocks.forEachIndexed { index, block ->
                        val (x, y, z) = coordinate(index, subChunk.y)

                        var blockName: String? = null
                        var containsBlock: Boolean? = null
                        if (createValidY) {
                            // 아래 두 줄이 생각보다 무겁다... 이거 별로 안무거울 줄 알았는데 이거 있고 없고가 굉장한 차이를 만드네.
                            blockName = subChunk.palette[block.toInt()]
                            containsBlock = blockColors.containsKey(blockName)
                            if (containsBlock) validYList.add(y)
                        }
                        if (y > yLimit) return@forEachIndexed

                        val indexInArray = (15 - x) + (15 - z) * 16
                        val already = resultBlocks[indexInArray]
                        if (already != null && (!already.isWater || already.depth.toInt() != -99)) return@forEachIndexed

                        if (blockName == null) blockName = subChunk.palette[block.toInt()]
                        if (containsBlock == null) containsBlock = blockColors.containsKey(blockName)

                        if (containsBlock) {
                            if (already == null) {
                                resultBlocks[indexInArray] = SurfaceBlock(
                                    blockColors
                                        .getValue(blockName)
                                        .chunked(2)
                                        .map { it.toInt(16).toByte() }
                                        .toMutableList()
                                        .apply { add(-1) }
                                        .toByteArray(),
                                    y,
                                    blockName == "water"
                                )
                            } else if (already.depth.toInt() == -99 && blockName != "water") {
                                already.depth = (y.coerceIn(53, 60) - 53).toShort()
                            }
                        }
                    }
                }
            }

            val list = resultBlocks.toMutableList()
            for (index in 0 until list.size) {
                if (list[index] == null) {
                    list.removeAt(index)
                    list.add(index, SurfaceBlock(byteArrayOf(0, 0, 0, 0), -63))
                }
            }

            return Surface(location, list.filterNotNull().toList(), validYList)
        }

        private fun coordinate(blockIndex: Int, y: Byte): Triple<Byte, Short, Byte> {
            return Triple(
                ((blockIndex / 16) % 16).toByte(),
                (y * 16 + (((4095 - blockIndex) / (16 * 16)) % 16)).toShort(),
                (blockIndex % 16).toByte()
            )
        }

    }

}

data class SubChunk(
    val data: LongArray,
    val palette: List<String>,
    val y: Byte
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SubChunk

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

data class Surface(
    val position: ChunkLocation,
    val blocks: List<SurfaceBlock>,
    val validY: Set<Short>
)

data class SurfaceBlock(
    val color: ByteArray,
    val y: Short,
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
