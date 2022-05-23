package composables.stateful.editor

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import doodler.anvil.*
import doodler.file.WorldTree
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.*
import org.jetbrains.skiko.toBufferedImage


@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun BoxScope.RegionPreview(
    tree: WorldTree,
    location: AnvilLocation,
    selected: ChunkLocation?,
    cached: SnapshotStateMap<AnvilLocation, ImageBitmap>,
    hasNbt: (ChunkLocation) -> Boolean,
    onSelect: (ChunkLocation) -> Unit
) {
    val nSelected = selected?.normalize(location)

    val load = load@ {
        val bytes = tree.overworld.region.find { it.name == "r.${location.x}.${location.z}.mca" }?.readBytes()
            ?: return@load
        val subChunks = AnvilWorker.loadChunksWith(bytes) { chunkLoc, tag ->
            Pair(chunkLoc, SurfaceWorker.createSubChunk(tag))
        }
        val pixels = ByteArray(512 * 512 * 4)
        val heights = ShortArray(512 * 512)
        subChunks.forEach { (loc, chunks) ->
            val baseX = loc.x * 16
            val baseZ = loc.z * 16
            val blocks = SurfaceWorker.createSurface(loc, chunks).blocks
            blocks.forEachIndexed { index, block ->
                val x = 511 - (baseX + (index / 16))
                val z = baseZ + (index % 16)

                val multiplier = if (block.isWater) block.depth / 7f * 30 - 30 else 1f

                pixels[(x * 512 + z) * 4 + 0] = (block.color[2] + multiplier).toInt().coerceIn(-128, 127).toByte()
                pixels[(x * 512 + z) * 4 + 1] = (block.color[1] + multiplier).toInt().coerceIn(-128, 127).toByte()
                pixels[(x * 512 + z) * 4 + 2] = (block.color[0] + multiplier).toInt().coerceIn(-128, 127).toByte()
                pixels[(x * 512 + z) * 4 + 3] = block.color[3]

                heights[x * 512 + z] = block.y

                val hIndex = (x + 1).coerceAtMost(511) * 512 + z
                val pIndex = hIndex * 4
                val aboveY = heights[hIndex]
                if (block.y < aboveY) {
                    (0..2).forEach {
                        pixels[pIndex + it] = (pixels[pIndex + it] + 15)
                            .coerceAtMost(127).toByte()
                    }
                } else if (block.y > aboveY) {
                    (0..2).forEach {
                        pixels[pIndex + it] = (pixels[pIndex + it] - 15)
                            .coerceAtLeast(-128).toByte()
                    }
                }
            }
        }
        cached[location] = Bitmap()
            .apply {
                allocPixels(ImageInfo(512, 512, ColorType.N32, ColorAlphaType.OPAQUE))
                installPixels(pixels)
            }
            .toBufferedImage()
            .toComposeImageBitmap()
    }

    LaunchedEffect(location) {
        if (cached[location] != null) return@LaunchedEffect

        withContext(Dispatchers.IO) {
            load()
        }
    }

    if (cached[location] == null) return

    var focused by remember { mutableStateOf(Pair(-1, -1)) }

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .aspectRatio(1f)
            .align(Alignment.Center)
            .onPointerEvent(PointerEventType.Exit) { focused = Pair(-1, -1) }
            .zIndex(0f)
    ) {
        Image(
            cached[location]!!,
            null,
            filterQuality = androidx.compose.ui.graphics.FilterQuality.None,
            modifier = Modifier.fillMaxSize()
        )
        Column(modifier = Modifier.fillMaxSize()) {
            for (x in 0 until 32) {
                Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    for (z in 0 until 32) {
                        val loc = ChunkLocation(x + 32 * location.x, z + 32 * location.z)
                        Box(
                            modifier = Modifier.weight(1f).fillMaxHeight()
                                .background(
                                    if (!hasNbt(loc)) Color(245, 102, 66, 60)
                                    else if (focused.first == x && focused.second == z) Color(255, 255, 255, 60)
                                    else Color.Transparent
                                )
                                .onPointerEvent(PointerEventType.Enter) {
                                    focused = Pair(x, z)
                                }
                                .onPointerEvent(PointerEventType.Press) {
                                    onSelect(loc)
                                }
                                .let {
                                    if (nSelected?.x == x && nSelected.z == z)
                                        it.border(2.dp, Color.White)
                                    else it
                                }
                        )
                    }
                }
            }
        }
    }

}
