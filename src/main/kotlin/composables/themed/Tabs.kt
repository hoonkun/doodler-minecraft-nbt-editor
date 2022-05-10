package composables.themed

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import composables.main.Editable
import composables.themed.ThemedColor.Companion.selectable

@Composable
fun ColumnScope.TabGroup(
    tabs: List<TabData>,
    onSelectEditable: (Editable) -> Unit,
    onCloseEditable: (Editable) -> Unit
) {
    val scrollState = rememberScrollState()

    Box (modifier = Modifier.background(Color(50, 51, 53)).fillMaxWidth().wrapContentHeight()) {
        Row(modifier = Modifier.wrapContentWidth().horizontalScroll(scrollState)) {
            for (data in tabs) {
                Tab(data, onSelectEditable, onCloseEditable)
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun Tab(
    data: TabData,
    onSelectEditable: (Editable) -> Unit,
    onCloseEditable: (Editable) -> Unit
) {
    var hover by remember { mutableStateOf(false) }
    var press by remember { mutableStateOf(false) }

    Box (
        modifier = Modifier
            .wrapContentSize()
            .onPointerEvent(PointerEventType.Press) { press = true }
            .onPointerEvent(PointerEventType.Release) { press = false; onSelectEditable(data.editable); }
            .onPointerEvent(PointerEventType.Enter) { hover = true }
            .onPointerEvent(PointerEventType.Exit) { hover = false }
            .background(selectable(data.selected, press, hover))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Spacer(modifier = Modifier.width(20.dp))
                Text(
                    data.editable.ident,
                    color = if (data.editable.hasChanges) Color(255, 160, 0) else Color.White,
                    fontSize = 22.sp
                )
                if (data.editable.ident != "+") {
                    Spacer(modifier = Modifier.width(15.dp))
                    LinkText("close", color = Color(255, 255, 255, 65), fontSize = 20.sp) {
                        onCloseEditable(data.editable)
                    }
                }
                Spacer(modifier = Modifier.width(18.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
        Box (modifier = Modifier.matchParentSize()) {
            Box (
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(if (data.selected) ThemedColor.Bright else Color.Transparent)
            ) { }
        }
    }
}

class TabData(
    val selected: Boolean,
    val editable: Editable
)
