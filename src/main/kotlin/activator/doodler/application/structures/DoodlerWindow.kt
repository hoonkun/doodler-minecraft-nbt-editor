package activator.doodler.application.structures

class DoodlerWindow(
    val type: Type,
    val title: String,
    private val close: (DoodlerWindow) -> Unit,
    val path: String? = null
) {
    fun close() = close(this)

    enum class Type(val value: String) {
        MAIN("activator.main"),
        WORLD_EDITOR("world_editor"),
        SINGLE_EDITOR("single_editor"),
        WORLD_SELECTOR("world_selector"),
        FILE_SELECTOR("file_selector")
    }
}