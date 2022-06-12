package composable.editor.world

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import doodler.editor.*
import doodler.editor.states.McaEditorState
import doodler.minecraft.McaWorker
import doodler.minecraft.structures.*
import java.io.File


@Composable
fun BoxScope.McaEditor(
    editor: McaEditor<*>,
    cache: TerrainCache,
    worldSpec: WorldSpecification,
    openChunkNbt: (ChunkLocation, File) -> Unit,
    update: (McaPayload) -> Unit
) {

    val chunks by remember(editor) {
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
        ChunkSelector(
            editor = editor,
            terrains = worldSpec.tree[editor.payload.dimension][McaType.Terrain],
            chunks = chunks,
            terrainCache = cache,
            update = update,
            openChunkNbt = openChunkNbt,
            defaultStateProvider = defaultStateProvider
        )
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
