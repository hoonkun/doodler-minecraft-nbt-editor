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
import composables.main.NbtTab
import composables.main.FileEditorTab

@Composable
fun TabGroup(
    tabs: List<TabData>,
    onSelectTab: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    Box (modifier = Modifier.background(Color(50, 51, 53)).fillMaxWidth().wrapContentHeight()) {
        Row(modifier = Modifier.wrapContentWidth().horizontalScroll(scrollState)) {
            for (data in tabs) {
                Tab(data, onSelectTab)
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun Tab(
    data: TabData,
    onSelectTab: (String) -> Unit
) {
    var hover by remember { mutableStateOf(false) }
    var press by remember { mutableStateOf(false) }

    Box (
        modifier = Modifier
            .wrapContentSize()
            .onPointerEvent(PointerEventType.Press) { press = true }
            .onPointerEvent(PointerEventType.Release) { press = false; onSelectTab(data.tab.name); }
            .onPointerEvent(PointerEventType.Enter) { hover = true }
            .onPointerEvent(PointerEventType.Exit) { hover = false }
            .background(
                if (press) Color(255, 255, 255, 40.plus(if (data.selected) 10 else 0))
                else if (hover) Color(255, 255, 255, 25.plus(if (data.selected) 10 else 0))
                else Color(255, 255, 255, if (data.selected) 20 else 0)
            )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Spacer(modifier = Modifier.width(20.dp))
                Text(
                    data.tab.name,
                    color = if (data.tab is NbtTab && data.tab.content.hasChanges) Color(255, 160, 0) else Color.White,
                    fontSize = 22.sp
                )
                if (data.tab.name != "+") {
                    Spacer(modifier = Modifier.width(15.dp))
                    LinkText("close", color = Color(255, 255, 255, 65), fontSize = 20.sp) {
                        data.tab.close()
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
    val tab: FileEditorTab
)
