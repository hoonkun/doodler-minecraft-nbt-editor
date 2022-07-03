package composable.editor.world

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import composable.editor.NbtEditor
import doodler.editor.*
import doodler.editor.states.*
import doodler.types.pass

@Composable
fun BoxScope.EditorManager(
    state: WorldEditorState
) {

    val manager = state.manager
    val worldSpec = state.worldSpec

    EditorManagerRoot {
        EditorTabGroup(
            state = state,
            onSelectTab = { manager.select(it) },
            onCloseTab = { manager.close(it) }
        )
        Editors {
            when (val selected = manager.selected) {
                is NbtEditor -> NbtEditor(selected, worldSpec)
                is McaEditor<*> -> McaEditor(manager, selected, manager.cache, worldSpec)
                else -> pass
            }
        }
    }

}

@Composable
fun BoxScope.EditorManagerRoot(content: @Composable ColumnScope.() -> Unit) =
    Column(modifier = Modifier.fillMaxSize().zIndex(100f), content = content)

@Composable
fun ColumnScope.Editors(content: @Composable BoxScope.() -> Unit) =
    Box(modifier = Modifier.fillMaxWidth().weight(1f), content = content)