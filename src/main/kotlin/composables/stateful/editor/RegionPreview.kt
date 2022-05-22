package composables.stateful.editor

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.zIndex
import doodler.anvil.*
import doodler.file.WorldTree
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jetbrains.skia.*
import org.jetbrains.skiko.toBufferedImage

@Composable
fun BoxScope.RegionPreview(tree: WorldTree, location: AnvilLocation = AnvilLocation(0, -1)) {
    var map by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(location) {
        withContext(Dispatchers.IO) {
            val bytes = tree.overworld.region.find { it.name == "r.${location.x}.${location.z}.mca" }?.readBytes()
                ?: return@withContext
            val subChunks = AnvilWorker.loadChunksWith(bytes) { chunkLoc, tag ->
                Pair(chunkLoc, SurfaceWorker.createSubChunk(tag))
            }
            val pixels = ByteArray(512 * 512 * 4) { index -> if (index % 4 == 2 || index % 4 == 3) -1 else 0 }
            subChunks.forEach { (loc, chunks) ->
                val baseX = loc.x * 16
                val baseZ = loc.z * 16
                val blocks = SurfaceWorker.createSurface(loc, chunks).blocks
                blocks.forEachIndexed { index, block ->
                    val x = baseX + (index / 16)
                    val z = 511 - (baseZ + (index % 16))

                    pixels[(x * 512 + z) * 4 + 0] = block.color[2]
                    pixels[(x * 512 + z) * 4 + 1] = block.color[1]
                    pixels[(x * 512 + z) * 4 + 2] = block.color[0]
                    pixels[(x * 512 + z) * 4 + 3] = block.color[3]
                }
            }
            map = Bitmap()
                .apply {
                    allocPixels(ImageInfo(512, 512, ColorType.N32, ColorAlphaType.OPAQUE))
                    installPixels(pixels)
                }
                .toBufferedImage()
                .toComposeImageBitmap()
            delay(3000)
        }
    }

    if (map == null) return

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .aspectRatio(1f)
            .align(Alignment.Center)
            .zIndex(0f)
    ) {
        Image(
            map!!,
            null,
            filterQuality = androidx.compose.ui.graphics.FilterQuality.None,
            modifier = Modifier.fillMaxSize()
        )
    }

}
