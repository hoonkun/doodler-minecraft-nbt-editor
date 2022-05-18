package composables.states.editor.world

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList

class Phylum (
    val list: SnapshotStateList<SpeciesHolder> = mutableStateListOf(),
    species: MutableState<SpeciesHolder?> = mutableStateOf(null)
) {
    var species by species
}
