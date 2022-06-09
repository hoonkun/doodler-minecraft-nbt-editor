package composable

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Typography
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.DpSize
import doodler.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import composable.editor.standalone.StandaloneNbtEditor
import composable.intro.Intro
import composable.selector.Selector
import doodler.application.state.DoodlerAppState
import doodler.application.structure.*
import doodler.theme.DoodlerTheme
import doodler.unit.sp


private val keys = mutableListOf<Key>()

@Composable
fun rememberGlobalKeys() = remember { keys }

@Composable
fun DoodlerWindow(
    appState: DoodlerAppState,
    window: DoodlerWindow
) = Window(
    onCloseRequest = { if (window is IntroDoodlerWindow) appState.eraseAll() else appState.erase(window) },
    state = WindowState(
        size =
            when (window) {
                is IntroDoodlerWindow -> DpSize(350.dp, 325.dp)
                is SelectorDoodlerWindow -> DpSize(525.dp, 300.dp)
                is EditorDoodlerWindow ->
                    when (window.type) {
                        DoodlerEditorType.WORLD -> DpSize(700.dp, 575.dp)
                        DoodlerEditorType.STANDALONE -> DpSize(850.dp, 775.dp)
                    }
            }
    ),
    onPreviewKeyEvent = {
        if (it.type == KeyEventType.KeyDown) keys.add(it.key)
        else keys.remove(it.key)

        false
    },
    title = window.title
) {
    MaterialTheme(
        typography = Typography(
            defaultFontFamily = DoodlerTheme.Fonts.JetbrainsMono,
            h1 = TextStyle(fontSize = 25.sp),
            h2 = TextStyle(fontSize = 21.sp),
            h3 = TextStyle(fontSize = 18.sp),
            h4 = TextStyle(fontSize = 15.sp),
            h5 = TextStyle(fontSize = 12.sp),
            h6 = TextStyle(fontSize = 10.sp)
        )
    ) {
        CompositionLocalProvider(
            LocalRippleTheme provides DoodlerTheme.ClearRippleTheme
        ) {
            when (window) {
                is IntroDoodlerWindow -> Intro { appState.sketch(SelectorDoodlerWindow("doodler: open '${it.displayName}'", it)) }
                is SelectorDoodlerWindow -> Selector(window.targetType) { file, type ->
                    appState.erase(window)
                    appState.sketch(EditorDoodlerWindow("", type, file.absolutePath))
                }
                is EditorDoodlerWindow -> {
                    when (window.type) {
                        DoodlerEditorType.STANDALONE -> StandaloneNbtEditor(window.path)
                        DoodlerEditorType.WORLD -> {  }
                    }
                }
            }
        }
    }
}