package composables.main

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import composables.editor.single.SingleEditor
import composables.editor.world.WorldEditor
import composables.selector.FileSelector
import composables.selector.WorldSelector
import doodler.application.states.DoodlerApplicationState
import doodler.application.structures.DoodlerWindow
import keys

@Composable
fun DoodlerWindow(
    appState: DoodlerApplicationState,
    windowState: DoodlerWindow
) = Window(
    onPreviewKeyEvent = {
        if (it.type == KeyEventType.KeyDown) keys.add(it.key)
        else if (it.type == KeyEventType.KeyUp) keys.remove(it.key)
        false
    },
    onCloseRequest = if (windowState.type == DoodlerWindow.Type.MAIN) appState::exit else windowState::close,
    state = WindowState(
        size =
            when (windowState.type) {
                DoodlerWindow.Type.MAIN -> DpSize(700.dp, 650.dp)
                DoodlerWindow.Type.SINGLE_EDITOR -> DpSize(1000.dp, 1050.dp)
                DoodlerWindow.Type.WORLD_SELECTOR -> DpSize(800.dp, 900.dp)
                else -> DpSize(1400.dp, 1150.dp)
            }
    ),
    title = windowState.title
) {
    when (windowState.type) {
        DoodlerWindow.Type.MAIN -> Intro { type -> appState.openNew(type, "Open...", "") }
        DoodlerWindow.Type.WORLD_EDITOR -> WorldEditor(windowState.path ?: throw Exception("No path specified!"))
        DoodlerWindow.Type.SINGLE_EDITOR -> SingleEditor(windowState.path ?: throw Exception("No path specified!"))
        DoodlerWindow.Type.FILE_SELECTOR -> FileSelector { displayName, path ->
            appState.openNew(DoodlerWindow.Type.SINGLE_EDITOR, displayName, path)
        }
        DoodlerWindow.Type.WORLD_SELECTOR -> WorldSelector { displayName, path ->
            appState.openNew(DoodlerWindow.Type.WORLD_EDITOR, displayName, path)
        }
    }
}