package composables.states.holder

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import composables.main.SpeciesHolder
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
    val list: SnapshotStateList<SpeciesHolder>,
    species: MutableState<SpeciesHolder?>
) {
    var species by species
}

@Composable
fun rememberPhylum (
    list: SnapshotStateList<SpeciesHolder> = remember { mutableStateListOf() },
    current: MutableState<SpeciesHolder?> = remember { mutableStateOf(null) }
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
