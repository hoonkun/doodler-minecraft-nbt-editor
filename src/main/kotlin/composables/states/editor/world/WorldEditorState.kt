package composables.states.editor.world

import androidx.compose.runtime.*

class WorldEditorState (
    val phylum: Phylum,
    val worldSpec: WorldSpecification
)

@Composable
fun rememberWorldEditorState (
    phylum: Phylum = Phylum(),
    worldSpec: WorldSpecification = WorldSpecification()
) = remember (phylum, worldSpec) {
    WorldEditorState(phylum, worldSpec)
}
