package doodler.application.structure

import androidx.compose.ui.unit.DpSize
import doodler.doodle.structures.TagDoodle
import doodler.editor.EditorManager
import doodler.editor.StandaloneNbtEditor
import doodler.editor.states.EditorState
import doodler.editor.states.NbtEditorState
import doodler.editor.states.WorldEditorState
import doodler.file.toStateFile
import doodler.minecraft.DatWorker
import doodler.minecraft.structures.DatFileType
import doodler.minecraft.structures.WorldSpecification
import doodler.unit.ddp
import java.io.File

sealed class DoodlerWindow(
    val title: String
) {
    abstract val initialSize: DpSize
}

class IntroDoodlerWindow(
    title: String
): DoodlerWindow(title) {
    override val initialSize: DpSize = DpSize(425.ddp, 375.ddp)
}

class SelectorDoodlerWindow(
    title: String,
    val targetType: DoodlerEditorType
): DoodlerWindow(title) {
    override val initialSize: DpSize = DpSize(525.ddp, 300.ddp)
}

sealed class EditorDoodlerWindow(
    title: String,
    val type: DoodlerEditorType,
    val path: String
): DoodlerWindow(title) {

    abstract val state: EditorState

    override val initialSize: DpSize
        get() {
            return when (type) {
                DoodlerEditorType.World -> DpSize(750.ddp, 625.ddp)
                DoodlerEditorType.Standalone -> DpSize(575.ddp, 575.ddp)
            }
        }
}

class WorldEditorDoodlerWindow(
    title: String,
    path: String,
    override val state: WorldEditorState = WorldEditorState(manager = EditorManager(), worldSpec = WorldSpecification(path))
): EditorDoodlerWindow(title, DoodlerEditorType.World, path)

class StandaloneEditorDoodlerWindow(
    title: String,
    file: File,
    override val state: NbtEditorState = NbtEditorState(
        root = TagDoodle(DatWorker.read(file.readBytes()), -1, null),
        file = file.toStateFile(),
        type = DatFileType
    )
): EditorDoodlerWindow(title, DoodlerEditorType.Standalone, file.absolutePath) {
    val editor = StandaloneNbtEditor(file = file, state = state)
}

enum class DoodlerEditorType(val displayName: String) {
    World("world"), Standalone("nbt file")
}
