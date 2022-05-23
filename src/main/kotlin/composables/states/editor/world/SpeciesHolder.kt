package composables.states.editor.world

import androidx.compose.runtime.*
import doodler.anvil.BlockLocation


abstract class SpeciesHolder(
    val ident: String,
    val format: Species.Format,
    val contentType: Species.ContentType,
) {
    enum class Type {
        Single, Multiple
    }
}

class SingleSpeciesHolder(
    ident: String,
    format: Species.Format,
    contentType: Species.ContentType,
    species: NbtSpecies
): SpeciesHolder(ident, format, contentType) {
    val species by mutableStateOf(species)
}

class MultipleSpeciesHolder(
    ident: String,
    format: Species.Format,
    contentType: Species.ContentType,
    val extra: Map<String, Any> = mapOf()
): SpeciesHolder(ident, format, contentType) {

    val species = mutableStateListOf<Species>()
    var selected by mutableStateOf<Species?>(null)

    init {
        val selector = SelectorSpecies("+", mutableStateOf(SelectorState.new(
            extra["playerpos"] as BlockLocation?
        )))
        species.add(selector)
        selected = selector
    }

    fun hasSpecies(ident: String) = species.any { it.ident == ident }

    fun select(species: Species) {
        if (!this.species.any { it.ident == species.ident })
            return

        selected = species
    }

    fun add(species: Species) {
        if (!this.species.any { it == species || it.ident == species.ident })
            this.species.add(species)
    }

    fun remove(species: Species) {
        if (species == selected)
            selected = this.species[this.species.indexOf(species) - 1]

        this.species.remove(species)
    }
}
