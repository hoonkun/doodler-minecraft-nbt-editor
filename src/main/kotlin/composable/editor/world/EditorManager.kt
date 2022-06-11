package composable.editor.world

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import composable.editor.NbtEditor
import doodler.editor.NbtEditor
import doodler.editor.WorldEditorState

@Composable
fun BoxScope.EditorManager(
    state: WorldEditorState
) {

    val manager = state.manager

    EditorManagerRoot {
        EditorTabGroup(
            manager = manager,
            onSelectTab = { manager.select(it) },
            onCloseTab = { manager.close(it) }
        )
        Editors {
            val selected = manager.selected
            if (selected is NbtEditor) NbtEditor(selected)
        }
    }

}

@Composable
fun BoxScope.EditorManagerRoot(content: @Composable ColumnScope.() -> Unit) =
    Column(modifier = Modifier.fillMaxSize().zIndex(100f), content = content)

@Composable
fun ColumnScope.Editors(content: @Composable BoxScope.() -> Unit) =
    Box(modifier = Modifier.fillMaxWidth().weight(1f), content = content)