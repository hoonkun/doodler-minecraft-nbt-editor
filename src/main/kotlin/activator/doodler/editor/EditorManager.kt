package activator.doodler.editor

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import activator.composables.editor.world.AnvilOpenRequest
import activator.doodler.editor.states.NbtState
import activator.doodler.editor.states.SelectorState
import doodler.minecraft.DatWorker
import doodler.minecraft.structures.*
import java.io.File

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

abstract class Editor {
    abstract val ident: String
    abstract val name: String
}

data class McaPayload(
    val request: AnvilOpenRequest,
    val dimension: WorldDimension,
    val type: McaType,
    val location: AnvilLocation,
    val file: File,
    val initial: BlockLocation? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as McaPayload

        if (dimension != other.dimension) return false
        if (type != other.type) return false
        if (location != other.location) return false
        if (file.absolutePath != other.file.absolutePath) return false
        if (initial != other.initial) return false

        return true
    }

    override fun hashCode(): Int {
        var result = dimension.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + location.hashCode()
        result = 31 * result + file.absolutePath.hashCode()
        result = 31 * result + initial.hashCode()
        return result
    }

    companion object {
        fun from(
            from: McaPayload,
            request: AnvilOpenRequest,
            dimension: WorldDimension? = null,
            type: McaType? = null,
            location: AnvilLocation? = null,
            file: File? = null,
            initial: BlockLocation? = null
        ) = McaPayload(
            request,
            dimension = dimension ?: from.dimension,
            type = type ?: from.type,
            location = location ?: from.location,
            file = file ?: from.file,
            initial = initial ?: from.initial
        )
    }
}

class McaEditor(
    val globalState: SnapshotStateMap<WorldDimension?, SelectorState> = mutableStateMapOf(),
    val mcaState: SnapshotStateMap<McaPayload?, SelectorState> = mutableStateMapOf(),
    from: MutableState<AnvilOpenRequest?> = mutableStateOf(null)
): Editor() {
    override val ident: String get() = "ANVIL_SELECTOR"
    override val name: String get() = "MAP"

    var from by from

    var globalMcaPayload: McaPayload? = null
}

abstract class NbtEditor(
    val state: NbtState
): Editor()

class StandaloneNbtEditor(
    state: NbtState,
    private val file: File
): NbtEditor(state) {
    override val ident: String get() = file.absolutePath
    override val name: String get() = file.name

    companion object {
        fun fromFile(file: File): StandaloneNbtEditor =
            StandaloneNbtEditor(NbtState.new(DatWorker.read(file.readBytes()), file, DatFileType), file)
    }
}

class AnvilNbtEditor(
    state: NbtState,
    private val anvil: File,
    private val location: ChunkLocation
): NbtEditor(state) {
    override val ident: String get() = "${anvil.absolutePath}/c.${location.x}.${location.z}"
    override val name: String get() = "c.${location.x}.${location.z}"
    val path: String get() = "${WorldDimension[anvil.parentFile.parentFile.name].displayName}/${anvil.parentFile.name}/"
}
