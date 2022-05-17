package composables.states.holder

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.text.input.TextFieldValue
import doodler.anvil.ChunkLocation
import doodler.file.WorldTree
import nbt.tag.CompoundTag
import nbt.AnyTag
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
    tree: MutableState<WorldTree?>,
    name: MutableState<String?>
) {
    var tree: WorldTree? by tree
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
    var initialComposition: Boolean = true,
    selectedChunk: MutableState<ChunkLocation?> = mutableStateOf(null),
    chunkXValue: MutableState<TextFieldValue> = mutableStateOf(TextFieldValue("")),
    chunkZValue: MutableState<TextFieldValue> = mutableStateOf(TextFieldValue("")),
    blockXValue: MutableState<TextFieldValue> = mutableStateOf(TextFieldValue("-")),
    blockZValue: MutableState<TextFieldValue> = mutableStateOf(TextFieldValue("-")),
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
    rootDoodle: NbtDoodle,
    ui: MutableState<DoodleUi>,
    val lazyState: LazyListState,
    val history: DoodleActionHistory = DoodleActionHistory(),
    var initialComposition: Boolean = true
) {
    var ui by ui

    val doodles: DoodleManager = DoodleManager(rootDoodle)

    init {
        rootDoodle.expand()

        val hasOnlyChild = rootDoodle.expandedItems
            .let { children -> children.size == 1 && children[0].let { it is NbtDoodle && it.tag.canHaveChildren } }

        if (initialComposition && hasOnlyChild) (rootDoodle.expandedItems[0] as NbtDoodle).expand()
    }

    fun undo() {
        val action = history.undo() ?: return
        when (action) {
            is DeleteDoodleAction -> undoDelete(action)
        }
    }

    fun redo() {
        val action = history.redo() ?: return
        when (action) {
            is DeleteDoodleAction -> redoDelete(action)
        }
    }

    fun delete() {
        ui.selected.sortBy { doodles.cached.indexOf(it) }

        val deleted = ui.selected.mapNotNull { it.delete() }
        deleted.forEach { it.parent?.update(NbtDoodle.UpdateTarget.VALUE, NbtDoodle.UpdateTarget.INDEX) }

        history.newAction(DeleteDoodleAction(deleted))

        ui.selected.clear()
    }

    private fun undoDelete(action: DeleteDoodleAction) {
        action.deleted.forEach {
            if (it.parent == null) throw Exception("Is this possible??")

            if (!it.parent.expanded)
                it.parent.expand()

            it.parent.create(it)
        }
        action.deleted.map { it.parent }.toSet()
            .forEach { it?.update(NbtDoodle.UpdateTarget.VALUE, NbtDoodle.UpdateTarget.INDEX) }
        ui.selected.clear()
        ui.selected.addAll(action.deleted)
    }

    private fun redoDelete(action: DeleteDoodleAction) {
        action.deleted.forEach {
            it.delete()
        }
        action.deleted.map { it.parent }.toSet()
            .forEach { it?.update(NbtDoodle.UpdateTarget.VALUE, NbtDoodle.UpdateTarget.INDEX) }
        ui.selected.clear()
    }

    companion object {
        fun new(
            rootTag: CompoundTag,
            ui: MutableState<DoodleUi> = mutableStateOf(DoodleUi.new()),
            lazyState: LazyListState = LazyListState()
        ) = NbtState(NbtDoodle(rootTag, -1, -1), ui, lazyState)
    }
}

class DoodleManager(private val root: NbtDoodle) {

    var cached: List<Doodle> = listOf()

    fun create(): List<Doodle> = root.children(true).also { cached = it }

}

abstract class Doodle (
    val depth: Int,
    var index: Int,
    val parent: NbtDoodle?
) {
    abstract val path: String

    abstract fun delete(): Doodle?
}

class NbtDoodle (
    val tag: AnyTag,
    depth: Int,
    index: Int = -1,
    parent: NbtDoodle? = null
): Doodle(depth, index, parent) {

    override val path: String get() = parent?.let {
        when (it.tag.type) {
            TagType.TAG_COMPOUND -> "${it.path}.${tag.name}"
            TagType.TAG_LIST -> "${it.path}[${index}]"
            else -> "" // no-op
        }
    } ?: "root"

    var name by mutableStateOf(tag.name)
        private set
    var value by mutableStateOf(if (this.tag.canHaveChildren) valueSuffix(tag) else tag.valueToString())
        private set

    var expanded = false

    val expandedItems: SnapshotStateList<Doodle> = mutableStateListOf()
    val collapsedItems: MutableList<Doodle> = mutableListOf()

    fun update(vararg targets: UpdateTarget) {
        if (targets.contains(UpdateTarget.NAME))
            name = tag.name
        if (targets.contains(UpdateTarget.VALUE))
            value = if (this.tag.canHaveChildren) valueSuffix(tag) else tag.valueToString()
        if (targets.contains(UpdateTarget.INDEX)) {
            if (expanded) {
                expandedItems.forEach { it.index = expandedItems.indexOf(it) }
            } else {
                collapsedItems.forEach { it.index = collapsedItems.indexOf(it) }
            }
        }
    }

    fun children(root: Boolean = false): List<Doodle> {
        return mutableListOf<Doodle>().apply {
            if (!root) add(this@NbtDoodle)
            addAll(expandedItems.map { if (it is NbtDoodle) it.children() else listOf(it) }.flatten())
        }
    }

    private fun initialDoodles(depth: Int): List<Doodle> {
        return when (tag) {
            is CompoundTag -> tag.doodle(this, depth)
            is ListTag -> tag.doodle(this, depth)
            is ByteArrayTag -> tag.doodle(this, depth)
            is IntArrayTag -> tag.doodle(this, depth)
            is LongArrayTag -> tag.doodle(this, depth)
            else -> throw Exception("this tag is not expandable!")
        }
    }

    fun expand() {
        if (!tag.canHaveChildren) return
        if (expanded) return

        parent?.let { if (!it.expanded) it.expand() }

        expanded = true

        val newDepth = depth + 1

        if (collapsedItems.isEmpty() && expandedItems.isEmpty()) {
            expandedItems.addAll(initialDoodles(newDepth))
        } else {
            expandedItems.addAll(collapsedItems)
            collapsedItems.clear()
        }
    }

    fun collapse() {
        if (!tag.canHaveChildren) return
        if (!expanded) return

        expanded = false

        expandedItems.forEach {
            if (it is NbtDoodle && it.tag.canHaveChildren && it.expanded) it.collapse()
        }

        collapsedItems.addAll(expandedItems)
        expandedItems.clear()
    }

    override fun delete(): NbtDoodle? {
        val parent = parent ?: return null

        when (parent.tag.type) {
            TagType.TAG_COMPOUND -> parent.tag.getAs<CompoundTag>().remove(tag.name)
            TagType.TAG_LIST -> parent.tag.getAs<ListTag>().value.remove(tag)
            else -> { /* no-op */ }
        }

        parent.expandedItems.remove(this)
        parent.collapsedItems.remove(this)

        return this
    }

    fun create(new: Doodle) {
        when (tag.type) {
            TagType.TAG_COMPOUND -> {
                new as? NbtDoodle
                    ?: throw Exception("invalid operation: internal error. expected: NbtDoodle, actual was: ${new.javaClass.name}")

                tag.getAs<CompoundTag>().insert(new.index, new.tag)
            }
            TagType.TAG_LIST -> {
                new as? NbtDoodle
                    ?: throw Exception("invalid operation: internal error. expected: NbtDoodle, actual was: ${new.javaClass.name}")

                val list = tag.getAs<ListTag>()

                if (list.elementsType != new.tag.type)
                    throw Exception("invalid operation: tag type mismatch. expected: ${tag.type.name}, actual was: ${new.tag.type.name}")
                else list.value.add(new.index, new.tag)
            }
            TagType.TAG_BYTE_ARRAY -> {
                new as? ValueDoodle
                    ?: throw Exception("invalid operation: internal error. expected: ValueDoodle, actual was: ${new.javaClass.name}")

                val value = new.value.toByteOrNull()
                    ?: throw Exception("invalid operation: value type mismatch. expected: Byte, actual was: ${new.value}")

                val array = tag.getAs<ByteArrayTag>()
                array.value = array.value.toMutableList().apply { add(new.index, value) }.toByteArray()
            }
            TagType.TAG_INT_ARRAY -> {
                new as? ValueDoodle
                    ?: throw Exception("invalid operation: internal error. expected: ValueDoodle, actual was: ${new.javaClass.name}")

                val value = new.value.toIntOrNull()
                    ?: throw Exception("invalid operation: value type mismatch. expected: Byte, actual was: ${new.value}")

                val array = tag.getAs<IntArrayTag>()
                array.value = array.value.toMutableList().apply { add(new.index, value) }.toIntArray()
            }
            TagType.TAG_LONG_ARRAY -> {
                new as? ValueDoodle
                    ?: throw Exception("invalid operation: internal error. expected: ValueDoodle, actual was: ${new.javaClass.name}")

                val value = new.value.toLongOrNull()
                    ?: throw Exception("invalid operation: value type mismatch. expected: Byte, actual was: ${new.value}")

                val array = tag.getAs<LongArrayTag>()
                array.value = array.value.toMutableList().apply { add(new.index, value) }.toLongArray()
            }
            else -> throw Exception("invalid operation: ${tag.javaClass.name} cannot own child tags.")
        }

        if (expanded) {
            if (new.index == -1) expandedItems.add(new)
            else expandedItems.add(new.index, new)
        } else {
            if (new.index == -1) collapsedItems.add(new)
            else collapsedItems.add(new.index, new)
        }
    }

    fun yank() {

    }

    fun edit() {

    }

    enum class UpdateTarget {
        NAME, VALUE, INDEX
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
    return this.value.mapIndexed { index, value -> NbtDoodle(value, depth, index, parent) }
}

fun ListTag.doodle(parent: NbtDoodle, depth: Int): List<Doodle> {
    return this.value.mapIndexed { index, value -> NbtDoodle(value, depth, index, parent) }
}

fun ByteArrayTag.doodle(parent: NbtDoodle, depth: Int): List<Doodle> {
    return this.value.mapIndexed { index, value ->
        ValueDoodle("$value", depth, index, parent)
    }
}

fun IntArrayTag.doodle(parent: NbtDoodle, depth: Int): List<Doodle> {
    return this.value.mapIndexed { index, value ->
        ValueDoodle("$value", depth, index, parent)
    }
}

fun LongArrayTag.doodle(parent: NbtDoodle, depth: Int): List<Doodle> {
    return this.value.mapIndexed { index, value ->
        ValueDoodle("$value", depth, index, parent)
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
    depth: Int,
    index: Int,
    parent: NbtDoodle? = null
): Doodle(depth, index, parent) {

    override val path: String
        get() = parent?.let {
            when (it.tag.type) {
                TagType.TAG_BYTE_ARRAY,
                TagType.TAG_INT_ARRAY,
                TagType.TAG_LONG_ARRAY -> "${it.path}[${index}]"
                else -> "" // no-op
            }
        } ?: "" // no-op

    override fun delete(): ValueDoodle? {
        val parent = parent ?: return null

        when (parent.tag.type) {
            TagType.TAG_BYTE_ARRAY -> {
                val tag = parent.tag.getAs<ByteArrayTag>()
                tag.value = tag.value.toMutableList().apply { removeAt(index) }.toByteArray()
            }
            TagType.TAG_INT_ARRAY -> {
                val tag = parent.tag.getAs<IntArrayTag>()
                tag.value = tag.value.toMutableList().apply { removeAt(index) }.toIntArray()
            }
            TagType.TAG_LONG_ARRAY -> {
                val tag = parent.tag.getAs<LongArrayTag>()
                tag.value = tag.value.toMutableList().apply { removeAt(index) }.toLongArray()
            }
            else -> { /* no-op */ }
        }

        parent.expandedItems.remove(this)
        parent.collapsedItems.remove(this)

        return this
    }

}

class DoodleUi (
    val selected: SnapshotStateList<Doodle>,
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
            selected: SnapshotStateList<Doodle> = mutableStateListOf(),
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
    tree: MutableState<WorldTree?> = remember { mutableStateOf(null) },
    name: MutableState<String?> = remember { mutableStateOf(null) }
) = remember (tree, name) {
    WorldSpecification(tree, name)
}

class DoodleActionHistory(
    private val undoStack: MutableList<DoodleAction> = mutableStateListOf(),
    private val redoStack: MutableList<DoodleAction> = mutableStateListOf()
) {
    var flags by mutableStateOf(Flags(canBeUndo = undoStack.isNotEmpty(), canBeRedo = redoStack.isNotEmpty()))

    fun newAction(action: DoodleAction) {
        redoStack.clear()
        undoStack.add(action)
        updateFlags()
    }

    fun undo(): DoodleAction? {
        if (!flags.canBeUndo) return null
        redoStack.add(undoStack.removeLast())
        updateFlags()
        return redoStack.last()
    }

    fun redo(): DoodleAction? {
        if (!flags.canBeRedo) return null
        undoStack.add(redoStack.removeLast())
        updateFlags()
        return undoStack.last()
    }

    private fun updateFlags() {
        flags = Flags(canBeUndo = undoStack.isNotEmpty(), canBeRedo = redoStack.isNotEmpty())
    }

    class Flags (
        val canBeUndo: Boolean,
        val canBeRedo: Boolean
    )

}

abstract class DoodleAction

class DeleteDoodleAction(
    val deleted: List<Doodle>
): DoodleAction()
