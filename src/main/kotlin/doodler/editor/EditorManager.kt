package doodler.editor

import androidx.compose.runtime.*
import doodler.minecraft.DatWorker
import doodler.minecraft.structures.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import doodler.doodle.structures.TagDoodle
import doodler.editor.states.McaEditorState
import doodler.editor.states.NbtEditorState
import doodler.file.toStateFile
import java.io.File

@Stable
class EditorManager {
    val editors = mutableStateListOf<Editor>()
    var selected by mutableStateOf<Editor?>(null)

    fun hasItem(ident: String) = editors.find { it.ident == ident } != null

    operator fun get(ident: String): Editor? = editors.find { it.ident == ident }

    fun select(ident: String) {
        select(this[ident])
    }

    fun select(item: Editor?) {
        if (item == null) return
        if (!editors.any { item.ident == it.ident }) return
        selected = item
    }

    fun open(item: Editor) {
        if (editors.any { item.ident == it.ident }) return
        editors.add(item)
        selected = item
    }

    fun close(item: Editor) {
        if (item == selected) {
            val index = editors.indexOf(item)
            selected =
                if (index != 0) editors.getOrNull(index - 1)
                else editors.getOrNull(1)
        }
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
            StandaloneNbtEditor(
                file,
                NbtEditorState(
                    TagDoodle(DatWorker.read(file.readBytes()), -1, null),
                    file.toStateFile(),
                    DatFileType
                )
            )
    }
}

class AnvilNbtEditor(
    private val anvil: File,
    private val location: ChunkLocation,
    state: NbtEditorState
): NbtEditor(state) {
    override val ident: String get() = ident(anvil, location)
    override val name: String get() = "c.${location.x}.${location.z}"

    val path: String get() = "${WorldDimension[anvil.parentFile.parentFile.name].displayName}/${anvil.parentFile.name}/"

    companion object {
        fun ident(anvil: File, location: ChunkLocation) = "${anvil.absolutePath}/c.${location.x}.${location.z}"
    }
}

sealed class McaEditor<K>(
    val states: SnapshotStateMap<K, McaEditorState>
): Editor()

class GlobalMcaEditor(
    states: SnapshotStateMap<WorldDimension, McaEditorState> = mutableStateMapOf()
): McaEditor<WorldDimension>(states) {
    var payload: GlobalUpdateRequest? by mutableStateOf(null)

    val state by derivedStateOf {
        val dimension = payload?.dimension ?: return@derivedStateOf null
        states[dimension]
    }

    override val ident: String get() = this.javaClass.name
    override val name: String get() = "WorldMap"

    fun state(defaultFactory: () -> McaEditorState): McaEditorState? {
        val localState = state
        if (localState != null) return localState

        val dimension = payload?.dimension ?: return null

        val newState = defaultFactory()
        states[dimension] = newState
        return newState
    }

    companion object {
        val Identifier: String = GlobalMcaEditor::class.java.name
    }

}

class SingleMcaEditor(
    _payload: SingleMcaRequest,
    states: SnapshotStateMap<SingleMcaRequest, McaEditorState> = mutableStateMapOf(),
): McaEditor<SingleMcaRequest>(states) {
    var payload by mutableStateOf(_payload)

    val state by derivedStateOf { states[payload] }

    override val ident: String get() = this.javaClass.name
    override val name: String by derivedStateOf { "${payload.location.x}.${payload.location.z}.mca" }

    fun state(defaultFactory: () -> McaEditorState): McaEditorState {
        val localState = state
        if (localState != null) return localState

        val newState = defaultFactory()
        states[payload] = newState
        return newState
    }

    companion object {
        val Identifier: String = SingleMcaEditor::class.java.name
    }
}

sealed class OpenRequest

class NbtOpenRequest(
    val file: File
): OpenRequest() {
    val ident: String get() = file.absolutePath
    val name: String get() = file.name
}

sealed class McaOpenRequest: OpenRequest()

sealed class GlobalMcaRequest: McaOpenRequest()

object GlobalOpenRequest: GlobalMcaRequest()

class GlobalUpdateRequest(
    val dimension: WorldDimension,
    val type: McaType,
    val location: AnvilLocation,
    val file: File
): GlobalMcaRequest()

class SingleMcaRequest(
    val dimension: WorldDimension,
    val type: McaType,
    val location: AnvilLocation,
    val file: File
): McaOpenRequest()
