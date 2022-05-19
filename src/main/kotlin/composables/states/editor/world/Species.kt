package composables.states.editor.world

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import doodler.anvil.ChunkLocation
import doodler.nbt.TagType
import doodler.nbt.tag.CompoundTag
import doodler.nbt.tag.ListTag

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

    // TODO:
    //  Compound 에서는 큰 의미가 없으나 List 에 붙혀넣었을 때는 순서를 변경할 수 있어야할 듯 함.
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

            selected.update(NbtDoodle.UpdateTarget.VALUE)
            selected.expand()

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
                if (target.elementsType != listTag.elementsType && listTag.elementsType != TagType.TAG_END)
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
                selected.tag.type == TagType.TAG_LIST && selected.tag.getAs<ListTag>().let { it.elementsType == target.elementsType || it.elementsType == TagType.TAG_END }
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