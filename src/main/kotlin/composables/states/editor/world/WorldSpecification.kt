package composables.states.editor.world

import androidx.compose.runtime.*
import doodler.file.WorldTree

class WorldSpecification (
    tree: MutableState<WorldTree?> = mutableStateOf(null),
    name: MutableState<String?> = mutableStateOf(null)
) {
    var tree: WorldTree? by tree
    val requireTree get() = tree!!

    var name by name
    val requireName get() = name!!
}
