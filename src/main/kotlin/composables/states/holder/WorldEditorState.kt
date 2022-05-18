package composables.states.holder

import androidx.compose.runtime.*
import composables.states.editor.world.*

class WorldEditorState (
    val phylum: Phylum,
    val worldSpec: WorldSpecification
)

@Composable
fun rememberWorldEditorState (
    phylum: Phylum = rememberPhylum(),
    worldSpec: WorldSpecification = rememberWorldSpec()
) = remember (phylum, worldSpec) {
    WorldEditorState(phylum, worldSpec)
}
