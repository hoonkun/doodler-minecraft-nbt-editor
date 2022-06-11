package composable.editor.world

import androidx.compose.runtime.Composable
import doodler.editor.GlobalMcaEditor
import doodler.editor.GlobalUpdateRequest
import doodler.editor.SingleMcaEditor
import doodler.editor.states.McaEditorState
import doodler.minecraft.structures.ChunkLocation
import java.io.File

@Composable
fun SingleChunkSelector(
    editor: SingleMcaEditor,
    chunks: List<ChunkLocation>,
    openChunkNbt: (ChunkLocation, File) -> Unit
) {

    val state = editor.state { McaEditorState.new(chunks[0]) }



}

@Composable
fun GlobalChunkSelector(
    editor: GlobalMcaEditor,
    chunks: List<ChunkLocation>,
    update: (GlobalUpdateRequest) -> Unit,
    openChunkNbt: (ChunkLocation, File) -> Unit,
    defaultStateProvider: () -> McaEditorState
) {

    val state = editor.state(defaultStateProvider) ?: return

}