package composables.themed

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import doodler.editor.Editor
import composables.themed.ThemedColor.Companion.selectable

@Composable
fun ColumnScope.SpeciesTabGroup(
    tabs: List<TabData>,
    onSelectEditable: (Editor) -> Unit,
    onCloseEditable: (Editor) -> Unit
) {
    val scrollState = rememberScrollState()

    Box (modifier = Modifier.background(ThemedColor.TabBar).fillMaxWidth().wrapContentHeight().zIndex(10f)) {
        Row(modifier = Modifier.wrapContentWidth().horizontalScroll(scrollState)) {
            for (data in tabs) {
                SpeciesTab(data, onSelectEditable, onCloseEditable)
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SpeciesTab(
    data: TabData,
    onSelectEditable: (Editor) -> Unit,
    onCloseEditable: (Editor) -> Unit
) {
    var hover by remember { mutableStateOf(false) }
    var press by remember { mutableStateOf(false) }

    Box (
        modifier = Modifier
            .wrapContentSize()
            .onPointerEvent(PointerEventType.Press) { press = true }
            .onPointerEvent(PointerEventType.Release) { press = false; onSelectEditable(data.item); }
            .onPointerEvent(PointerEventType.Enter) { hover = true }
            .onPointerEvent(PointerEventType.Exit) { hover = false }
            .background(selectable(data.selected, press, hover))
            .height(40.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(modifier = Modifier.width(15.dp))
                Text(
                    data.item.name,
                    color = ThemedColor.Editor.Tag.General,
                    fontSize = 19.sp,
                    fontFamily = JetBrainsMono
                )
                if (data.item.ident != "+") {
                    Spacer(modifier = Modifier.width(10.dp))
                    CloseButton { onCloseEditable(data.item) }
                }
                Spacer(modifier = Modifier.width(13.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
        }
        Box (modifier = Modifier.matchParentSize()) {
            Box (
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(if (data.selected) ThemedColor.Bright else Color.Transparent)
            ) { }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
private fun RowScope.CloseButton(onCloseEditable: () -> Unit) {
    var hover by remember { mutableStateOf(false) }

    Box (
        modifier = Modifier
            .onPointerEvent(PointerEventType.Enter) { hover = true }
            .onPointerEvent(PointerEventType.Exit) { hover = false }
            .mouseClickable(onClick = { onCloseEditable() })
            .background(ThemedColor.Editor.tabCloseButtonBackground(hover), CircleShape)
            .width(21.dp).height(21.dp)
    ) {
        Text (
            "\u2715",
            color = ThemedColor.Editor.tabCloseButtonIcon(hover),
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            modifier = Modifier
                .align(Alignment.Center)
        )
    }
}

class TabData(
    val selected: Boolean,
    val item: Editor
)
