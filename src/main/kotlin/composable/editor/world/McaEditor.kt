package composable.editor.world

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import doodler.editor.GlobalMcaEditor
import doodler.editor.GlobalUpdateRequest
import doodler.editor.McaEditor
import doodler.editor.SingleMcaEditor
import doodler.editor.states.McaEditorState
import doodler.exceptions.DoodleException
import doodler.minecraft.McaWorker
import doodler.minecraft.structures.*
import java.io.File


@Composable
fun <K> BoxScope.McaEditor(
    editor: McaEditor<K>,
    worldSpec: WorldSpecification,
    openChunkNbt: (ChunkLocation, File) -> Unit,
    update: (GlobalUpdateRequest) -> Unit
) {
    val terrainCache = remember { mutableStateMapOf<CachedTerrainInfo, ImageBitmap>() }
    val yRangeCache = remember { mutableMapOf<AnvilLocation, List<IntRange>>() }

    when (editor) {
        is SingleMcaEditor -> SingleMcaEditor(editor, openChunkNbt)
        is GlobalMcaEditor -> GlobalMcaEditor(editor, worldSpec, openChunkNbt, update)
    }
}

@Composable
fun BoxScope.SingleMcaEditor(
    editor: SingleMcaEditor,
    openChunkNbt: (ChunkLocation, File) -> Unit
) {

    val chunks by remember {
        derivedStateOf {
            McaWorker.loadChunkList(
                location = editor.payload.location,
                bytes = editor.payload.file.readBytes()
            )
        }
    }

    ChunkSelectorColumn { SingleChunkSelector(editor, chunks, openChunkNbt) }

}

@Composable
fun BoxScope.GlobalMcaEditor(
    editor: GlobalMcaEditor,
    worldSpec: WorldSpecification,
    openChunkNbt: (ChunkLocation, File) -> Unit,
    update: (GlobalUpdateRequest) -> Unit
) {

    val chunks by remember {
        derivedStateOf {
            val payload = editor.payload
            if (payload == null) {
                val (dimension, block) = worldSpec.playerPos
                    ?: throw DoodleException("Internal Error", null, "Cannot read player data from level.dat")

                val type = McaType.TERRAIN
                val anvil = block.toChunkLocation().toAnvilLocation()
                val file = worldSpec.tree[dimension][type.pathName].find { it.name == "r.${anvil.x}.${anvil.z}.mca" }
                    ?: throw DoodleException("Internal Error", null, "Cannot find region file which player exists")

                val chunks = worldSpec.tree[dimension][type.pathName].map(file2chunkList).flatten()

                editor.payload = GlobalUpdateRequest(
                    dimension = dimension,
                    type = type,
                    location = anvil,
                    file = file
                )

                return@derivedStateOf chunks
            }

            worldSpec.tree[payload.dimension][payload.type.pathName].map(file2chunkList).flatten()
        }
    }

    val defaultStateProvider = { McaEditorState.new(worldSpec.playerPos?.second) }

    ChunkSelectorColumn { GlobalChunkSelector(editor, chunks, update, openChunkNbt, defaultStateProvider) }

}

@Composable
fun ChunkSelectorColumn(content: @Composable ColumnScope.() -> Unit) =
    Column(modifier = Modifier.fillMaxSize(), content = content)

private val file2chunkList: (file: File) -> List<ChunkLocation> = {
    val segments = it.name.split(".")
    val itAnvil = AnvilLocation(segments[1].toInt(), segments[2].toInt())
    McaWorker.loadChunkList(itAnvil, it.readBytes())
}
