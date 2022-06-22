package doodler.application.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import doodler.application.structure.*
import doodler.local.LocalDataState
import java.io.File

@Stable
class DoodlerAppState {

    val data = LocalDataState()

    val windows = mutableStateListOf<DoodlerWindow>(
        IntroDoodlerWindow("doodler")
    )

    fun eraseAll() {
        windows.clear()
    }

    fun erase(that: DoodlerWindow) {
        windows.remove(that)
    }

    fun sketch(that: DoodlerWindow): Boolean {
        if (windows.any { it is EditorDoodlerWindow && that is EditorDoodlerWindow && it.path == that.path })
            return false
        windows.add(that)
        return true
    }

    fun sketchEditor(title: String, type: DoodlerEditorType, file: File): Boolean {
        val that = when (type) {
            DoodlerEditorType.World -> WorldEditorDoodlerWindow(
                title = "doodler - ${title}[${type.name}]",
                path = file.absolutePath,
            )
            DoodlerEditorType.Standalone -> StandaloneEditorDoodlerWindow(
                title = "doodler - ${title}[${type.name}]",
                file = file
            )
        }
        if (windows.any { it is EditorDoodlerWindow && it.path == that.path })
            return false

        windows.add(that)
        return true
    }

}

@Composable
fun rememberDoodlerApplicationState() = remember { DoodlerAppState() }