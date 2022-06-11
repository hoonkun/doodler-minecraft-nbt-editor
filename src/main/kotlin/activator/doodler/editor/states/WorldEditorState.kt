package activator.doodler.editor.states

import androidx.compose.runtime.*
import activator.doodler.editor.EditorManager
import doodler.minecraft.structures.WorldSpecification

class WorldEditorState (
    val manager: EditorManager,
    val worldSpec: WorldSpecification
)

@Composable
fun rememberWorldEditorState (
    worldName: String,
    manager: EditorManager = EditorManager(),
    worldSpec: WorldSpecification = WorldSpecification(worldName)
) = remember (manager, worldSpec) {
    WorldEditorState(manager, worldSpec)
}
