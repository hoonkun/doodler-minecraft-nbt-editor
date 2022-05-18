package composables.states.editor.world

import androidx.compose.runtime.*
import doodler.file.WorldTree

class WorldSpecification (
    tree: MutableState<WorldTree?>,
    name: MutableState<String?>
) {
    var tree: WorldTree? by tree
    val requireTree get() = tree!!

    var name by name
    val requireName get() = name!!
}

@Composable
fun rememberWorldSpec (
    tree: MutableState<WorldTree?> = remember { mutableStateOf(null) },
    name: MutableState<String?> = remember { mutableStateOf(null) }
) = remember (tree, name) {
    WorldSpecification(tree, name)
}
