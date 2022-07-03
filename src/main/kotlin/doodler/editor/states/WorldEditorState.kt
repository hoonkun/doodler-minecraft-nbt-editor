package doodler.editor.states

import androidx.compose.runtime.Stable
import doodler.minecraft.structures.WorldSpecification
import doodler.editor.EditorManager

@Stable
class WorldEditorState (
    val manager: EditorManager,
    val worldSpec: WorldSpecification
): EditorState()
