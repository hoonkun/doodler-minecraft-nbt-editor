package composables.states.editor.world

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import composables.stateful.editor.AnvilOpenRequest
import doodler.anvil.AnvilLocation
import doodler.anvil.ChunkLocation
import doodler.file.IOUtils
import doodler.file.WorldDimension
import java.io.File

class Editor {
    val items = mutableStateListOf<EditorItem>()
    var selected by mutableStateOf<EditorItem?>(null)

    fun hasItem(ident: String) = items.find { it.ident == ident } != null

    operator fun get(ident: String): EditorItem? = items.find { it.ident == ident }

    fun select(item: EditorItem) {
        if (!items.any { item.ident == it.ident }) return
        selected = item
    }

    fun open(item: EditorItem) {
        if (items.any { item.ident == it.ident }) return
        items.add(item)
        selected = item
    }

    fun close(item: EditorItem) {
        if (item == selected) selected = items.getOrNull(items.indexOf(item) - 1)
        items.remove(item)
    }
}

abstract class EditorItem {
    abstract val ident: String
    abstract val name: String
}

class SelectorItem(
    val state: SnapshotStateMap<AnvilLocation?, SnapshotStateMap<WorldDimension, SelectorState>> = mutableStateMapOf(),
    from: MutableState<AnvilOpenRequest?> = mutableStateOf(null)
): EditorItem() {
    override val ident: String get() = "ANVIL_SELECTOR"
    override val name: String get() = "MAP"

    var from by from
}

abstract class NbtItem(
    val state: NbtState
): EditorItem()

class StandaloneNbtItem(
    state: NbtState,
    val file: File
): NbtItem(state) {
    override val ident: String get() = file.absolutePath
    override val name: String get() = file.name

    companion object {
        fun fromFile(file: File): StandaloneNbtItem =
            StandaloneNbtItem(NbtState.new(IOUtils.readLevel(file.readBytes())), file)
    }
}

class AnvilNbtItem(
    state: NbtState,
    val anvil: File,
    val location: ChunkLocation
): NbtItem(state) {
    override val ident: String get() = "${anvil.absolutePath}/c.${location.x}.${location.z}"
    override val name: String get() = "[] c.${location.x}.${location.z}"
}
