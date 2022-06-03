package doodler.application.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import doodler.application.structure.DoodlerWindow
import doodler.application.structure.IntroDoodlerWindow

@Stable
class DoodlerAppState {

    val windows = mutableStateListOf<DoodlerWindow>(
        IntroDoodlerWindow("doodler")
    )

    fun eraseAll() {
        windows.clear()
    }

    fun erase(that: DoodlerWindow) {
        windows.remove(that)
    }

    fun sketch(that: DoodlerWindow) {
        windows.add(that)
    }

}

@Composable
fun rememberDoodlerApplicationState() = remember { DoodlerAppState() }