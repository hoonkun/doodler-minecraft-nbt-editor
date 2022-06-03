package activator.doodler.application.states

import androidx.compose.runtime.mutableStateListOf
import activator.doodler.application.structures.DoodlerWindow

class DoodlerApplicationState {
    val windows = mutableStateListOf<DoodlerWindow>()

    init {
        windows += createNewState(DoodlerWindow.Type.MAIN, "doodler")
    }

    fun openNew(type: DoodlerWindow.Type, name: String, path: String) {
        windows += createNewState(type, "doodler - $name [${if (type == DoodlerWindow.Type.WORLD_EDITOR) "world" else "single"}]", path)
    }

    fun exit() {
        windows.clear()
    }

    private fun createNewState(
        type: DoodlerWindow.Type,
        title: String,
        path: String? = null
    ) = DoodlerWindow(
        type,
        title,
        windows::remove,
        path
    )
}