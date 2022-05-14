package composables.main

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.text.input.TextFieldValue
import doodler.anvil.ChunkLocation
import doodler.doodle.Doodle
import doodler.doodle.DoodleState
import doodler.doodle.rememberDoodleState
import doodler.file.WorldData
import nbt.tag.CompoundTag

var rootWorldData by mutableStateOf<WorldData?>(null)

fun display(dimension: String): String {
    return when (dimension) {
        "" -> "Overworld"
        "DIM-1" -> "Nether"
        "DIM1" -> "TheEnd"
        else -> "Unknown"
    }
}

class Editable(
    val ident: String,
    val root: CompoundTag? = null
) {
    private var _editorState: EditorState? = null
    val editorState get() = _editorState!!

    fun setEditorState(editorState: EditorState) {
        _editorState = editorState
    }

    fun editorStateOrNull(): EditorState? {
        return _editorState
    }

    val hasChanges = false

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is Editable) return false

        return other.ident == this.ident
    }

    override fun hashCode(): Int {
        return this.ident.hashCode()
    }

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

@Composable
fun rememberEditorState(
    doodles: SnapshotStateList<Doodle> = remember { mutableStateListOf() },
    doodleState: DoodleState = rememberDoodleState(),
    lazyState: LazyListState = rememberLazyListState()
) = remember(doodles, doodleState, lazyState) {
    EditorState(doodles, doodleState, lazyState)
}

class EditorState(
    val doodles: SnapshotStateList<Doodle>,
    val doodleState: DoodleState,
    val lazyState: LazyListState
)

@Composable
fun rememberSelectorState(
    selectedChunk: MutableState<ChunkLocation?> = remember { mutableStateOf(null) },
    chunkXValue: MutableState<TextFieldValue> = remember { mutableStateOf(TextFieldValue("")) },
    chunkZValue: MutableState<TextFieldValue> = remember { mutableStateOf(TextFieldValue("")) },
    blockXValue: MutableState<TextFieldValue> = remember { mutableStateOf(TextFieldValue("-")) },
    blockZValue: MutableState<TextFieldValue> = remember { mutableStateOf(TextFieldValue("-")) },
    isChunkXValid: MutableState<Boolean> = remember { mutableStateOf(true) },
    isChunkZValid: MutableState<Boolean> = remember { mutableStateOf(true) }
) = remember(selectedChunk, chunkXValue, chunkZValue, blockXValue, blockZValue, isChunkXValid, isChunkZValid) {
    SelectorState(selectedChunk, chunkXValue, chunkZValue, blockXValue, blockZValue, isChunkXValid, isChunkZValid)
}

class SelectorState(
    val selectedChunk: MutableState<ChunkLocation?>,
    val chunkXValue: MutableState<TextFieldValue>,
    val chunkZValue: MutableState<TextFieldValue>,
    val blockXValue: MutableState<TextFieldValue>,
    val blockZValue: MutableState<TextFieldValue>,
    val isChunkXValid: MutableState<Boolean>,
    val isChunkZValid: MutableState<Boolean>,
)

abstract class EditableHolder(
    val which: String,
    val format: Editable.Format,
    val contentType: Editable.ContentType,
) {
    enum class Type {
        Single, Multiple
    }
}

class SingleEditableHolder(
    which: String,
    format: Editable.Format,
    contentType: Editable.ContentType,
    editable: Editable
): EditableHolder(which, format, contentType) {
    val editable by mutableStateOf(editable)
}

class MultipleEditableHolder(
    which: String,
    format: Editable.Format,
    contentType: Editable.ContentType,
    val extra: Map<String, String> = mapOf()
): EditableHolder(which, format, contentType) {
    val editables = mutableStateListOf<Editable>()
    var selected by mutableStateOf("+")

    init {
        editables.add(Editable("+"))
    }

    private var _selectorState: SelectorState? = null
    val selectorState get() = _selectorState!!

    fun setSelectorState(selectorState: SelectorState) {
        _selectorState = selectorState
    }

    fun selectorStateOrNull(): SelectorState? {
        return _selectorState
    }

    fun hasEditable(ident: String) = editables.any { it.ident == ident }

    fun select(editable: Editable) {
        if (!editables.any { it.ident == editable.ident })
            return

        selected = editable.ident
    }

    fun add(editable: Editable) {
        if (!editables.any { it == editable || it.ident == editable.ident })
            editables.add(editable)
    }

    fun remove(editable: Editable) {
        if (editable.ident == selected)
            selected = editables[editables.indexOf(editable) - 1].ident

        editables.remove(editable)
    }
}
