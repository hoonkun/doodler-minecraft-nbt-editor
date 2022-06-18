package composable

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Typography
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import composable.editor.standalone.StandaloneNbtEditor
import composable.editor.world.WorldEditor
import composable.intro.Intro
import composable.selector.Selector
import doodler.application.state.DoodlerAppState
import doodler.application.structure.*
import doodler.local.RecentOpen
import doodler.minecraft.DatWorker
import doodler.nbt.tag.CompoundTag
import doodler.nbt.tag.StringTag
import doodler.theme.DoodlerTheme
import doodler.unit.ddp
import doodler.unit.dsp
import java.io.File


private val keys = mutableListOf<Key>()

@Composable
fun rememberGlobalKeys() = remember { keys }

@Composable
fun DoodlerWindow(
    appState: DoodlerAppState,
    window: DoodlerWindow
) = Window(
    onCloseRequest = { if (window is IntroDoodlerWindow) appState.eraseAll() else appState.erase(window) },
    state = WindowState(size = window.initialSize),
    icon = painterResource("/icons/intro/doodler_icon_large.png"),
    resizable = false,
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
            h1 = TextStyle(fontSize = 25.dsp),
            h2 = TextStyle(fontSize = 21.dsp),
            h3 = TextStyle(fontSize = 18.dsp),
            h4 = TextStyle(fontSize = 15.dsp),
            h5 = TextStyle(fontSize = 12.dsp),
            h6 = TextStyle(fontSize = 10.dsp)
        )
    ) {
        CompositionLocalProvider(
            LocalRippleTheme provides DoodlerTheme.ClearRippleTheme
        ) {
            Box(
                modifier = Modifier
                    .requiredSizeIn(minWidth = window.initialSize.width, minHeight = window.initialSize.height - 30.ddp)
            ) {
                when (window) {
                    is IntroDoodlerWindow -> Intro(
                        localApplicationData = appState.data,
                        openRecent = openRecent@ { type, file ->
                            val item = appState.data.recent.find { it.type == type && it.path == file.absolutePath }
                                ?: return@openRecent

                            appState.data.recent.remove(item)
                            appState.data.recent.add(0, item)
                            appState.data.save()

                            appState.sketch(
                                EditorDoodlerWindow(
                                    title = "doodler - ${item.name}[${type.name}]",
                                    type = type,
                                    path = file.absolutePath
                                )
                            )
                        },
                        openSelector = {
                            appState.sketch(SelectorDoodlerWindow("doodler: open '${it.displayName}'", it))
                        }
                    )
                    is SelectorDoodlerWindow -> Selector(window.targetType) { file, type ->
                        val name =
                            if (type == DoodlerEditorType.World) {
                                DatWorker.read(File("${file.absolutePath}/level.dat").readBytes())["Data"]
                                    ?.getAs<CompoundTag>()?.get("LevelName")
                                    ?.getAs<StringTag>()?.value ?: return@Selector
                            } else {
                                file.name
                            }

                        appState.data.recent.removeIf { it.path == file.path }

                        appState.data.recent.add(0, RecentOpen(type = type, name = name, path = file.absolutePath))
                        appState.data.save()

                        appState.erase(window)
                        appState.sketch(
                            EditorDoodlerWindow(
                                title = "doodler - $name[${type.name}]",
                                type = type,
                                path = file.absolutePath
                            )
                        )
                    }
                    is EditorDoodlerWindow -> {
                        when (window.type) {
                            DoodlerEditorType.Standalone -> StandaloneNbtEditor(window.path)
                            DoodlerEditorType.World -> WorldEditor(window.path)
                        }
                    }
                }
            }
        }
    }
}