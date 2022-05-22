package composables.stateful.editor

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.zIndex
import doodler.anvil.*
import doodler.file.WorldTree
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.skia.*
import org.jetbrains.skiko.toBufferedImage

@Composable
fun BoxScope.RegionPreview(tree: WorldTree, location: AnvilLocation = AnvilLocation(0, -1)) {
    var byteList by remember { mutableStateOf(ByteArray(0)) }

    LaunchedEffect(location) {
        launch {
            val bytes = tree.overworld.region.find { it.name == "r.${location.x}.${location.z}.mca" }?.readBytes()
                ?: return@launch
            val subChunks = AnvilWorker.loadChunksWith(bytes) { chunkLoc, tag ->
                Pair(chunkLoc, SurfaceWorker.createSubChunk(tag))
            }
            val temp = ByteArray(512 * 512 * 4) { index -> if (index % 4 == 2 || index % 4 == 3) -1 else 0 }
            subChunks.forEach { (loc, chunks) ->
                val baseX = loc.x * 16
                val baseZ = loc.z * 16
                val blocks = SurfaceWorker.createSurface(loc, chunks).blocks
                blocks.forEachIndexed { index, block ->
                    val x = baseX + (index / 16)
                    val z = baseZ + (index % 16)

                    temp[(x * 512 + z) * 4 + 0] = block.color[2]
                    temp[(x * 512 + z) * 4 + 1] = block.color[1]
                    temp[(x * 512 + z) * 4 + 2] = block.color[0]
                    temp[(x * 512 + z) * 4 + 3] = block.color[3]
                }
            }
            byteList = temp
            delay(3000)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .aspectRatio(1f)
            .align(Alignment.Center)
            .zIndex(0f)
    ) {
        Image(
            Bitmap().apply {
                allocPixels(ImageInfo(512, 512, ColorType.N32, ColorAlphaType.OPAQUE))
                installPixels(byteList)
            }.toBufferedImage().toComposeImageBitmap(),
            null,
            filterQuality = androidx.compose.ui.graphics.FilterQuality.None,
            modifier = Modifier.fillMaxSize()
        )
    }

}
