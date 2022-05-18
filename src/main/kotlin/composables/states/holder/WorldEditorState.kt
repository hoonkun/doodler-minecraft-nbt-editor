package composables.states.holder

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.text.input.TextFieldValue
import doodler.anvil.ChunkLocation
import doodler.file.WorldTree
import doodler.nbt.tag.*
import doodler.nbt.AnyTag
import doodler.nbt.TagType

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

    val clipboards: MutableList<Pair<PasteTarget, List<Doodle>>> = mutableListOf()
    val pasteTarget: PasteTarget
        get() {
            val selected = ui.selected
            val attributes = mutableSetOf<TagAttribute>()
            val tagTypes = mutableSetOf<TagType>()

            attributes.addAll(
                selected.map {
                    if (it is ValueDoodle) TagAttribute.VALUE
                    else if (it is NbtDoodle && it.tag.name == null) TagAttribute.UNNAMED
                    else TagAttribute.NAMED
                }
            )

            tagTypes.addAll(selected.mapNotNull {
                when (it) {
                    is NbtDoodle -> it.tag.type
                    is ValueDoodle -> it.parent?.tag?.type
                }
            })

            if (attributes.size != 1) return CannotBePasted
            val attribute = attributes.toList()[0]

            return if (attribute == TagAttribute.NAMED) {
                CanBePastedIntoCompound
            } else if (attribute == TagAttribute.UNNAMED && tagTypes.size == 1) {
                CanBePastedIntoList(tagTypes.toList()[0])
            } else if (attribute == TagAttribute.VALUE && tagTypes.size == 1) {
                CanBePastedIntoArray(tagTypes.toList()[0])
            } else {
                CannotBePasted
            }
        }

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
            is PasteDoodleAction -> undoPaste(action)
        }
    }

    fun redo() {
        val action = history.redo() ?: return
        when (action) {
            is DeleteDoodleAction -> redoDelete(action)
            is PasteDoodleAction -> redoPaste(action)
        }
    }

    // TODO: 복사 및 붙혀넣기가 가능한 조합이 생각보다 복잡함.
    //  - 이름이 없는 태그와 있는 태그, 혹은 값 중 둘 이상이 섞여있음: 복사 불가
    //  - 아래의 경우에서 '값'이 하나라도 있으면 복사불가.
    //    - 태그의 타입들이 서로 다르고 이름이 없는 태그가 한 개 이상 있음: 복사 불가
    //    - 태그의 타입들이 서로 다르나, 이름이 있는 태그들만 있음: 복사 가능, Compound 태그에 붙혀넣을 수 있음
    //    - 태그의 타입들이 서로 같고, 이름이 있는 태그들만 있음: 복사 가능, Compound 태그에 붙혀넣을 수 있음
    //    - 태그의 타입들이 서로 같고, 이름이 없는 태그들만 있음: 복사 가능, 해당 타입을 가진 List 태그에 붙혀넣을 수 있음
    //  - 값 뿐임: 복사 가능, 해당 타입을 가진 Array 태그에 붙혀넣을 수 있음
    //  추가로, Compound 에서는 큰 의미가 없으나 List 에 붙혀넣었을 때는 순서를 변경할 수 있어야할 듯 함.
    //  리스트 태그의 아이템이나 배열 태그의 아이템을 선택했을 경우 추가적인 액션 패널을 보여줘야할 것 같음.
    fun yank() {
        val target = pasteTarget
        if (target == CannotBePasted) return

        if (clipboards.size >= 5) clipboards.removeAt(0)
        clipboards.add(
            Pair(target, mutableListOf<Doodle>().apply { addAll(ui.selected.map { it.clone(null) }) })
        )
    }

    fun paste() {
        if (ui.selected.isEmpty()) throw Exception("invalid operation: no selected elements")
        if (ui.selected.size > 1) throw Exception("invalid operation: cannot be paste in multiple elements at once.")

        val selected = ui.selected[0]
        if (selected !is NbtDoodle)
            throw Exception("invalid operation: expected NbtDoodle, actual was ${selected.javaClass.name}")

        val (target, doodles) = clipboards.last()
        val pasteTags = {
            val created = doodles.map { selected.create(it.clone(selected), false) }
            val action = PasteDoodleAction(created)
            history.newAction(action)

            ui.selected.clear()
            ui.selected.addAll(created)
        }

        when (target) {
            CannotBePasted -> throw Exception("invalid operation: internal error. cannot be pasted.")
            CanBePastedIntoCompound -> {
                if (selected.tag.type != TagType.TAG_COMPOUND)
                    throw Exception("invalid operation: these tags can only be pasted into: CompoundTag.")
            }
            is CanBePastedIntoList -> {
                if (selected.tag.type != TagType.TAG_LIST)
                    throw Exception("invalid operation: these tags can only be pasted into: ListTag.")

                val listTag = selected.tag.getAs<ListTag>()
                if (target.elementsType != listTag.elementsType)
                    throw Exception("invalid operation: tag type mismatch. only ${listTag.elementsType} can be added, given was: ${target.elementsType}")
            }
            is CanBePastedIntoArray -> {
                if (selected.tag.type != target.arrayTagType)
                    throw Exception("invalid operation: these values can only be pasted into: ${target.arrayTagType}")
            }
        }

        pasteTags()
    }

    fun pasteEnabled(content: Pair<PasteTarget, List<Doodle>>? = clipboards.lastOrNull()): Boolean {
        if (content == null) return false

        if (ui.selected.isEmpty()) return false
        if (ui.selected.size > 1) return false

        val selected = ui.selected[0]
        if (selected !is NbtDoodle) return false

        val (target) = content

        return when (target) {
            CannotBePasted -> false
            CanBePastedIntoCompound -> selected.tag.type == TagType.TAG_COMPOUND
            is CanBePastedIntoList -> {
                // TODO: 이거, ListTag.elementsType 이 TAG_END 일 경우에는(빈 리스트일 경우) 그냥 집어넣고 elementsType 을 바꾸는건 어떨지?
                selected.tag.type == TagType.TAG_LIST && target.elementsType == selected.tag.getAs<ListTag>().elementsType
            }
            is CanBePastedIntoArray -> selected.tag.type == target.arrayTagType
        }
    }

    fun delete() {
        ui.selected.sortBy { doodles.cached.indexOf(it) }

        val deleted = ui.selected.mapNotNull { it.delete() }
        deleted.forEach { it.parent?.update(NbtDoodle.UpdateTarget.VALUE, NbtDoodle.UpdateTarget.INDEX) }

        history.newAction(DeleteDoodleAction(deleted))

        ui.selected.clear()
    }

    private fun undoPaste(action: PasteDoodleAction) {
        delete(action.created)
    }

    private fun undoDelete(action: DeleteDoodleAction) {
        create(action.deleted)
    }

    private fun create(targets: List<Doodle>) {
        targets.forEach {
            val eachParent = it.parent ?: throw Exception("Is this possible??")

            if (!eachParent.expanded)
                eachParent.expand()

            eachParent.create(it)
        }
        targets.map { it.parent }.toSet()
            .forEach { it?.update(NbtDoodle.UpdateTarget.VALUE, NbtDoodle.UpdateTarget.INDEX) }
        ui.selected.clear()
        ui.selected.addAll(targets)
    }

    private fun redoPaste(action: PasteDoodleAction) {
        create(action.created)
    }

    private fun redoDelete(action: DeleteDoodleAction) {
        delete(action.deleted)
    }

    private fun delete(targets: List<Doodle>) {
        targets.forEach {
            it.delete()
        }
        targets.map { it.parent }.toSet()
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

    private enum class TagAttribute {
        NAMED, UNNAMED, VALUE
    }

    sealed class PasteTarget

    object CannotBePasted: PasteTarget()

    object CanBePastedIntoCompound: PasteTarget()

    data class CanBePastedIntoList(val elementsType: TagType): PasteTarget()

    data class CanBePastedIntoArray(val arrayTagType: TagType): PasteTarget()

}

class DoodleManager(private val root: NbtDoodle) {

    var cached: List<Doodle> = listOf()

    fun create(): List<Doodle> = root.children(true).also { cached = it }

}

sealed class Doodle (
    var depth: Int,
    var index: Int,
    var parent: NbtDoodle?
) {
    abstract val path: String

    abstract fun delete(): Doodle?

    abstract fun clone(parent: NbtDoodle?): Doodle
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

    override fun clone(parent: NbtDoodle?): NbtDoodle {
        return NbtDoodle(tag.clone(tag.name), depth, index, parent)
            .apply {
                expanded = this@NbtDoodle.expanded
                expandedItems.addAll(this@NbtDoodle.expandedItems.map { it.clone(this) })
                collapsedItems.addAll(this@NbtDoodle.collapsedItems.map { it.clone(this) })
            }
    }

    fun create(new: Doodle, useIndex: Boolean = true): Doodle {
        new.depth = depth + 1

        when (tag.type) {
            TagType.TAG_COMPOUND -> {
                new as? NbtDoodle
                    ?: throw Exception("invalid operation: internal error. expected: NbtDoodle, actual was: ${new.javaClass.name}")

                if (useIndex) tag.getAs<CompoundTag>().insert(new.index, new.tag)
                else tag.getAs<CompoundTag>().add(new.tag)

                if (!useIndex) new.index = tag.getAs<CompoundTag>().value.size - 1
            }
            TagType.TAG_LIST -> {
                new as? NbtDoodle
                    ?: throw Exception("invalid operation: internal error. expected: NbtDoodle, actual was: ${new.javaClass.name}")

                val list = tag.getAs<ListTag>()

                if (list.elementsType != new.tag.type)
                    throw Exception("invalid operation: tag type mismatch. expected: ${tag.type.name}, actual was: ${new.tag.type.name}")
                else {
                    if (useIndex) list.value.add(new.index, new.tag)
                    else list.value.add(new.tag)
                }

                if (!useIndex) new.index = list.value.size - 1
            }
            TagType.TAG_BYTE_ARRAY -> {
                new as? ValueDoodle
                    ?: throw Exception("invalid operation: internal error. expected: ValueDoodle, actual was: ${new.javaClass.name}")

                val value = new.value.toByteOrNull()
                    ?: throw Exception("invalid operation: value type mismatch. expected: Byte, actual was: ${new.value}")

                val array = tag.getAs<ByteArrayTag>()
                array.value = array.value.toMutableList().apply {
                    if (useIndex) add(new.index, value)
                    else add(value)
                }.toByteArray()

                if (!useIndex) new.index = array.value.size - 1
            }
            TagType.TAG_INT_ARRAY -> {
                new as? ValueDoodle
                    ?: throw Exception("invalid operation: internal error. expected: ValueDoodle, actual was: ${new.javaClass.name}")

                val value = new.value.toIntOrNull()
                    ?: throw Exception("invalid operation: value type mismatch. expected: Byte, actual was: ${new.value}")

                val array = tag.getAs<IntArrayTag>()
                array.value = array.value.toMutableList().apply {
                    if (useIndex) add(new.index, value)
                    else add(value)
                }.toIntArray()

                if (!useIndex) new.index = array.value.size - 1
            }
            TagType.TAG_LONG_ARRAY -> {
                new as? ValueDoodle
                    ?: throw Exception("invalid operation: internal error. expected: ValueDoodle, actual was: ${new.javaClass.name}")

                val value = new.value.toLongOrNull()
                    ?: throw Exception("invalid operation: value type mismatch. expected: Byte, actual was: ${new.value}")

                val array = tag.getAs<LongArrayTag>()
                array.value = array.value.toMutableList().apply {
                    if (useIndex) add(new.index, value)
                    else add(value)
                }.toLongArray()

                if (!useIndex) new.index = array.value.size - 1
            }
            else -> throw Exception("invalid operation: ${tag.javaClass.name} cannot own child tags.")
        }

        if (expanded) {
            if (new.index == -1 || !useIndex) expandedItems.add(new)
            else expandedItems.add(new.index, new)
        } else {
            if (new.index == -1 || !useIndex) collapsedItems.add(new)
            else collapsedItems.add(new.index, new)
        }

        return new
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
    parent: NbtDoodle?
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

    override fun clone(parent: NbtDoodle?): ValueDoodle {
        return ValueDoodle(value, depth, index, parent)
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

class PasteDoodleAction(
    val created: List<Doodle>
): DoodleAction()
