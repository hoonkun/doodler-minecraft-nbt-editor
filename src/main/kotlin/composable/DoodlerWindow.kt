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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.window.*
import composable.editor.standalone.StandaloneNbtEditor
import composable.editor.world.WorldEditor
import composable.intro.Intro
import composable.selector.Selector
import doodler.application.state.DoodlerAppState
import doodler.application.structure.*
import doodler.editor.NbtEditor
import doodler.local.*
import doodler.minecraft.structures.WorldSpecification
import doodler.theme.DoodlerTheme
import doodler.unit.adp
import doodler.unit.ddp
import doodler.unit.dsp
import java.awt.Dimension


private val keys = mutableListOf<Key>()

@Composable
fun rememberGlobalKeys() = remember { keys }

@Composable
fun DoodlerWindow(
    appState: DoodlerAppState,
    window: DoodlerWindow,
) {

    var unsavedChanges by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }

    val requestCloseAll = {
        val worldEditorStates = appState.windows.filterIsInstance<WorldEditorDoodlerWindow>()
        val standaloneEditorStates = appState.windows.filterIsInstance<StandaloneEditorDoodlerWindow>()

        val unsavedWorlds = worldEditorStates
            .filter { window ->
                window.editorState.manager.editors.any { it is NbtEditor && it.state.actionFlags.canBeSaved }
            }
            .map { it.editorState.worldSpec.name }

        val unsavedStandalone = standaloneEditorStates
            .filter { window -> window.editorState.actionFlags.canBeSaved }
            .map { it.editor.name }

        unsavedWorlds to unsavedStandalone
    }

    val requestCloseEditor: (EditorDoodlerWindow) -> List<String> = { editorWindow ->
        when (editorWindow) {
            is WorldEditorDoodlerWindow -> {
                editorWindow.editorState.manager.editors
                    .filter { it is NbtEditor && it.state.actionFlags.canBeSaved }
                    .map { it.name }
            }
            is StandaloneEditorDoodlerWindow -> {
                if (editorWindow.editorState.actionFlags.canBeSaved) listOf(editorWindow.editor.name)
                else emptyList()
            }
        }
    }

    val onCloseRequest = {
        when (window) {
            is IntroDoodlerWindow -> {
                val (world, standalone) = requestCloseAll()
                if (world.isNotEmpty() || standalone.isNotEmpty()) {
                    val worldNames =
                        if (world.isEmpty()) ""
                        else "\n\nworld:\n${world.joinToString("\n") { "- $it" }}"
                    val standaloneNames =
                        if (standalone.isEmpty()) ""
                        else "\n\nstandalone:\n${standalone.joinToString("\n") { "- $it" }}"
                    val message = "There is unsaved editors!$worldNames$standaloneNames"
                    unsavedChanges = message to { appState.eraseAll() }
                } else {
                    appState.eraseAll()
                }
            }
            is WorldEditorDoodlerWindow -> {
                val world = requestCloseEditor(window)
                if (world.isNotEmpty()) {
                    unsavedChanges =
                        "There is unsaved tabs!\n${world.joinToString(", ")}" to { appState.erase(window) }
                } else {
                    appState.erase(window)
                }
            }
            is StandaloneEditorDoodlerWindow -> {
                val standalone = requestCloseEditor(window)
                if (standalone.isNotEmpty()) {
                    unsavedChanges =
                        "There is unsaved changes!" to { appState.erase(window) }
                } else {
                    appState.erase(window)
                }
            }
            else -> {
                appState.erase(window)
            }
        }
    }

    Window(
        onCloseRequest = onCloseRequest,
        state = window.state,
        icon = painterResource("/icons/intro/doodler_icon_large.png"),
        resizable = false,
        onPreviewKeyEvent = {
            if (it.type == KeyEventType.KeyDown) keys.add(it.key)
            else keys.remove(it.key)

            false
        },
        title = window.title
    ) {

        var editorAlreadyExists by mutableStateOf(false)

        MaterialTheme(typography = Typography(defaultFontFamily = DoodlerTheme.Fonts.JetbrainsMono)) {
            CompositionLocalProvider(
                LocalRippleTheme provides DoodlerTheme.ClearRippleTheme
            ) {
                Box(
                    modifier = Modifier
                        .requiredSizeIn(
                            minWidth = this.window.let { it.width - (it.insets.left + it.insets.right) }.adp,
                            minHeight = this.window.let { it.height - (it.insets.top + it.insets.bottom) }.adp
                        )
                ) {
                    when (window) {
                        is IntroDoodlerWindow -> Intro(
                            window = window,
                            openRecent = openRecent@ { type, file ->
                                val item = UserSavedLocalState.recent.find {
                                    it.type == type && it.path == file.absolutePath
                                } ?: return@openRecent

                                UserSavedLocalState.recent.remove(item)
                                UserSavedLocalState.recent.add(0, item)
                                UserSavedLocalState.save()

                                editorAlreadyExists =
                                    when (item.type) {
                                        DoodlerEditorType.World -> !appState.sketchWorldEditor(
                                            title = item.name,
                                            path = file.absolutePath,
                                            worldSpec = WorldSpecification(file.absolutePath)
                                        )
                                        DoodlerEditorType.Standalone -> !appState.sketchStandaloneEditor(
                                            title = item.name,
                                            file = file
                                        )
                                    }
                            },
                            openSelector = {
                                appState.sketch(
                                    SelectorDoodlerWindow("doodler: open '${it.displayName}'", targetType = it)
                                )
                            },
                            changeGlobalScale = {
                                appState.restart {
                                    editSavedLocal(globalScale = GlobalScale + it)
                                    GlobalScale += it
                                }
                            }
                        )
                        is SelectorDoodlerWindow -> Selector(window.targetType) { file, type ->
                            val (name, alreadyExists) =
                                when (type) {
                                    DoodlerEditorType.World -> {
                                        val worldSpec = WorldSpecification(file.absolutePath)
                                        val name = worldSpec.name

                                        appState.erase(window)
                                        name to !appState.sketchWorldEditor(
                                            title = name,
                                            path = file.absolutePath,
                                            worldSpec = WorldSpecification(file.absolutePath)
                                        )
                                    }
                                    DoodlerEditorType.Standalone -> {
                                        file.name to !appState.sketchStandaloneEditor(
                                            title = file.name,
                                            file = file
                                        )
                                    }
                                }

                            editorAlreadyExists = alreadyExists

                            UserSavedLocalState.recent.removeIf { it.path == file.path }

                            UserSavedLocalState.recent.add(
                                index = 0,
                                element = Recent(type = type, name = name, path = file.absolutePath)
                            )
                            UserSavedLocalState.save()
                        }
                        is StandaloneEditorDoodlerWindow -> StandaloneNbtEditor(window)
                        is WorldEditorDoodlerWindow -> WorldEditor(window.editorState)
                    }
                }
            }

            if (editorAlreadyExists) {
                EditorAlreadyExistsWarning { editorAlreadyExists = false }
            }

            if (unsavedChanges != null) {
                UnsavedChangesWarning(
                    message = unsavedChanges?.first ?: "Uh-oh... A problem occurred while closing window...",
                    ok = {
                        unsavedChanges?.second?.invoke()
                        unsavedChanges = null
                    },
                    cancel = { unsavedChanges = null }
                )
            }

        }

    }
}

@Composable
fun UnsavedChangesWarning(message: String, ok: () -> Unit, cancel: () -> Unit) {

    val messageState by remember { mutableStateOf(message) }

    val lines by derivedStateOf { messageState.split("\n") }
    val firstLine by derivedStateOf { lines[0] }
    val restLines by derivedStateOf {
        "\n".plus(lines.slice(1 until lines.size).joinToString("\n"))
    }

    Dialog(
        onCloseRequest = cancel,
        title = "",
        resizable = false,
        state = rememberDialogState(size = DpSize.Unspecified)
    ) {
        Row(
            modifier = Modifier
                .background(DoodlerTheme.Colors.Background)
                .onGloballyPositioned { coordinates ->
                    this.window.size = Dimension(
                        coordinates.size.width + this.window.insets.let { it.left + it.right },
                        coordinates.size.height + this.window.insets.let { it.top + it.bottom }
                    )
                }
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 20.ddp, vertical = 15.ddp)
            ) {
                Text(
                    text = ":O",
                    color = DoodlerTheme.Colors.Text.IdeFunctionName,
                    fontSize = 15.dsp,
                    modifier = Modifier.padding(bottom = 3.ddp)
                )
                Spacer(modifier = Modifier.height(10.ddp))
                Text(
                    text = firstLine,
                    color = DoodlerTheme.Colors.Text.IdeGeneral,
                    fontSize = 10.8.dsp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 17.dsp
                )
                Spacer(modifier = Modifier.height(6.ddp))
                if (restLines.isNotEmpty()) {
                    Column {
                        for (restSection in restLines.split("\n\n")) {
                            val text = restSection.trim()
                            if (text.isEmpty()) continue
                            Text(
                                text = text,
                                color = DoodlerTheme.Colors.Text.IdeGeneral,
                                fontSize = 9.dsp,
                                lineHeight = 17.dsp,
                                modifier = Modifier.requiredWidthIn(max = 275.ddp)
                            )
                            Spacer(modifier = Modifier.height(6.ddp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(15.ddp))
                Row(horizontalArrangement = Arrangement.Center) {
                    DialogButton("close anyway", ok)
                    Spacer(modifier = Modifier.width(20.ddp))
                    DialogButton("cancel", cancel)
                }
            }
        }
    }
}

@Composable
private fun EditorAlreadyExistsWarning(onCloseRequest: () -> Unit) =
    Dialog(
        onCloseRequest = onCloseRequest,
        title = "",
        resizable = false,
        state = DialogState(width = 275.ddp, height = 125.ddp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.background(DoodlerTheme.Colors.Background).fillMaxSize()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = ":(",
                    color = DoodlerTheme.Colors.Text.IdeFunctionName,
                    fontSize = 15.dsp,
                    modifier = Modifier.padding(bottom = 3.ddp)
                )
                Text(
                    text = "Cannot re-open already existing editor!!",
                    color = DoodlerTheme.Colors.Text.IdeGeneral,
                    fontSize = 9.dsp,
                    modifier = Modifier.padding(bottom = 15.ddp)
                )
                DialogButton("back", onCloseRequest)
            }
        }
    }

@Composable
private fun DialogButton(
    text: String,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    Box(
        modifier = Modifier.hoverable(interaction).clickable(onClick = onClick)
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
            text = text,
            color = DoodlerTheme.Colors.Text.IdeGeneral,
            fontSize = 10.dsp,
            modifier = Modifier.padding(horizontal = 7.ddp, vertical = 3.ddp)
        )
    }
}
