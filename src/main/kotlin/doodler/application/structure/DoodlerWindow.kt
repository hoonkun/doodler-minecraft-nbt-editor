package doodler.application.structure

import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
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
    abstract val state: WindowState

    abstract fun copy(): DoodlerWindow
}

class IntroDoodlerWindow(
    title: String,
    position: WindowPosition = WindowPosition(Alignment.Center)
): DoodlerWindow(title) {
    override val state: WindowState = WindowState(size = DpSize(425.ddp, 375.ddp), position = position)

    override fun copy() = IntroDoodlerWindow(title, state.position)
}

class SelectorDoodlerWindow(
    title: String,
    position: WindowPosition = WindowPosition(Alignment.Center),
    val targetType: DoodlerEditorType
): DoodlerWindow(title) {
    override val state: WindowState = WindowState(size = DpSize(525.ddp, 300.ddp), position = position)

    override fun copy() = SelectorDoodlerWindow(title, state.position, targetType)
}

sealed class EditorDoodlerWindow(
    title: String,
    position: WindowPosition = WindowPosition(Alignment.Center),
    val type: DoodlerEditorType,
    val path: String
): DoodlerWindow(title) {

    abstract val editorState: EditorState

    override val state: WindowState =
        WindowState(
            size = when (type) {
                DoodlerEditorType.World -> DpSize(750.ddp, 625.ddp)
                DoodlerEditorType.Standalone -> DpSize(575.ddp, 575.ddp)
            },
            position = position
        )
}

class WorldEditorDoodlerWindow(
    title: String,
    path: String,
    position: WindowPosition = WindowPosition(Alignment.Center),
    override val editorState: WorldEditorState = WorldEditorState(manager = EditorManager(), worldSpec = WorldSpecification(path))
): EditorDoodlerWindow(title, position, DoodlerEditorType.World, path) {

    override fun copy() = WorldEditorDoodlerWindow(title, path, state.position, editorState)

}

class StandaloneEditorDoodlerWindow(
    title: String,
    position: WindowPosition = WindowPosition(Alignment.Center),
    val file: File,
    override val editorState: NbtEditorState = NbtEditorState(
        root = TagDoodle(DatWorker.read(file.readBytes()), -1, null),
        file = file.toStateFile(),
        type = DatFileType
    )
): EditorDoodlerWindow(title, position, DoodlerEditorType.Standalone, file.absolutePath) {

    val editor = StandaloneNbtEditor(file = file, state = editorState)

    override fun copy() = StandaloneEditorDoodlerWindow(title, state.position, file, editorState)

}

enum class DoodlerEditorType(val displayName: String) {
    World("world"), Standalone("nbt file")
}
