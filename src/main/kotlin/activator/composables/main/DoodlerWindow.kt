package activator.composables.main

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import activator.composables.editor.single.SingleEditor
import activator.composables.editor.world.WorldEditor
import activator.composables.selector.FileSelector
import activator.composables.selector.WorldSelector
import activator.doodler.application.states.DoodlerApplicationState
import activator.doodler.application.structures.DoodlerWindow
import activator.doodler.logger.DoodlerLogger
import activator.keys

@Composable
fun DoodlerWindow(
    appState: DoodlerApplicationState,
    windowState: DoodlerWindow
) = Window(
    onPreviewKeyEvent = onPreviewKeyEvent@ {
        if (windowState.type != DoodlerWindow.Type.WORLD_EDITOR && windowState.type != DoodlerWindow.Type.SINGLE_EDITOR)
            return@onPreviewKeyEvent false

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
                DoodlerWindow.Type.WORLD_SELECTOR -> DpSize(850.dp, 500.dp)
                DoodlerWindow.Type.FILE_SELECTOR -> DpSize(850.dp, 500.dp)
                else -> DpSize(1400.dp, 1150.dp)
            }
    ),
    title = windowState.title
) {
    DoodlerLogger.recomposition("DoodlerWindow")

    when (windowState.type) {
        DoodlerWindow.Type.MAIN -> Intro { type -> appState.openNew(type, "Open...", "") }
        DoodlerWindow.Type.WORLD_EDITOR -> WorldEditor(windowState.path ?: throw Exception("No path specified!"))
        DoodlerWindow.Type.SINGLE_EDITOR -> SingleEditor(windowState.path ?: throw Exception("No path specified!"))
        DoodlerWindow.Type.FILE_SELECTOR -> FileSelector(windowState) { displayName, path ->
            appState.openNew(DoodlerWindow.Type.SINGLE_EDITOR, displayName, path)
        }
        DoodlerWindow.Type.WORLD_SELECTOR -> WorldSelector(windowState) { displayName, path ->
            appState.openNew(DoodlerWindow.Type.WORLD_EDITOR, displayName, path)
        }
    }
}