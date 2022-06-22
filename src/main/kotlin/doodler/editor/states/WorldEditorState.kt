package doodler.editor.states

import doodler.minecraft.structures.WorldSpecification
import doodler.editor.EditorManager


class WorldEditorState (
    val manager: EditorManager,
    val worldSpec: WorldSpecification
): EditorState()
