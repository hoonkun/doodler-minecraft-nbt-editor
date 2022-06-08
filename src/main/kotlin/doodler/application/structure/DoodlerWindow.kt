package doodler.application.structure

sealed class DoodlerWindow(
    val title: String
)

class IntroDoodlerWindow(
    title: String
): DoodlerWindow(title)

class SelectorDoodlerWindow(
    title: String,
    val targetType: DoodlerEditorType
): DoodlerWindow(title)

class EditorDoodlerWindow(
    title: String,
    val type: DoodlerEditorType,
    val path: String
): DoodlerWindow(title)

enum class DoodlerEditorType(val displayName: String) {
    WORLD("world"), STANDALONE("nbt file")
}
