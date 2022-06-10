package doodler.editor

import activator.doodler.editor.StandaloneNbtEditor
import activator.doodler.editor.states.NbtState
import androidx.compose.runtime.*
import doodler.minecraft.DatWorker
import doodler.minecraft.structures.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import doodler.editor.states.McaEditorState
import doodler.editor.states.NbtEditorState
import java.io.File

@Stable
class EditorManager {
    val editors = mutableStateListOf<Editor>()
    var selected by mutableStateOf<Editor?>(null)

    fun hasItem(ident: String) = editors.find { it.ident == ident } != null

    operator fun get(ident: String): Editor? = editors.find { it.ident == ident }

    fun select(item: Editor) {
        if (!editors.any { item.ident == it.ident }) return
        selected = item
    }

    fun open(item: Editor) {
        if (editors.any { item.ident == it.ident }) return
        editors.add(item)
        selected = item
    }

    fun close(item: Editor) {
        if (item == selected) selected = editors.getOrNull(editors.indexOf(item) - 1)
        editors.remove(item)
    }
}

sealed class Editor {
    abstract val ident: String
    abstract val name: String
}

sealed class NbtEditor(
    val state: NbtEditorState
): Editor()

class StandaloneNbtEditor(
    private val file: File,
    state: NbtEditorState
): NbtEditor(state) {
    override val ident: String get() = file.absolutePath
    override val name: String get() = file.name

    companion object {
        fun fromFile(file: File): StandaloneNbtEditor =
            StandaloneNbtEditor(NbtState.new(DatWorker.read(file.readBytes()), file, DatFileType), file)
    }
}

class AnvilNbtEditor(
    private val anvil: File,
    private val location: ChunkLocation,
    state: NbtEditorState
): NbtEditor(state) {
    override val ident: String get() = "${anvil.absolutePath}/c.${location.x}.${location.z}"
    override val name: String get() = "c.${location.x}.${location.z}"

    val path: String get() = "${WorldDimension[anvil.parentFile.parentFile.name].displayName}/${anvil.parentFile.name}/"
}

sealed class McaEditor<K>(
    val states: SnapshotStateMap<K, McaEditorState>
): Editor() {
    abstract val from: McaOpenRequest?
}

class GlobalMcaEditor(
    states: SnapshotStateMap<WorldDimension, McaEditorState>
): McaEditor<WorldDimension>(states) {
    override val from: GlobalMcaRequest? by mutableStateOf(null)

    override val ident: String get() = this.javaClass.name
    override val name: String get() = "WorldMap"
}

class SingleMcaEditor(
    states: SnapshotStateMap<SingleMcaStatePayload, McaEditorState>
): McaEditor<SingleMcaStatePayload>(states) {
    override val from: SingleMcaRequest? by mutableStateOf(null)

    override val ident: String get() = this.javaClass.name
    override val name: String by derivedStateOf { "${from?.location?.x}.${from?.location?.z}.mca" }
}

fun <T> T.alwaysEquals() = AlwaysEquals(this)

class AlwaysEquals<T>(args: T) {
    override fun equals(other: Any?): Boolean = true
    override fun hashCode(): Int = 31
}

data class SingleMcaStatePayload(
    val request: AlwaysEquals<McaOpenRequest>,
    val dimension: WorldDimension,
    val type: McaType,
    val location: AnvilLocation,
    val file: File,
    val initial: BlockLocation? = null
)

sealed class OpenRequest

class NbtOpenRequest(
    val file: File
): OpenRequest() {
    val ident: String get() = file.absolutePath
    val name: String get() = file.name
}

sealed class McaOpenRequest: OpenRequest()

sealed class GlobalMcaRequest: McaOpenRequest()

object GlobalInitRequest: GlobalMcaRequest()

class GlobalUpdateRequest(
    val dimension: WorldDimension? = null,
    val type: McaType? = null,
    val region: AnvilLocation? = null
): GlobalMcaRequest()

class SingleMcaRequest(
    val location: AnvilLocation,
    val file: File
): McaOpenRequest()

