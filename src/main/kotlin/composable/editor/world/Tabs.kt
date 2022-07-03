package composable.editor.world

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import doodler.editor.AnvilNbtEditor
import doodler.editor.Editor
import doodler.editor.NbtEditor
import doodler.editor.states.WorldEditorState
import doodler.extension.toUUID
import doodler.minecraft.structures.WorldSpecification
import doodler.theme.DoodlerTheme
import doodler.types.BooleanProvider
import doodler.unit.ddp
import doodler.unit.dsp
import kotlinx.coroutines.CoroutineScope


@Composable
fun EditorTabGroup(
    state: WorldEditorState,
    onSelectTab: (Editor) -> Unit,
    onCloseTab: (Editor) -> Unit
) {

    val manager = state.manager
    val worldSpec = state.worldSpec

    val scrollState = rememberScrollState()

    val tabWidths = remember { mutableStateListOf<Int>() }

    val onCalculateTabWidth: (Editor, Int) -> Unit = { editor, width ->
        while (tabWidths.size < manager.editors.size) { tabWidths.add(0) }
        tabWidths[manager.editors.indexOf(editor)] = width
    }

    val onTabDrag: (Editor, Float) -> Float = lambda@ { editor, offset ->
        val index = manager.editors.indexOf(editor)
        if (index > 0 && tabWidths[index - 1] / 2f < -offset) {
            val offsetDiff = tabWidths[index - 1] / 2f
            manager.editors.add(index - 1, manager.editors.removeAt(index))
            tabWidths.add(index - 1, tabWidths.removeAt(index))
            return@lambda offsetDiff
        }
        if (index < manager.editors.lastIndex && tabWidths[index + 1] / 2f < offset) {
            val offsetDiff = tabWidths[index + 1] / 2f
            manager.editors.add(index + 1, manager.editors.removeAt(index))
            tabWidths.add(index + 1, tabWidths.removeAt(index))
            return@lambda offsetDiff - tabWidths[index]
        }
        offset
    }

    Box (
        modifier = Modifier
            .background(DoodlerTheme.Colors.SecondaryBackground)
            .fillMaxWidth().wrapContentHeight()
            .zIndex(10f)
    ) {
        Row(
            modifier = Modifier
                .wrapContentWidth()
                .horizontalScroll(scrollState)
        ) {
            for (editor in manager.editors) {
                key(editor.ident) {
                    EditorTab(
                        editor = editor,
                        worldSpec = worldSpec,
                        selectedProvider = { manager.selected == editor },
                        onSelectTab = onSelectTab,
                        onCloseTab = onCloseTab,
                        onTabDrag = onTabDrag,
                        onCalculateTabWidth = onCalculateTabWidth
                    )
                }
            }
        }
    }
}

@Composable
fun EditorTab(
    editor: Editor,
    worldSpec: WorldSpecification,
    selectedProvider: BooleanProvider,
    onSelectTab: (Editor) -> Unit,
    onCloseTab: (Editor) -> Unit,
    onTabDrag: (Editor, Float) -> Float,
    onCalculateTabWidth: (Editor, Int) -> Unit
) {
    val interaction = remember { MutableInteractionSource() }

    val hovered by interaction.collectIsHoveredAsState()
    val pressed by interaction.collectIsPressedAsState()

    val offset = remember { mutableStateOf(0f) }
    val draggableState = rememberDraggableState {
        offset.value = onTabDrag(editor, offset.value + it)
    }

    val onStopDrag: suspend CoroutineScope.(Float) -> Unit = {
        offset.value = 0f
    }

    val isPlayerUUID: (String) -> Boolean = lambda@ {
        if (it.toUUID() == null) return@lambda false
        if (worldSpec.playerNames[it] == null) return@lambda false

        return@lambda true
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .hoverable(interaction)
            .clickable(interaction, null) { onSelectTab(editor) }
            .offset { IntOffset(x = offset.value.toInt(), y = 0) }
            .zIndex(if (offset.value != 0f) 10f else 0f)
            .onGloballyPositioned { onCalculateTabWidth(editor, it.size.width) }
            .draggable(
                state = draggableState,
                orientation = Orientation.Horizontal,
                enabled = true,
                onDragStopped = onStopDrag
            )
            .drawBehind {
                drawRect(DoodlerTheme.Colors.SecondaryBackground)
                drawRect(DoodlerTheme.Colors.Editor.Tab(selectedProvider(), pressed, hovered))

                if (!selectedProvider()) return@drawBehind
                drawRect(
                    color = DoodlerTheme.Colors.Primary,
                    topLeft = Offset(0f, size.height - 2.ddp.value),
                    size = Size(size.width, 1.8.ddp.value)
                )
            }
            .height(20.ddp).wrapContentWidth()
    ) {
        Spacer(modifier = Modifier.width(7.65.ddp))

        Row(verticalAlignment = Alignment.Bottom) {

            if (editor is AnvilNbtEditor) {
                Text(
                    text = editor.path,
                    color = DoodlerTheme.Colors.Editor.TabPath,
                    fontSize = 7.5.dsp
                )
                Spacer(modifier = Modifier.width(2.7.ddp))
            }

            Text(
                text = editor.name.split(".")[0].let {
                    if (isPlayerUUID(it)) {
                        val text = "${worldSpec.playerNames.getValue(it)}[...uuid].dat"
                        AnnotatedString(
                            text = text,
                            spanStyles = listOf(
                                AnnotatedString.Range(
                                    item = SpanStyle(color = Color.Gray, fontSize = 6.dsp),
                                    start = text.length - 13,
                                    end = text.length - 4
                                )
                            )
                        )
                    } else AnnotatedString(editor.name)
                },
                color =
                if (editor is NbtEditor && editor.state.actionFlags.canBeSaved) DoodlerTheme.Colors.Editor.TabHasChanges
                else DoodlerTheme.Colors.Text.IdeGeneral,
                fontSize = 9.dsp
            )

        }

        Spacer(modifier = Modifier.width(3.6.ddp))

        EditorCloseButton { onCloseTab(editor) }

        Spacer(modifier = Modifier.width(5.4.ddp))
    }
}

@Composable
fun EditorCloseButton(
    close: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    Box(
        modifier = Modifier
            .drawBehind {
                drawCircle(DoodlerTheme.Colors.Editor.TabCloseButtonBackground(hovered), size.width / 2f)
            }
    ) {
        Text(
            text = "\u2715",
            fontWeight = FontWeight.Bold,
            fontSize = 9.dsp,
            color = DoodlerTheme.Colors.Editor.TabCloseButton(hovered),
            modifier = Modifier
                .padding(start = 2.2.ddp, bottom = 2.2.ddp, top = 1.5.ddp, end = 1.5.ddp)
                .hoverable(interaction)
                .clickable { close() }
        )
    }
}
