package doodler.minecraft

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.toComposeImageBitmap
import composable.editor.world.yRange
import doodler.editor.CachedTerrainInfo
import doodler.editor.TerrainCache
import doodler.extension.throwIfInactive
import doodler.extension.toReversedRange
import doodler.local.UserSavedLocalState
import doodler.minecraft.ArrayPacker.Companion.unpack
import doodler.minecraft.structures.*
import doodler.nbt.tag.*
import doodler.types.EmptyLambda
import kotlinx.coroutines.*
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skiko.toBufferedImage
import java.io.File

class SurfaceWorker {

    companion object {

        private val blockColors: Map<String, String> =
            SurfaceWorker::class.java.getResource("/minecraft/block_colors/blocks.json")!!.readText()
                .replace(Regex("[{}\"#]"), "")
                .split(",\n")
                .associate { it.split(":").let { pair -> "${pair[0].trim()}:${pair[1]}" to pair[2].trim() } }

        private val rootJob = Job()
        private val scope = CoroutineScope(Dispatchers.IO + rootJob)
        private val loaderStack = mutableStateListOf<Job>()

        val maxStack get() = UserSavedLocalState.loaderStackSize
        val stackPoint get() = loaderStack.size

        private suspend fun subChunk(
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

        private suspend fun surface(
            location: ChunkLocation,
            input: List<SurfaceSubChunk>,
            yLimit: Int,
            createValidY: Boolean = false
        ): Surface {
            return coroutineScope {
                val resultBlocks = arrayOfNulls<SurfaceBlock>(256)
                val validYList = mutableSetOf<Int>()

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

        private suspend fun bitmap(
            from: File,
            cache: TerrainCache,
            yLimit: Int,
            location: AnvilLocation,
            dimension: WorldDimension
        ) = coroutineScope {

            val terrainInfo = CachedTerrainInfo(yLimit, location)

            if (cache.terrains[terrainInfo] != null) return@coroutineScope

            val bytes = from.readBytes()
            val subChunks = McaWorker.loadChunksWith(bytes) { chunkLocation, compoundTag ->
                chunkLocation to subChunk(compoundTag)
            }
            val pixels = ByteArray(512 * 512 * 4)
            val heights = IntArray(512 * 512)

            val createY = cache.yRanges[terrainInfo.location] == null
            val yRange = mutableSetOf<Int>()

            subChunks.forEach { (location, chunks) ->
                val baseX = location.x * 16
                val baseZ = location.z * 16
                val surface = surface(location, chunks, yLimit, createY)
                val blocks = surface.blocks

                if (createY) yRange.addAll(surface.validY)

                blocks.forEachIndexed { index, block ->
                    val x = 511 - (baseX + (index / 16))
                    val z = baseZ + (index % 16)

                    val bIndex = x * 512 + z
                    val pbIndex = bIndex * 4

                    val multiplier = if (block.isWater) block.depth / 7f * 30 - 30 else 1f
                    val cutout = if (block.y == yLimit) 0.5f else 1f

                    pixels[pbIndex + 0] = ((block.color[2].toUByte().toInt() + multiplier) * cutout).toInt().coerceIn(0, 255).toByte()
                    pixels[pbIndex + 1] = ((block.color[1].toUByte().toInt() + multiplier) * cutout).toInt().coerceIn(0, 255).toByte()
                    pixels[pbIndex + 2] = ((block.color[0].toUByte().toInt() + multiplier) * cutout).toInt().coerceIn(0, 255).toByte()
                    pixels[pbIndex + 3] = block.color[3]

                    heights[bIndex] = block.y

                    val hIndex = (x + 1).coerceAtMost(511) * 512 + z
                    val pIndex = hIndex * 4
                    val aboveY = heights[hIndex]
                    if (block.y < aboveY) {
                        (0..2).forEach {
                            pixels[pIndex + it] = (pixels[pIndex + it].toUByte().toInt() + 15)
                                .coerceAtMost(255).toByte()
                        }
                    } else if (block.y > aboveY) {
                        (0..2).forEach {
                            pixels[pIndex + it] = (pixels[pIndex + it].toUByte().toInt() - 15)
                                .coerceAtLeast(0).toByte()
                        }
                    }
                }
            }

            if (createY)
                cache.yRanges[terrainInfo.location] = yRange
                    .toReversedRange(dimension.yRange.first, dimension.yRange.last)

            val bitmap = Bitmap()
                .apply {
                    allocPixels(ImageInfo(512, 512, ColorType.N32, ColorAlphaType.OPAQUE))
                    installPixels(pixels)
                }
                .toBufferedImage()
                .toComposeImageBitmap()

            val y = cache.yRanges[terrainInfo.location]?.find { it.contains(yLimit) }
            if (y != null) {
                y.asIterable().forEach { limit ->
                    val criteriaInfo = terrainInfo.copy(yLimit = limit)
                    val criteria = cache.terrains[criteriaInfo]
                    if (criteria == null) cache.terrains[criteriaInfo] = bitmap
                }
            } else {
                cache.terrains[terrainInfo] = bitmap
            }

        }

        fun load(
            from: File?,
            cache: TerrainCache,
            yLimit: Int,
            location: AnvilLocation,
            dimension: WorldDimension
        ) {
            if (from == null) return
            if (cache.terrains[CachedTerrainInfo(yLimit, location)] != null) return

            val newJob = scope.launch {
                try { bitmap(from, cache, yLimit, location, dimension) }
                catch(e: Exception) { EmptyLambda() }
            }

            loaderStack.add(newJob)
            newJob.invokeOnCompletion { loaderStack.remove(newJob) }

            if (loaderStack.size > maxStack) loaderStack.removeFirst().cancel()
        }

        private fun coordinate(blockIndex: Int, y: Byte): Triple<Byte, Int, Byte> {
            return Triple(
                ((blockIndex / 16) % 16).toByte(),
                (y * 16 + (((4095 - blockIndex) / (16 * 16)) % 16)),
                (blockIndex % 16).toByte()
            )
        }

    }

}
