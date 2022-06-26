package doodler.minecraft

import doodler.extension.throwIfInactive
import doodler.minecraft.ArrayPacker.Companion.unpack
import doodler.minecraft.structures.ChunkLocation
import doodler.minecraft.structures.SurfaceSubChunk
import doodler.minecraft.structures.Surface
import doodler.minecraft.structures.SurfaceBlock
import doodler.nbt.tag.*
import kotlinx.coroutines.coroutineScope

class SurfaceWorker {

    companion object {

        private val blockColors: Map<String, String> =
            SurfaceWorker::class.java.getResource("/minecraft/block_colors/blocks.json")!!.readText()
                .replace(Regex("[{}\"#]"), "")
                .split(",\n")
                .associate { it.split(":").let { pair -> "${pair[0].trim()}:${pair[1]}" to pair[2].trim() } }

        suspend fun createSubChunk(
            tag: CompoundTag
        ): List<SurfaceSubChunk> {
            return coroutineScope lambda@ {
                val sections = tag["sections"]?.getAs<ListTag>()?.value ?: return@lambda listOf()
                sections.map {
                    throwIfInactive()
                    val blockStates = it.getAs<CompoundTag>()["block_states"]?.getAs<CompoundTag>() ?: return@lambda listOf()
                    val data = blockStates["data"]?.getAs<LongArrayTag>()?.value ?: longArrayOf()
                    val palette = blockStates["palette"]?.getAs<ListTag>()?.value?.map { paletteEach ->
                        paletteEach.getAs<CompoundTag>()["Name"]?.getAs<StringTag>()?.value ?: return@lambda listOf()
                    } ?: throw Exception("Cannot create palette")
                    SurfaceSubChunk(data, palette, it.getAs<CompoundTag>()["Y"]?.getAs<ByteTag>()?.value ?: return@lambda listOf())
                }.reversed()
            }
        }

        suspend fun createSurface(
            location: ChunkLocation,
            input: List<SurfaceSubChunk>,
            yLimit: Short,
            createValidY: Boolean = false
        ): Surface {
            return coroutineScope {
                val resultBlocks = arrayOfNulls<SurfaceBlock>(256)
                val validYList = mutableSetOf<Short>()

                run {
                    input.forEach { subChunk ->
                        throwIfInactive()

                        if (subChunk.palette.size == 1 && subChunk.palette[0] == "minecraft:air") return@forEach

                        val blocks = subChunk.data.unpack(subChunk.palette.size).reversed()

                        blocks.forEachIndexed { index, block ->
                            throwIfInactive()

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

                Surface(location, list.filterNotNull().toList(), validYList)
            }
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
