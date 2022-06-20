package doodler.editor

import androidx.compose.runtime.*
import doodler.minecraft.DatWorker
import doodler.minecraft.structures.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.graphics.ImageBitmap
import doodler.doodle.structures.CreatorDoodle
import doodler.doodle.structures.Doodle
import doodler.doodle.structures.EditorDoodle
import doodler.doodle.structures.TagDoodle
import doodler.editor.states.McaEditorState
import doodler.editor.states.NbtEditorState
import doodler.editor.structures.EditorLog
import doodler.file.toStateFile
import java.io.File

@Stable
class EditorManager {
    val editors = mutableStateListOf<Editor>()
    var selected by mutableStateOf<Editor?>(null)

    val globalLogs by derivedStateOf {
        mutableListOf<EditorLog>()
            .apply {
                editors.filterIsInstance<NbtEditor>()
                    .forEach { addAll(it.state.logs) }
            }
            .sortedBy { it.createdAt }
    }

    val cache = TerrainCache(mutableStateMapOf(), mutableMapOf())

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

    val breadcrumb get() = breadcrumbs()

    protected abstract fun breadcrumbs(): List<Breadcrumb>
}

sealed class NbtEditor(
    val state: NbtEditorState
): Editor() {

    protected fun parentBreadcrumbs(): List<Breadcrumb> {
        return mutableListOf<Breadcrumb>().apply {
            val doodle = state.selected.first()
            when (val action = state.action) {
                is CreatorDoodle -> add(NbtBreadcrumb(action))
                is EditorDoodle -> add(NbtBreadcrumb(action))
                else -> { /* no-op */ }
            }
            if (state.action !is EditorDoodle) add(NbtBreadcrumb(doodle))

            var current = doodle.parent
            while (current != null && current != state.root) {
                add(NbtBreadcrumb(current))
                current = current.parent
            }
        }.reversed()
    }

}

class StandaloneNbtEditor(
    private val file: File,
    state: NbtEditorState
): NbtEditor(state) {
    override val ident: String get() = file.absolutePath
    override val name: String get() = file.name

    override fun breadcrumbs(): List<Breadcrumb> {
        val root = OtherBreadcrumb(OtherBreadcrumb.Type.DatFile, file.name)
        val result = mutableListOf<Breadcrumb>(root)

        if (state.selected.size > 1) result.add(MultipleNbtBreadcrumb(state.selected.size))
        else if (state.selected.size == 1) result.addAll(parentBreadcrumbs())

        return result
    }

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

    override fun breadcrumbs(): List<Breadcrumb> {
        val dimension = DimensionBreadcrumb(WorldDimension[anvil.parentFile.parentFile.name])
        val type = McaTypeBreadcrumb(McaType[anvil.parentFile.name])
        val mca = OtherBreadcrumb(OtherBreadcrumb.Type.McaFile, anvil.name)
        val chunk = OtherBreadcrumb(OtherBreadcrumb.Type.Chunk, name)

        val result = mutableListOf(dimension, type, mca, chunk)

        if (state.selected.size > 1) result.add(MultipleNbtBreadcrumb(state.selected.size))
        else if (state.selected.size == 1) result.addAll(parentBreadcrumbs())

        return result
    }

    companion object {
        fun ident(anvil: File, location: ChunkLocation) = "${anvil.absolutePath}/c.${location.x}.${location.z}"
    }
}

sealed class McaEditor<K>(
    val states: SnapshotStateMap<K, McaEditorState>,
    initialPayload: McaPayload,
): Editor() {

    var payload: McaPayload by mutableStateOf(initialPayload)

    abstract val state: McaEditorState?

    abstract fun state(defaultFactory: (McaPayload) -> McaEditorState): McaEditorState

    override fun breadcrumbs(): List<Breadcrumb> {
        val dimension = DimensionBreadcrumb(payload.dimension)
        val type = McaTypeBreadcrumb(payload.type)
        val anvil = OtherBreadcrumb(OtherBreadcrumb.Type.McaFile, payload.file.name)
        val chunk = state?.selectedChunk?.let { OtherBreadcrumb(OtherBreadcrumb.Type.Chunk, "$it") }

        return mutableListOf(dimension, type, anvil).apply { if (chunk != null) add(chunk) }.toList()
    }

}

class GlobalMcaEditor(
    initialPayload: McaPayload,
    states: SnapshotStateMap<WorldDimension, McaEditorState> = mutableStateMapOf()
): McaEditor<WorldDimension>(states, initialPayload) {

    override val state by derivedStateOf { states[payload.dimension] }

    override val ident: String get() = this.javaClass.name
    override val name: String get() = "WorldMap"

    override fun state(defaultFactory: (McaPayload) -> McaEditorState): McaEditorState {
        val localState = state
        if (localState != null) return localState

        val newState = defaultFactory(payload)
        states[payload.dimension] = newState
        return newState
    }

    override fun breadcrumbs(): List<Breadcrumb> {
        return mutableListOf<Breadcrumb>(OtherBreadcrumb(OtherBreadcrumb.Type.WorldMap, "WorldMap"))
            .apply { addAll(super.breadcrumbs()) }
    }

    companion object {
        val Identifier: String = GlobalMcaEditor::class.java.name
    }

}

class SingleMcaEditor(
    states: SnapshotStateMap<McaPayload, McaEditorState> = mutableStateMapOf(),
    initialPayload: McaPayload,
): McaEditor<McaPayload>(states, initialPayload) {

    override val state by derivedStateOf { states[payload] }

    override val ident: String get() = this.javaClass.name
    override val name: String by derivedStateOf { "${payload.location.x}.${payload.location.z}.mca" }

    override fun state(defaultFactory: (McaPayload) -> McaEditorState): McaEditorState {
        val localState = state
        if (localState != null) return localState

        val newState = defaultFactory(payload)
        states[payload] = newState
        return newState
    }

    override fun breadcrumbs(): List<Breadcrumb> {
        return mutableListOf<Breadcrumb>(OtherBreadcrumb(OtherBreadcrumb.Type.McaMap, "McaMap"))
            .apply { addAll(super.breadcrumbs()) }
    }

    companion object {
        val Identifier: String = SingleMcaEditor::class.java.name
    }
}

sealed class Breadcrumb

class NbtBreadcrumb(
    val doodle: Doodle
): Breadcrumb()

class MultipleNbtBreadcrumb(
    val count: Int
): Breadcrumb()

class DimensionBreadcrumb(
    val dimension: WorldDimension
): Breadcrumb()

class McaTypeBreadcrumb(
    val type: McaType
): Breadcrumb()

class OtherBreadcrumb(
    val type: Type,
    val name: String
): Breadcrumb() {
    enum class Type {
        WorldMap,
        McaMap,
        McaFile,
        DatFile,
        Chunk
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

object GlobalOpenRequest: McaOpenRequest()
class SingleOpenRequest(val payload: McaPayload): McaOpenRequest()

data class McaPayload(
    val dimension: WorldDimension,
    val type: McaType,
    val location: AnvilLocation,
    val file: File
)

data class TerrainCache(
    val terrains: SnapshotStateMap<CachedTerrainInfo, ImageBitmap>,
    val yRanges: MutableMap<AnvilLocation, List<IntRange>>
)

data class CachedTerrainInfo(val yLimit: Int, val location: AnvilLocation)
