package composables.states.editor.world

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList

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