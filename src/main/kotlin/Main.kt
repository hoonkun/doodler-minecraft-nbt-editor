import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.WindowState
import composables.main.NoWorldsSelected
import composables.main.SingleEditor
import composables.main.WorldEditor

@Composable
@Preview
fun App(
    addWorld: (String, String) -> Unit,
    addSingle: (String, String) -> Unit
) {

    MaterialTheme {
        Column (modifier = Modifier.fillMaxSize()) {
            NoWorldsSelected(addWorld, addSingle)
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

val keys = mutableStateListOf<Key>()

@Composable
private fun DoodlerWindow(
    appState: DoodlerApplicationState,
    windowState: DoodlerWindowData
) = Window(
    onPreviewKeyEvent = {
        if (it.type == KeyEventType.KeyDown) keys.add(it.key)
        else if (it.type == KeyEventType.KeyUp) keys.remove(it.key)
        false
    },
    onCloseRequest = if (windowState.type == DoodlerWindowType.MAIN) appState::exit else windowState::close,
    state = WindowState(
        size = if (windowState.type == DoodlerWindowType.MAIN) DpSize(700.dp, 650.dp) else DpSize(1400.dp, 1100.dp)
    ),
    title = windowState.title
) {
    when (windowState.type) {
        DoodlerWindowType.MAIN -> App(
            addWorld = { displayName, path -> appState.openNew(DoodlerWindowType.WORLD_EDITOR, displayName, path) },
            addSingle = { displayName, path -> appState.openNew(DoodlerWindowType.SINGLE_EDITOR, displayName, path) }
        )
        DoodlerWindowType.WORLD_EDITOR -> WorldEditor("/home/hoonkun/minecraft/data-directory/saves/doodler_test_world")
        DoodlerWindowType.SINGLE_EDITOR -> SingleEditor(windowState.path ?: throw Exception("No path specified!"))
    }
}

private class DoodlerApplicationState {
    val windows = mutableStateListOf<DoodlerWindowData>()

    init {
        windows += createNewState(DoodlerWindowType.MAIN, "doodler")
    }

    fun openNew(type: DoodlerWindowType, name: String, path: String) {
        windows += createNewState(type, "doodler - $name(${if (type == DoodlerWindowType.WORLD_EDITOR) "world" else "single"})", path)
    }

    fun exit() {
        windows.clear()
    }

    private fun createNewState(
        type: DoodlerWindowType,
        title: String,
        path: String? = null
    ) = DoodlerWindowData(
        type,
        title,
        windows::remove,
        path
    )
}

private class DoodlerWindowData(
    val type: DoodlerWindowType,
    val title: String,
    private val close: (DoodlerWindowData) -> Unit,
    val path: String? = null
) {
    fun close() = close(this)
}

enum class DoodlerWindowType(val value: String) {
    MAIN("main"), WORLD_EDITOR("world_editor"), SINGLE_EDITOR("single_editor")
}