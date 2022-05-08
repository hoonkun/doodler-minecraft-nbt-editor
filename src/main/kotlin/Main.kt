import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.WindowState
import composables.main.NoWorldsSelected
import composables.main.WorldEditor

@Composable
@Preview
fun App(
    addWorld: (String) -> Unit
) {
    val onSelectWorld: (String) -> Unit = {
        addWorld(it)
    }

    MaterialTheme {
        Column (modifier = Modifier.fillMaxSize()) {
            NoWorldsSelected(onSelectWorld)
            BottomAppBar(
                elevation = 15.dp,
                backgroundColor = Color(60, 63, 65),
                contentPadding = PaddingValues(top = 0.dp, bottom = 0.dp, start = 25.dp, end = 25.dp),
                modifier = Modifier.height(40.dp)
            ) {
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    "by kiwicraft",
                    color = Color(255, 255, 255, 180),
                    fontSize = 14.sp
                )
            }
        }
    }
}

fun main() = application {
    val applicationState = remember { DoodlerApplicationState() }

    for (window in applicationState.windows) {
        key(window) {
            DoodlerWindow(applicationState, window)
        }
    }
}

@Composable
private fun DoodlerWindow(
    appState: DoodlerApplicationState,
    windowState: DoodlerWindowState
) = Window(
    onCloseRequest = windowState::close,
    state = WindowState(
        size = if (windowState.type == DoodlerWindowType.MAIN) DpSize(700.dp, 650.dp) else DpSize(1400.dp, 1100.dp)
    ),
    title = windowState.title
) {
    if (windowState.type == DoodlerWindowType.MAIN) App { appState.openNew(DoodlerWindowType.WORLD_EDITOR, it) }
    else if (windowState.type == DoodlerWindowType.WORLD_EDITOR) WorldEditor()
}

private class DoodlerApplicationState {
    val windows = mutableStateListOf<DoodlerWindowState>()

    init {
        windows += createNewState(DoodlerWindowType.MAIN, "doodler")
    }

    fun openNew(type: DoodlerWindowType, name: String) {
        windows += createNewState(type, "Editor $name")
    }

    fun exit() {
        windows.clear()
    }

    private fun createNewState(
        type: DoodlerWindowType,
        title: String
    ) = DoodlerWindowState(
        type,
        title,
        windows::remove
    )
}

private class DoodlerWindowState(
    val type: DoodlerWindowType,
    val title: String,
    private val close: (DoodlerWindowState) -> Unit
) {
    fun close() = close(this)
}

enum class DoodlerWindowType(val value: String) {
    MAIN("main"), WORLD_EDITOR("world_editor"), SINGLE_EDITOR("single_editor")
}