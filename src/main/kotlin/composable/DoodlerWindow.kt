package composable

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.Typography
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.window.*
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
    state = WindowState(size = window.initialSize, position = WindowPosition(Alignment.Center)),
    icon = painterResource("/icons/intro/doodler_icon_large.png"),
    resizable = false,
    onPreviewKeyEvent = {
        if (it.type == KeyEventType.KeyDown) keys.add(it.key)
        else keys.remove(it.key)

        false
    },
    title = window.title
) {

    var existsWarning by mutableStateOf(false)

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

                            existsWarning = !appState.sketch(
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
                        existsWarning = !appState.sketch(
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

        if (existsWarning) {
            Dialog(
                onCloseRequest = { existsWarning = false },
                title = "",
                state = DialogState(width = 275.ddp, height = 125.ddp),
            ) {
                EditorAlreadyExistsWarning { existsWarning = false }
            }
        }

    }

}

@Composable
private fun EditorAlreadyExistsWarning(onCloseRequest: () -> Unit) {

    val hoverInteractionSource = remember { MutableInteractionSource() }
    val hovered by hoverInteractionSource.collectIsHoveredAsState()

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.background(DoodlerTheme.Colors.Background).fillMaxSize()
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = ":(",
                color = DoodlerTheme.Colors.Text.IdeFunctionName,
                fontSize = MaterialTheme.typography.h4.fontSize,
                modifier = Modifier.padding(bottom = 3.ddp)
            )
            Text(
                text = "Cannot re-open already existing editor!!",
                color = DoodlerTheme.Colors.Text.IdeGeneral,
                fontSize = MaterialTheme.typography.h6.fontSize * 0.9f,
                modifier = Modifier.padding(bottom = 15.ddp)
            )
            Box(
                modifier = Modifier.hoverable(hoverInteractionSource).clickable { onCloseRequest() }
                    .drawBehind {
                        if (hovered) drawRoundRect(
                            color = Color.White.copy(alpha = 0.1f),
                            cornerRadius = CornerRadius(3.ddp.value, 3.ddp.value)
                        )
                        else drawRoundRect(
                            color = Color.White.copy(alpha = 0.17f),
                            cornerRadius = CornerRadius(3.ddp.value, 3.ddp.value)
                        )
                    }
            ) {
                Text(
                    text = "back",
                    color = DoodlerTheme.Colors.Text.IdeGeneral,
                    fontSize = MaterialTheme.typography.h6.fontSize,
                    modifier = Modifier.padding(horizontal = 7.ddp, vertical = 3.ddp)
                )
            }
        }
    }
}
