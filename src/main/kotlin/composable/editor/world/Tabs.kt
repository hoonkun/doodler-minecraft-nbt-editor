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
            .background(DoodlerTheme.Colors.Primary)
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

    Box(
        modifier = Modifier
            .hoverable(hoverInteractionSource)
            .clickable(pressInteractionSource, null) { onSelectTab(editor) }
            .padding(vertical = 5.dp, horizontal = 12.dp)
            .drawBehind {
                drawRect(DoodlerTheme.Colors.Editor.Tab(selectedProvider(), pressed, hovered))

                if (!selectedProvider()) return@drawBehind
                drawRect(
                    color = DoodlerTheme.Colors.Primary,
                    topLeft = Offset(0f, size.height - 1.5f.dp.value),
                    size = Size(size.width, 1.5f.dp.value)
                )
            }
            .height(30.dp).wrapContentWidth()
    ) {

        if (editor is AnvilNbtEditor) {
            Text(
                text = editor.path,
                color = DoodlerTheme.Colors.Editor.TabPath,
                fontSize = MaterialTheme.typography.h6.fontSize,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
            Spacer(modifier = Modifier.width(3.dp))
        }

        Text(
            text = editor.name,
            color =
                if (editor is NbtEditor && editor.state.actionFlags.canBeSaved) DoodlerTheme.Colors.Editor.TabHasChanges
                else DoodlerTheme.Colors.Text.IdeGeneral,
            fontSize = MaterialTheme.typography.h4.fontSize,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
        Spacer(modifier = Modifier.width(7.dp))

        EditorCloseButton { onCloseTab(editor) }

    }
}

@Composable
fun EditorCloseButton(
    close: () -> Unit
) {
    val hoverInteractionSource = remember { MutableInteractionSource() }
    val hovered by hoverInteractionSource.collectIsHoveredAsState()

    Text(
        text = "\u2715",
        fontWeight = FontWeight.Bold,
        fontSize = MaterialTheme.typography.h5.fontSize,
        color = DoodlerTheme.Colors.Editor.TabCloseButton(hovered),
        modifier = Modifier
            .padding(3.dp)
            .hoverable(hoverInteractionSource)
            .clickable { close() }
            .drawBehind {
                drawCircle(DoodlerTheme.Colors.Editor.TabCloseButtonBackground(hovered), size.width / 2f)
            }
    )
}
