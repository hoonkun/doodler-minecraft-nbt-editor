package composable.editor.world

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import doodler.editor.*
import doodler.editor.states.McaEditorState
import doodler.minecraft.McaWorker
import doodler.minecraft.structures.*
import java.io.File


@Composable
fun <K> BoxScope.McaEditor(
    editor: McaEditor<K>,
    worldSpec: WorldSpecification,
    openChunkNbt: (ChunkLocation, File) -> Unit,
    update: (McaPayload) -> Unit
) {
    val terrainCache = remember { mutableStateMapOf<CachedTerrainInfo, ImageBitmap>() }
    val yRangeCache = remember { mutableMapOf<AnvilLocation, List<IntRange>>() }

    val chunks by remember {
        derivedStateOf {
            val payload = editor.payload
            when (editor) {
                is SingleMcaEditor -> McaWorker.loadChunkList(payload.location, payload.file.readBytes())
                is GlobalMcaEditor -> worldSpec.tree[payload.dimension][payload.type].map(file2chunkList).flatten()
            }
        }
    }

    val defaultStateProvider =
        when (editor) {
            is SingleMcaEditor -> ({ McaEditorState.new(chunks[0]) })
            is GlobalMcaEditor -> ({ McaEditorState.new(worldSpec.playerPos?.second) })
        }

    ChunkSelectorColumn {
        ChunkSelector(editor, chunks, update, openChunkNbt, defaultStateProvider)
    }
}

@Composable
fun ChunkSelectorColumn(content: @Composable ColumnScope.() -> Unit) =
    Column(modifier = Modifier.fillMaxSize(), content = content)

private val file2chunkList: (file: File) -> List<ChunkLocation> = {
    val segments = it.name.split(".")
    val itAnvil = AnvilLocation(segments[1].toInt(), segments[2].toInt())
    McaWorker.loadChunkList(itAnvil, it.readBytes())
}
