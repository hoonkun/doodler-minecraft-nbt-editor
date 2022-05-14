package composables.themed

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import doodler.doodle.*
import nbt.TagType

@Composable
private fun ItemRoot(
    pressed: Boolean,
    selected: Boolean,
    focused: Boolean,
    content: @Composable RowScope.() -> Unit
) {
    Box (
        modifier = Modifier
            .background(
                if (selected) {
                    if (pressed) Color(91, 115, 65, 50)
                    else if (focused) Color(91, 115, 65, 40)
                    else Color(91, 115, 65, 40)
                } else {
                    if (pressed) Color(0, 0, 0, 80)
                    else if (focused) Color(0, 0, 0, 40)
                    else Color.Transparent
                }
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            content()
        }
    }

}

@Composable
private fun ItemIndicator(selected: Boolean, content: @Composable BoxScope.() -> Unit) {
    val color =
        if (selected) Color(70, 70, 70)
        else Color(60, 60, 60)
    Box (
        modifier = Modifier
            .wrapContentSize()
            .background(color, shape = RoundedCornerShape(5.dp))
            .padding(top = 2.dp, bottom = 2.dp, start = 3.dp, end = 3.dp),
        content = content
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ToolBarItemIndicator(content: @Composable BoxScope.() -> Unit) {
    var hover by remember { mutableStateOf(false) }

    Box (
        modifier = Modifier
            .wrapContentSize()
            .onPointerEvent(PointerEventType.Enter) { hover = true }
            .onPointerEvent(PointerEventType.Exit) { hover = false }
            .padding(top = 5.dp, bottom = 5.dp)
            .background(if (hover) Color(0, 0, 0, 30) else Color.Transparent, RoundedCornerShape(3.dp))
    ) {
        Box(
            modifier = Modifier
                .wrapContentSize()
                .padding(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .wrapContentSize()
                    .padding(top = 2.dp, bottom = 2.dp, start = 3.dp, end = 3.dp),
                content = content
            )
        }
    }
}

@Composable
fun IndicatorText(text: String, color: Color) {
    Text(text, color = color, fontFamily = JetBrainsMono, fontSize = 18.sp)
}

@Composable
private fun NumberTypeIndicator(typeString: String) {
    IndicatorText(" $typeString ", color = Color(104, 151, 187))
}

@Composable
private fun StringTypeIndicator() {
    IndicatorText("...", color = Color(106, 135, 89))
}

@Composable
private fun CompoundTypeIndicator() {
    IndicatorText("{ }", color = Color(255, 199, 109))
}

@Composable
private fun ListTypeIndicator() {
    IndicatorText("[ ]", color = Color(204, 120, 50))
}

@Composable
private fun ArrayTypeIndicator(typeString: String) {
    IndicatorText("[$typeString]", color = Color(104, 151, 187))
}

@Composable
private fun Indicator(type: TagType, selected: Boolean) {
    ItemIndicator (selected) {
        IndicatorText(type)
    }
}

@Composable
fun ToolBarIndicator(type: TagType) {
    ToolBarItemIndicator {
        IndicatorText(type)
    }
}

@Composable
fun ToolBarAction(content: @Composable () -> Unit) {
    ToolBarItemIndicator {
        content()
    }
}

@Composable
fun IndicatorText(type: TagType) {
    when (type) {
        TagType.TAG_BYTE -> NumberTypeIndicator("B")
        TagType.TAG_INT -> NumberTypeIndicator("I")
        TagType.TAG_SHORT -> NumberTypeIndicator("S")
        TagType.TAG_FLOAT -> NumberTypeIndicator("F")
        TagType.TAG_DOUBLE -> NumberTypeIndicator("D")
        TagType.TAG_LONG -> NumberTypeIndicator("L")
        TagType.TAG_BYTE_ARRAY -> ArrayTypeIndicator("B")
        TagType.TAG_INT_ARRAY -> ArrayTypeIndicator("I")
        TagType.TAG_LONG_ARRAY -> ArrayTypeIndicator("L")
        TagType.TAG_STRING -> StringTypeIndicator()
        TagType.TAG_COMPOUND -> CompoundTypeIndicator()
        TagType.TAG_LIST -> ListTypeIndicator()
        TagType.TAG_END -> { /* impossible */
        }
    }
}

@Composable
private fun ItemText(text: String, color: Color, fontSize: TextUnit = 20.sp) {
    Text(text, color = color, fontFamily = JetBrainsMono, fontSize = fontSize)
}

@Composable
private fun Key(key: String) {
    ItemText(key, Color(169, 183, 198))
}

@Composable
private fun NumberValue(value: String) {
    ItemText(value, Color(104, 151, 187))
}

@Composable
private fun StringValue(value: String) {
    ItemText(value, Color(106, 135, 89))
}

@Composable
private fun ExpandableValue(value: String, selected: Boolean) {
    val color =
        if (selected) Color(70, 70, 70)
        else Color(60, 60, 60)
    val textColor =
        if (selected) Color(157, 157, 157)
        else Color(150, 150, 150)

    Box (
        modifier = Modifier
            .wrapContentSize()
            .background(color, shape = RoundedCornerShape(5.dp))
            .padding(top = 2.dp, bottom = 2.dp, start = 10.dp, end = 10.dp)
    ) {
        ItemText(value, textColor, 18.sp)
    }
}

@Composable
private fun Index(index: Int, selected: Boolean) {
    val color =
        if (selected) Color(70, 70, 70)
        else Color(60, 60, 60)
    val textColor =
        if (selected) Color(157, 157, 157)
        else Color(150, 150, 150)

    Box (
        modifier = Modifier
            .wrapContentSize()
            .background(color, shape = RoundedCornerShape(5.dp))
            .padding(top = 2.dp, bottom = 2.dp, start = 5.dp, end = 5.dp)
    ) {
        ItemText("$index:", textColor, 18.sp)
    }
}

@Composable
private fun KeyValue(type: TagType, key: String?, value: String, index: Int, selected: Boolean) {
    if (key != null) {
        Key(key)
        Spacer(modifier = Modifier.width(20.dp))
    }
    if (index >= 0) {
        Index(index, selected)
        Spacer(modifier = Modifier.width(10.dp))
    }
    when (type) {
        TagType.TAG_BYTE,
            TagType.TAG_DOUBLE,
            TagType.TAG_FLOAT,
            TagType.TAG_INT,
            TagType.TAG_LONG,
            TagType.TAG_SHORT -> NumberValue(value)
        TagType.TAG_STRING -> StringValue(value)
        TagType.TAG_COMPOUND,
            TagType.TAG_BYTE_ARRAY,
            TagType.TAG_INT_ARRAY,
            TagType.TAG_LONG_ARRAY,
            TagType.TAG_LIST -> ExpandableValue(value, selected)
        TagType.TAG_END -> { /* impossible */ }
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun NbtItemTreeView(
    doodle: Doodle,
    state: DoodleState,
    scrollTo: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    var pressed by remember { mutableStateOf(false) }

    val color = if (pressed) Color(30, 30, 30)
    else if (focused) Color(36, 36, 36)
    else Color(43, 43, 43)

    Box (modifier = Modifier
        .border(2.dp, Color(100, 100, 100))
        .onPointerEvent(PointerEventType.Enter) { focused = true; state.focusTreeView(doodle) }
        .onPointerEvent(PointerEventType.Exit) { focused = false; state.unFocusTreeView(doodle) }
        .onPointerEvent(PointerEventType.Press) { pressed = true }
        .onPointerEvent(PointerEventType.Release) { pressed = false }
        .mouseClickable(onClick = { scrollTo() })
        .background(color)
        .zIndex(999f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(start = (20 + 40 * doodle.depth).dp)
                .fillMaxWidth()
                .height(50.dp)
        ) {
            DoodleContent(doodle, false)
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun NbtItem(
    doodle: Doodle,
    onSelect: () -> Unit,
    onExpand: () -> Unit,
    state: DoodleState,
    treeCollapse: (Doodle, Int) -> Unit
) {
    val hierarchy = getHierarchy(doodle)

    val selected = state.selected.contains(doodle)

    ItemRoot(
        state.pressed == doodle,
        selected,
        state.focusedDirectly == doodle || state.focusedTree == doodle
    ) {
        Row (
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 20.dp).height(50.dp)
        ) {
            for (i in 0 until doodle.depth) {
                val current = hierarchy[i]
                val focused = state.focusedDirectly == current || state.focusedTree == current
                Box (modifier = Modifier
                    .fillMaxHeight()
                    .wrapContentWidth()
                    .onPointerEvent(PointerEventType.Enter) { state.focusTree(current) }
                    .onPointerEvent(PointerEventType.Exit) { state.unFocusTree(current) }
                    .onPointerEvent(PointerEventType.Release) { treeCollapse(current, current.collapse()) }
                ) {
                    val co = if (selected) 16 else 0

                    Spacer(modifier = Modifier.width(40.dp))
                    Box (
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(
                                if (focused || state.focusedTreeView == current) Color(100 + co, 100 + co, 100 + co)
                                else Color(60 + co, 60 + co, 60 + co)
                            )
                    ) { }
                }
            }
            Box (
                modifier = Modifier.weight(1f)
                    .onPointerEvent(PointerEventType.Enter) { state.focusDirectly(doodle); state.focusedTree = null }
                    .onPointerEvent(PointerEventType.Exit) { state.unFocusDirectly(doodle) }
                    .onPointerEvent(PointerEventType.Press) { state.press(doodle) }
                    .onPointerEvent(PointerEventType.Release) { state.unPress(doodle) }
                    .mouseClickable(onClick = {
                        if (buttons.isPrimaryPressed) onExpand()
                        else if (buttons.isSecondaryPressed) onSelect()
                    })
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxHeight()
                ) {
                    DoodleContent(doodle, selected)
                }
            }
        }
    }
}

@Composable
fun DoodleContent(doodle: Doodle, selected: Boolean) {
    when (doodle) {
        is NbtDoodle -> {
            Indicator(doodle.type, selected)
            Spacer(modifier = Modifier.width(20.dp))
            KeyValue(doodle.type, doodle.name, doodle.value, doodle.index, selected)
        }
        is PrimitiveValueDoodle -> {
            Index(doodle.index, selected)
            Spacer(modifier = Modifier.width(10.dp))
            NumberValue(doodle.value)
        }
    }
}

fun getHierarchy(doodle: Doodle): List<NbtDoodle> {
    val result = mutableListOf<NbtDoodle>()
    var parent = doodle.parentTag
    while (parent != null) {
        result.add(parent)
        parent = parent.parentTag
    }
    result.reverse()
    return result
}
