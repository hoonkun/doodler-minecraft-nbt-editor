package doodler.editor

import doodler.minecraft.structures.WorldSpecification
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember


class WorldEditorState (
    val manager: EditorManager,
    val worldSpec: WorldSpecification
)

@Composable
fun rememberWorldEditorState (
    worldPath: String,
    manager: EditorManager = EditorManager(),
    worldSpec: WorldSpecification = WorldSpecification(worldPath)
) = remember (manager, worldSpec) {
    WorldEditorState(manager, worldSpec)
}
