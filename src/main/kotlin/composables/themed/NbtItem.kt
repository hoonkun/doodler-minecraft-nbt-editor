package composables.themed

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.mouseClickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import doodler.doodle.Doodle
import doodler.doodle.DoodleState
import doodler.doodle.NbtDoodle
import doodler.doodle.PrimitiveValueDoodle
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
            .background(ThemedColor.selectable(
                selected = selected,
                press = pressed,
                hover = focused
            ))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            content()
        }
    }

}

@Composable
private fun ItemIndicator(content: @Composable BoxScope.() -> Unit) {
    Box (
        modifier = Modifier
            .wrapContentSize()
            .background(Color(60, 60, 60), shape = RoundedCornerShape(5.dp))
            .padding(top = 2.dp, bottom = 2.dp, start = 3.dp, end = 3.dp),
        content = content
    )
}

@Composable
private fun IndicatorText(text: String, color: Color) {
    Text(text, color = color, fontFamily = JetBrainsMono, fontSize = 18.sp)
}

@Composable
private fun NumberTypeIndicator(typeString: String) {
    ItemIndicator {
        IndicatorText(" $typeString ", color = Color(104, 151, 187))
    }
}

@Composable
private fun StringTypeIndicator() {
    ItemIndicator {
        IndicatorText("...", color = Color(106, 135, 89))
    }
}

@Composable
private fun CompoundTypeIndicator() {
    ItemIndicator {
        IndicatorText("{C}", color = Color(255, 199, 109))
    }
}

@Composable
private fun ListTypeIndicator() {
    ItemIndicator {
        IndicatorText("[ ]", color = Color(204, 120, 50))
    }
}

@Composable
private fun ArrayTypeIndicator(typeString: String) {
    ItemIndicator {
        IndicatorText("[$typeString]", color = Color(104, 151, 187))
    }
}

@Composable
private fun Indicator(type: TagType) {
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
        TagType.TAG_END -> { /* impossible */ }
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
private fun ExpandableValue(value: String) {
    Box (
        modifier = Modifier
            .wrapContentSize()
            .background(Color(60, 60, 60), shape = RoundedCornerShape(5.dp))
            .padding(top = 2.dp, bottom = 2.dp, start = 10.dp, end = 10.dp)
    ) {
        ItemText(value, Color(150, 150, 150), 18.sp)
    }
}

@Composable
private fun Index(index: Int) {
    Box (
        modifier = Modifier
            .wrapContentSize()
            .background(Color(60, 60, 60), shape = RoundedCornerShape(5.dp))
            .padding(top = 2.dp, bottom = 2.dp, start = 5.dp, end = 5.dp)
    ) {
        ItemText("$index:", Color(150, 150, 150), 18.sp)
    }
}

@Composable
private fun KeyValue(type: TagType, key: String?, value: String, index: Int) {
    if (key != null) {
        Key(key)
        Spacer(modifier = Modifier.width(20.dp))
    }
    if (index >= 0) {
        Index(index)
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
            TagType.TAG_LIST -> ExpandableValue(value)
        TagType.TAG_END -> { /* impossible */ }
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

    ItemRoot(
        state.pressed == doodle,
        state.selected.contains(doodle),
        state.focusedDirectly == doodle || state.focusedTree == doodle
    ) {
        Row (
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 20.dp).height(60.dp)
        ) {
            for (i in 0 until doodle.depth) {
                val focused = state.focusedDirectly == hierarchy[i] || state.focusedTree == hierarchy[i]
                Box (modifier = Modifier
                    .fillMaxHeight()
                    .wrapContentWidth()
                    .onPointerEvent(PointerEventType.Enter) { state.focusTree(hierarchy[i]) }
                    .onPointerEvent(PointerEventType.Exit) { state.unFocusTree(hierarchy[i]) }
                    .onPointerEvent(PointerEventType.Release) { treeCollapse(hierarchy[i], hierarchy[i].collapse()) }
                ) {
                    Spacer(modifier = Modifier.width(50.dp))
                    Box (
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(
                                if (focused) Color(100, 100, 100)
                                else Color(60, 60, 60)
                            )
                    ) { }
                }
            }
            Box (
                modifier = Modifier.weight(1f)
                    .onPointerEvent(PointerEventType.Enter) { state.focusDirectly(doodle) }
                    .onPointerEvent(PointerEventType.Exit) { state.unFocusDirectly(doodle) }
                    .onPointerEvent(PointerEventType.Press) { state.press(doodle) }
                    .onPointerEvent(PointerEventType.Release) { state.unPress(doodle) }
                    .mouseClickable(onClick = { if (buttons.isPrimaryPressed) onExpand() })
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(top = 10.dp, bottom = 10.dp)
                        .fillMaxHeight()
                ) {
                    when (doodle) {
                        is NbtDoodle -> {
                            Indicator(doodle.type)
                            Spacer(modifier = Modifier.width(20.dp))
                            KeyValue(doodle.type, doodle.name, doodle.value, doodle.index)
                        }
                        is PrimitiveValueDoodle -> {
                            Index(doodle.index)
                            Spacer(modifier = Modifier.width(10.dp))
                            NumberValue(doodle.value)
                        }
                    }
                }
            }
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
