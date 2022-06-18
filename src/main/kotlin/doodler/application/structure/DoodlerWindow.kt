package doodler.application.structure

import androidx.compose.ui.unit.DpSize
import doodler.unit.ddp

sealed class DoodlerWindow(
    val title: String
) {
    abstract val initialSize: DpSize
}

class IntroDoodlerWindow(
    title: String
): DoodlerWindow(title) {
    override val initialSize: DpSize = DpSize(425.ddp, 387.ddp)
}

class SelectorDoodlerWindow(
    title: String,
    val targetType: DoodlerEditorType
): DoodlerWindow(title) {
    override val initialSize: DpSize = DpSize(525.ddp, 300.ddp)
}

class EditorDoodlerWindow(
    title: String,
    val type: DoodlerEditorType,
    val path: String
): DoodlerWindow(title) {
    override val initialSize: DpSize
        get() {
            return when (type) {
                DoodlerEditorType.World -> DpSize(750.ddp, 625.ddp)
                DoodlerEditorType.Standalone -> DpSize(850.ddp, 775.ddp)
            }
        }
}

enum class DoodlerEditorType(val displayName: String) {
    World("world"), Standalone("nbt file")
}
