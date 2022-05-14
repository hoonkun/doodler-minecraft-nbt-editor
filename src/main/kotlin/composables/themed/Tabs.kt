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
import composables.states.holder.NbtSpecies
import composables.states.holder.Species
import composables.themed.ThemedColor.Companion.selectable

@Composable
fun ColumnScope.TabGroup(
    tabs: List<TabData>,
    onSelectEditable: (Species) -> Unit,
    onCloseEditable: (Species) -> Unit
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
    onSelectEditable: (Species) -> Unit,
    onCloseEditable: (Species) -> Unit
) {
    var hover by remember { mutableStateOf(false) }
    var press by remember { mutableStateOf(false) }

    Box (
        modifier = Modifier
            .wrapContentSize()
            .onPointerEvent(PointerEventType.Press) { press = true }
            .onPointerEvent(PointerEventType.Release) { press = false; onSelectEditable(data.species); }
            .onPointerEvent(PointerEventType.Enter) { hover = true }
            .onPointerEvent(PointerEventType.Exit) { hover = false }
            .background(selectable(data.selected, press, hover))
            .height(50.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(modifier = Modifier.width(20.dp))
                Text(
                    data.species.ident,
                    color = if (data.species is NbtSpecies && data.species.hasChanges) Color(255, 160, 0) else Color.White,
                    fontSize = 22.sp,
                    fontFamily = JetBrainsMono
                )
                if (data.species.ident != "+") {
                    Spacer(modifier = Modifier.width(15.dp))
                    CloseButton { onCloseEditable(data.species) }
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

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun RowScope.CloseButton(onCloseEditable: () -> Unit) {
    var hover by remember { mutableStateOf(false) }

    Box (
        modifier = Modifier
            .onPointerEvent(PointerEventType.Enter) { hover = true }
            .onPointerEvent(PointerEventType.Exit) { hover = false }
            .mouseClickable(onClick = { onCloseEditable() })
            .background(if (hover) Color(255, 255, 255, 75) else Color.Transparent, CircleShape)
            .width(26.dp).height(26.dp)
    ) {
        Text (
            "\u2715",
            color = if (hover) Color(0, 0, 0, 200) else Color(255, 255, 255, 100),
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            modifier = Modifier
                .align(Alignment.Center)
        )
    }
}

class TabData(
    val selected: Boolean,
    val species: Species
)
