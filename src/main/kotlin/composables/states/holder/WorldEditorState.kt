package composables.states.holder

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import composables.main.EditableHolder
import doodler.file.WorldData

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

class Phylum (
    val list: SnapshotStateList<EditableHolder>,
    current: MutableState<EditableHolder?>
) {
    var current by current
}

@Composable
fun rememberPhylum (
    list: SnapshotStateList<EditableHolder> = remember { mutableStateListOf() },
    current: MutableState<EditableHolder?> = remember { mutableStateOf(null) }
) = remember (list, current) {
    Phylum(list, current)
}

class WorldSpecification (
    tree: MutableState<WorldData?>,
    name: MutableState<String?>
) {
    var tree: WorldData? by tree
    val requireTree get() = tree!!

    var name by name
    val requireName get() = name!!
}

@Composable
fun rememberWorldSpec (
    tree: MutableState<WorldData?> = remember { mutableStateOf(null) },
    name: MutableState<String?> = remember { mutableStateOf(null) }
) = remember (tree, name) {
    WorldSpecification(tree, name)
}
