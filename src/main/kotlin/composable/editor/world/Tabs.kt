package composable.editor.world

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.zIndex
import doodler.editor.AnvilNbtEditor
import doodler.editor.Editor
import doodler.editor.EditorManager
import doodler.editor.NbtEditor
import doodler.theme.DoodlerTheme
import doodler.types.BooleanProvider
import doodler.unit.dp

@Composable
fun EditorTabGroup(
    manager: EditorManager,
    onSelectTab: (Editor) -> Unit,
    onCloseTab: (Editor) -> Unit
) {
    val scrollState = rememberScrollState()

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
                EditorTab(
                    editor = editor,
                    selectedProvider = { manager.selected == editor },
                    onSelectTab = onSelectTab,
                    onCloseTab = onCloseTab
                )
            }
        }
    }
}

@Composable
fun EditorTab(
    editor: Editor,
    selectedProvider: BooleanProvider,
    onSelectTab: (Editor) -> Unit,
    onCloseTab: (Editor) -> Unit
) {
    val hoverInteractionSource = remember { MutableInteractionSource() }
    val pressInteractionSource = remember { MutableInteractionSource() }

    val hovered by hoverInteractionSource.collectIsHoveredAsState()
    val pressed by pressInteractionSource.collectIsPressedAsState()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .hoverable(hoverInteractionSource)
            .clickable(pressInteractionSource, null) { onSelectTab(editor) }
            .drawBehind {
                drawRect(DoodlerTheme.Colors.Editor.Tab(selectedProvider(), pressed, hovered))

                if (!selectedProvider()) return@drawBehind
                drawRect(
                    color = DoodlerTheme.Colors.Primary,
                    topLeft = Offset(0f, size.height - 2.dp.value),
                    size = Size(size.width, 2.dp.value)
                )
            }
            .height(25.dp).wrapContentWidth()
    ) {
        Spacer(modifier = Modifier.width(6.dp))

        Row(verticalAlignment = Alignment.Bottom) {

            if (editor is AnvilNbtEditor) {
                Text(
                    text = editor.path,
                    color = DoodlerTheme.Colors.Editor.TabPath,
                    fontSize = MaterialTheme.typography.h6.fontSize
                )
                Spacer(modifier = Modifier.width(3.dp))
            }

            Text(
                text = editor.name,
                color =
                if (editor is NbtEditor && editor.state.actionFlags.canBeSaved) DoodlerTheme.Colors.Editor.TabHasChanges
                else DoodlerTheme.Colors.Text.IdeGeneral,
                fontSize = MaterialTheme.typography.h5.fontSize
            )

        }

        Spacer(modifier = Modifier.width(4.dp))

        EditorCloseButton { onCloseTab(editor) }

        Spacer(modifier = Modifier.width(6.dp))
    }
}

@Composable
fun EditorCloseButton(
    close: () -> Unit
) {
    val hoverInteractionSource = remember { MutableInteractionSource() }
    val hovered by hoverInteractionSource.collectIsHoveredAsState()

    Box(
        modifier = Modifier
            .drawBehind {
                drawCircle(DoodlerTheme.Colors.Editor.TabCloseButtonBackground(hovered), size.width / 2f)
            }
    ) {
        Text(
            text = "\u2715",
            fontWeight = FontWeight.Bold,
            fontSize = MaterialTheme.typography.h5.fontSize,
            color = DoodlerTheme.Colors.Editor.TabCloseButton(hovered),
            modifier = Modifier
                .padding(3.dp)
                .hoverable(hoverInteractionSource)
                .clickable { close() }
        )
    }
}
