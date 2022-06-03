package composable

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Typography
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.DpSize
import doodler.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import composable.intro.Intro
import doodler.application.state.DoodlerAppState
import doodler.application.structure.*
import doodler.theme.DoodlerTheme

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
                is SelectorDoodlerWindow -> DpSize(425.dp, 250.dp)
                is EditorDoodlerWindow ->
                    when (window.type) {
                        DoodlerEditorType.WORLD -> DpSize(700.dp, 575.dp)
                        DoodlerEditorType.STANDALONE -> DpSize(500.dp, 525.dp)
                    }
            }
    ),
    title = window.title
) {
    MaterialTheme(
        typography = Typography(defaultFontFamily = DoodlerTheme.Fonts.JetbrainsMono)
    ) {
        CompositionLocalProvider(
            LocalRippleTheme provides DoodlerTheme.ClearRippleTheme
        ) {
            when (window) {
                is IntroDoodlerWindow -> Intro { }
                is SelectorDoodlerWindow -> {}
                is EditorDoodlerWindow -> {}
            }
        }
    }
}