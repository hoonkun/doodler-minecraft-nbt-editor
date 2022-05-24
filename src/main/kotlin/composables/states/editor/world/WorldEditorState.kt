package composables.states.editor.world

import androidx.compose.runtime.*

class WorldEditorState (
    val editor: Editor,
    val worldSpec: WorldSpecification
)

@Composable
fun rememberWorldEditorState (
    editor: Editor = Editor(),
    worldSpec: WorldSpecification = WorldSpecification()
) = remember (editor, worldSpec) {
    WorldEditorState(editor, worldSpec)
}
