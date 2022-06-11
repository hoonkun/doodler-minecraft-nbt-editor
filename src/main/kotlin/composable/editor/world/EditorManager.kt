package composable.editor.world

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import composable.editor.NbtEditor
import doodler.doodle.structures.TagDoodle
import doodler.editor.*
import doodler.editor.states.NbtEditorState
import doodler.exceptions.InternalAssertionException
import doodler.file.toStateFile
import doodler.minecraft.McaWorker
import doodler.minecraft.structures.ChunkLocation
import doodler.minecraft.structures.McaFileType
import java.io.File

@Composable
fun BoxScope.EditorManager(
    state: WorldEditorState
) {

    val manager = state.manager

    val openChunkNbt: (ChunkLocation, File) -> Unit = openChunkNbt@ { location, file ->
        val ident = AnvilNbtEditor.ident(file, location)
        if (manager.hasItem(ident)) {
            manager.select(ident)
            return@openChunkNbt
        }

        val root = McaWorker.loadChunk(location, file.readBytes()) ?: return@openChunkNbt
        manager.open(
            AnvilNbtEditor(
                anvil = file,
                location = location,
                state = NbtEditorState(
                    root = TagDoodle(root, -1, null),
                    file = file.toStateFile(),
                    type = McaFileType(location)
                )
            )
        )
    }

    val updateGlobal: (McaPayload) -> Unit = updateGlobal@ {
        val editor = manager[GlobalMcaEditor.Identifier] ?: return@updateGlobal
        if (editor !is GlobalMcaEditor)
            throw InternalAssertionException("GlobalMcaEditor", editor.javaClass.simpleName)

        editor.payload = it
    }

    EditorManagerRoot {
        EditorTabGroup(
            manager = manager,
            onSelectTab = { manager.select(it) },
            onCloseTab = { manager.close(it) }
        )
        Editors {
            val selected = manager.selected

            if (selected is NbtEditor) NbtEditor(selected)
            if (selected is McaEditor<*>) McaEditor(selected, state.worldSpec, openChunkNbt, updateGlobal)
        }
    }

}

@Composable
fun BoxScope.EditorManagerRoot(content: @Composable ColumnScope.() -> Unit) =
    Column(modifier = Modifier.fillMaxSize().zIndex(100f), content = content)

@Composable
fun ColumnScope.Editors(content: @Composable BoxScope.() -> Unit) =
    Box(modifier = Modifier.fillMaxWidth().weight(1f), content = content)