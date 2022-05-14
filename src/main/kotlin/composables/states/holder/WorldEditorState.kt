package composables.states.holder

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.text.input.TextFieldValue
import doodler.anvil.ChunkLocation
import doodler.file.WorldData
import nbt.AnyTag
import nbt.Tag
import nbt.TagType
import nbt.tag.*

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
    val extra: Map<String, String> = mapOf()
): SpeciesHolder(ident, format, contentType) {

    val species = mutableStateListOf<Species>()
    var selected by mutableStateOf<Species?>(null)

    init {
        val selector = SelectorSpecies("+", mutableStateOf(SelectorState()))
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

abstract class Species (
    val ident: String
) {
    enum class Format {
        DAT, MCA
    }

    enum class ContentType(val displayName: String, val description: String) {
        LEVEL("World Data", "level.dat"),
        PLAYER("Players", "playerdata/"),
        STATISTICS("Statistics", "stats/"),
        ADVANCEMENTS("Advancements", "advancements/"),
        OTHERS("Others", "data/"),

        TERRAIN("Terrain", "region/"),
        ENTITY("Entities", "entities/"),
        POI("Work Block Owners", "poi/")
    }
}

class SelectorSpecies (
    ident: String,
    state: MutableState<SelectorState>
): Species(ident) {
    var state by state
}

class SelectorState (
    selectedChunk: MutableState<ChunkLocation?> = mutableStateOf(null),
    chunkXValue: MutableState<TextFieldValue> = mutableStateOf(TextFieldValue("")),
    chunkZValue: MutableState<TextFieldValue> = mutableStateOf(TextFieldValue("")),
    blockXValue: MutableState<TextFieldValue> = mutableStateOf(TextFieldValue("")),
    blockZValue: MutableState<TextFieldValue> = mutableStateOf(TextFieldValue("")),
    isChunkXValid: MutableState<Boolean> = mutableStateOf(false),
    isChunkZValid: MutableState<Boolean> = mutableStateOf(false)
) {
    var selectedChunk by selectedChunk
    var chunkXValue by chunkXValue
    var chunkZValue by chunkZValue
    var blockXValue by blockXValue
    var blockZValue by blockZValue
    var isChunkXValid by isChunkXValid
    var isChunkZValid by isChunkZValid
}

class NbtSpecies (
    ident: String,
    val root: CompoundTag,
    state: MutableState<NbtState>
): Species(ident) {
    var state by state

    val hasChanges = false

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is NbtSpecies) return false

        return other.ident == this.ident
    }

    override fun hashCode(): Int {
        return this.ident.hashCode()
    }
}

class NbtState (
    val doodles: SnapshotStateList<Doodle>,
    ui: MutableState<DoodleUi>,
    val lazyState: LazyListState
) {
    var ui by ui

    companion object {
        fun new(
            doodles: SnapshotStateList<Doodle> = mutableStateListOf(),
            ui: MutableState<DoodleUi> = mutableStateOf(DoodleUi.new()),
            lazyState: LazyListState = LazyListState()
        ) = NbtState(doodles, ui, lazyState)
    }
}

abstract class Doodle (
    val depth: Int,
    val index: Int,
    val parentTag: NbtDoodle?
) {
    abstract val path: String
}

class NbtDoodle (
    val tag: AnyTag,
    depth: Int,
    index: Int = -1,
    parentTag: NbtDoodle? = null
): Doodle(depth, index, parentTag) {
    
    override val path: String get() = tag.path ?: "null"

    val type = tag.type
    val canHaveChildren = Tag.canHaveChildren(type)

    val name = tag.name
    val value = if (this.canHaveChildren) valueSuffix(tag) else tag.valueToString()

    var expanded = false
    private var children: List<Doodle>? = null

    fun expand(): List<Doodle> {
        expanded = true
        val newDepth = depth + 1
        val result = when (tag) {
            is CompoundTag -> tag.doodle(this, newDepth)
            is ListTag -> tag.doodle(this, newDepth)
            is ByteArrayTag -> tag.doodle(this, newDepth, path)
            is IntArrayTag -> tag.doodle(this, newDepth, path)
            is LongArrayTag -> tag.doodle(this, newDepth, path)
            else -> throw Exception("this tag is not expandable!")
        }
        children = result
        return result
    }

    fun collapse(): Int {
        expanded = false
        val localChildren = children
        val collapsableChildren = if (localChildren == null) 0 else {
            var count = 0
            localChildren.forEach {
                if (it is NbtDoodle && it.canHaveChildren && it.expanded) count += it.collapse()
            }
            count
        }
        return when (tag) {
            is CompoundTag -> tag.value.size
            is ListTag -> tag.value.size
            is ByteArrayTag -> tag.value.size
            is IntArrayTag -> tag.value.size
            is LongArrayTag -> tag.value.size
            else -> throw Exception("this tag is not collapsable!")
        } + collapsableChildren
    }
    
    companion object {
    
        private fun valueSuffix(tag: AnyTag): String {
            return when (tag) {
                is CompoundTag -> "${size(tag.value.size)} child ${tags(tag.value.size)}"
                is ListTag -> "${size(tag.value.size)} ${elementsType(tag.elementsType)}${tags(tag.value.size)} inside"
                is ByteArrayTag -> "${size(tag.value.size)} ${entries(tag.value.size)}"
                is IntArrayTag -> "${size(tag.value.size)} ${entries(tag.value.size)}"
                is LongArrayTag -> "${size(tag.value.size)} ${entries(tag.value.size)}"
                else -> throw Exception("this tag does not have iterable value!")
            }
        }
    
        private fun size(size: Int): String = if (size > 0) "$size" else "no"
    
        private fun tags(size: Int): String = if (size != 1) "tags" else "tag"
    
        private fun entries(size: Int): String = if (size != 1) "entries" else "entry"
    
        private fun elementsType(type: TagType): String = if (type == TagType.TAG_END) "" else "${type.displayName()} "

    }

}

fun display(dimension: String): String {
    return when (dimension) {
        "" -> "Overworld"
        "DIM-1" -> "Nether"
        "DIM1" -> "TheEnd"
        else -> "Unknown"
    }
}

fun CompoundTag.doodle(parent: NbtDoodle?, depth: Int): List<Doodle> {
    return this.value.values.map { NbtDoodle(it, depth, parentTag = parent) }
}

fun ListTag.doodle(parent: NbtDoodle, depth: Int): List<Doodle> {
    return this.value.mapIndexed { index, value -> NbtDoodle(value, depth, index, parent) }
}

fun ByteArrayTag.doodle(parent: NbtDoodle, depth: Int, parentKey: String): List<Doodle> {
    return this.value.mapIndexed { index, value ->
        ValueDoodle("$value", "$parentKey[$index]", depth, index, parent)
    }
}

fun IntArrayTag.doodle(parent: NbtDoodle, depth: Int, parentKey: String): List<Doodle> {
    return this.value.mapIndexed { index, value ->
        ValueDoodle("$value", "$parentKey[$index]", depth, index, parent)
    }
}

fun LongArrayTag.doodle(parent: NbtDoodle, depth: Int, parentKey: String): List<Doodle> {
    return this.value.mapIndexed { index, value ->
        ValueDoodle("$value", "$parentKey[$index]", depth, index, parent)
    }
}

fun TagType.displayName(): String {
    return when (this) {
        TagType.TAG_END -> "noop"
        TagType.TAG_COMPOUND -> "compound"
        TagType.TAG_LIST -> "list"
        TagType.TAG_LONG_ARRAY -> "long array"
        TagType.TAG_INT_ARRAY -> "int array"
        TagType.TAG_BYTE_ARRAY -> "byte array"
        TagType.TAG_STRING -> "string"
        TagType.TAG_SHORT -> "short"
        TagType.TAG_LONG -> "long"
        TagType.TAG_INT -> "int"
        TagType.TAG_FLOAT -> "float"
        TagType.TAG_BYTE -> "byte"
        TagType.TAG_DOUBLE -> "double"
    }
}

class ValueDoodle (
    val value: String,
    override val path: String,
    depth: Int,
    index: Int,
    parentTag: NbtDoodle? = null
): Doodle(depth, index, parentTag)

class DoodleUi (
    val selected: SnapshotStateList<Doodle?>,
    pressed: MutableState<Doodle?>,
    focusedDirectly: MutableState<Doodle?>,
    focusedTree: MutableState<Doodle?>,
    focusedTreeView: MutableState<Doodle?>
) {
    var pressed by pressed
    var focusedDirectly by focusedDirectly
    var focusedTree by focusedTree
    var focusedTreeView by focusedTreeView

    companion object {
        fun new(
            selected: SnapshotStateList<Doodle?> = mutableStateListOf(),
            pressed: MutableState<Doodle?> = mutableStateOf(null),
            focusedDirectly: MutableState<Doodle?> = mutableStateOf(null),
            focusedTree: MutableState<Doodle?> = mutableStateOf(null),
            focusedTreeView: MutableState<Doodle?> = mutableStateOf(null)
        ) = DoodleUi(
            selected, pressed, focusedDirectly, focusedTree, focusedTreeView
        )
    }

    fun press(target: Doodle) {
        pressed = target
    }

    fun unPress(target: Doodle) {
        if (pressed == target) pressed = null
    }

    fun getLastSelected(): Doodle? = if (selected.isEmpty()) null else selected.last()

    fun addRangeToSelected(targets: List<Doodle>) {
        selected.addAll(targets.filter { !selected.contains(it) })
    }

    fun addToSelected(target: Doodle) {
        if (!selected.contains(target)) selected.add(target)
    }

    fun removeFromSelected(target: Doodle) {
        if (selected.contains(target)) selected.remove(target)
    }

    fun setSelected(target: Doodle) {
        selected.clear()
        selected.add(target)
    }

    fun focusDirectly(target: Doodle) {
        if (focusedDirectly != target) focusedDirectly = target
    }

    fun unFocusDirectly(target: Doodle) {
        if (focusedDirectly == target) focusedDirectly = null
    }

    fun focusTreeView(target: Doodle) {
        if (focusedTreeView != target) focusedTreeView = target
    }

    fun unFocusTreeView(target: Doodle) {
        if (focusedTreeView == target) focusedTreeView = null
    }

    fun focusTree(target: Doodle) {
        if (focusedTree != target) focusedTree = target
    }

    fun unFocusTree(target: Doodle) {
        if (focusedTree == target) focusedTree = null
    }
}

@Composable
fun rememberWorldSpec (
    tree: MutableState<WorldData?> = remember { mutableStateOf(null) },
    name: MutableState<String?> = remember { mutableStateOf(null) }
) = remember (tree, name) {
    WorldSpecification(tree, name)
}
