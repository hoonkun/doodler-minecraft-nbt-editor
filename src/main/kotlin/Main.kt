import androidx.compose.runtime.key
import androidx.compose.ui.window.application
import composable.DoodlerWindow
import doodler.application.state.rememberDoodlerApplicationState

fun main() = application {
    val doodlerAppState = rememberDoodlerApplicationState()

    for (window in doodlerAppState.windows) {
        key(window) { DoodlerWindow(doodlerAppState, window) }
    }
}
