package doodler.application.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import doodler.application.structure.DoodlerWindow
import doodler.application.structure.EditorDoodlerWindow
import doodler.application.structure.IntroDoodlerWindow
import doodler.local.LocalDataState

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

}

@Composable
fun rememberDoodlerApplicationState() = remember { DoodlerAppState() }