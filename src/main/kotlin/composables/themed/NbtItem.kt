package composables.themed

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import composables.states.editor.world.Doodle
import composables.states.editor.world.DoodleUi
import composables.states.editor.world.NbtDoodle
import composables.states.editor.world.ValueDoodle

import doodler.nbt.TagType

@Composable
private fun ItemRoot(
    pressed: Boolean,
    selected: Boolean,
    focused: Boolean,
    content: @Composable RowScope.() -> Unit
) {
    Box (
        modifier = Modifier
            .background(ThemedColor.Editor.item(selected, pressed, focused))
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
    Box (
        modifier = Modifier
            .wrapContentSize()
            .background(ThemedColor.Editor.indicator(selected), RoundedCornerShape(5.dp))
            .padding(top = 2.dp, bottom = 2.dp, start = 3.dp, end = 3.dp),
        content = content
    )
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
private fun ToolBarItemIndicator(disabled: Boolean, onClick: MouseClickScope.() -> Unit, content: @Composable BoxScope.() -> Unit) {
    var hover by remember { mutableStateOf(false) }

    Box (
        modifier = Modifier
            .wrapContentSize()
            .onPointerEvent(PointerEventType.Enter) { if (!disabled) hover = true }
            .onPointerEvent(PointerEventType.Exit) { if (!disabled) hover = false }
            .mouseClickable(onClick = if (disabled) ({ }) else onClick)
            .padding(top = 5.dp, bottom = 5.dp)
            .background(
                if (hover) ThemedColor.from(Color.Black, alpha = 30)
                else Color.Transparent, RoundedCornerShape(3.dp)
            )
            .alpha(if (disabled) 0.3f else 1f)
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
    IndicatorText(" $typeString ", color = ThemedColor.Editor.Tag.Number)
}

@Composable
private fun StringTypeIndicator() {
    IndicatorText("...", color = ThemedColor.Editor.Tag.String)
}

@Composable
private fun CompoundTypeIndicator() {
    IndicatorText("{ }", color = ThemedColor.Editor.Tag.Compound)
}

@Composable
private fun ListTypeIndicator() {
    IndicatorText("[ ]", color = ThemedColor.Editor.Tag.List)
}

@Composable
private fun ArrayTypeIndicator(typeString: String) {
    IndicatorText("[$typeString]", color = ThemedColor.Editor.Tag.NumberArray)
}

@Composable
private fun Indicator(type: TagType, selected: Boolean) {
    ItemIndicator (selected) {
        IndicatorText(type)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ToolBarIndicator(type: TagType, onClick: MouseClickScope.() -> Unit = { }) {
    ToolBarItemIndicator (false, onClick) {
        IndicatorText(type)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ToolBarAction(disabled: Boolean = false, onClick: MouseClickScope.() -> Unit = { }, content: @Composable () -> Unit) {
    ToolBarItemIndicator (disabled, onClick) {
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
        TagType.TAG_END -> { /* impossible */ }
    }
}

@Composable
private fun ItemText(text: String, color: Color, fontSize: TextUnit = 20.sp) {
    Text(text, color = color, fontFamily = JetBrainsMono, fontSize = fontSize)
}

@Composable
private fun Key(key: String) {
    ItemText(key, ThemedColor.Editor.Tag.General)
}

@Composable
private fun NumberValue(value: String) {
    ItemText(value, ThemedColor.Editor.Tag.Number)
}

@Composable
private fun StringValue(value: String) {
    ItemText(value, ThemedColor.Editor.Tag.String)
}

@Composable
private fun ExpandableValue(value: String, selected: Boolean) {
    Box (
        modifier = Modifier
            .wrapContentSize()
            .background(ThemedColor.Editor.indicator(selected), RoundedCornerShape(5.dp))
            .padding(top = 2.dp, bottom = 2.dp, start = 10.dp, end = 10.dp)
    ) {
        ItemText(value, ThemedColor.Editor.indicatorText(selected), 18.sp)
    }
}

@Composable
private fun Index(index: Int, selected: Boolean) {
    Box (
        modifier = Modifier
            .wrapContentSize()
            .background(ThemedColor.Editor.indicator(selected), shape = RoundedCornerShape(5.dp))
            .padding(top = 2.dp, bottom = 2.dp, start = 5.dp, end = 5.dp)
    ) {
        ItemText("$index:", ThemedColor.Editor.indicatorText(selected), 18.sp)
    }
}

@Composable
private fun KeyValue(doodle: NbtDoodle, selected: Boolean) {
    val key = doodle.name
    if (key != null) {
        Key(key)
        Spacer(modifier = Modifier.width(20.dp))
    }
    if ((doodle.parent?.tag?.type ?: TagType.TAG_COMPOUND) != TagType.TAG_COMPOUND && doodle.index >= 0) {
        Index(doodle.index, selected)
        Spacer(modifier = Modifier.width(10.dp))
    }
    when (doodle.tag.type) {
        TagType.TAG_BYTE,
            TagType.TAG_DOUBLE,
            TagType.TAG_FLOAT,
            TagType.TAG_INT,
            TagType.TAG_LONG,
            TagType.TAG_SHORT -> NumberValue(doodle.value)
        TagType.TAG_STRING -> StringValue(doodle.value)
        TagType.TAG_COMPOUND,
            TagType.TAG_BYTE_ARRAY,
            TagType.TAG_INT_ARRAY,
            TagType.TAG_LONG_ARRAY,
            TagType.TAG_LIST -> ExpandableValue(doodle.value, selected)
        TagType.TAG_END -> { /* impossible */ }
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun NbtItemTreeView(
    doodle: Doodle,
    state: DoodleUi,
    scrollTo: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    var pressed by remember { mutableStateOf(false) }

    Box (modifier = Modifier
        .background(ThemedColor.EditorArea)
        .wrapContentSize()
        .zIndex(999f)
    ) {
        Box(modifier = Modifier
            .border(2.dp, ThemedColor.Editor.TreeBorder)
            .onPointerEvent(PointerEventType.Enter) { focused = true; state.focusTreeView(doodle) }
            .onPointerEvent(PointerEventType.Exit) { focused = false; state.unFocusTreeView(doodle) }
            .onPointerEvent(PointerEventType.Press) { pressed = true }
            .onPointerEvent(PointerEventType.Release) { pressed = false }
            .mouseClickable(onClick = { scrollTo() })
            .background(ThemedColor.Editor.normalItem(pressed, focused))
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
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun NbtItem(
    doodle: Doodle,
    state: DoodleUi,
    toggle: (Doodle) -> Unit,
    select: (Doodle) -> Unit,
    treeCollapse: (NbtDoodle) -> Unit
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
                    .onPointerEvent(PointerEventType.Release) { treeCollapse(current) }
                ) {
                    Spacer(modifier = Modifier.width(40.dp))
                    Box (
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(
                                ThemedColor.Editor.depthLine(
                                    selected,
                                    focused || state.focusedTreeView == current
                                )
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
                        if (buttons.isPrimaryPressed) toggle(doodle)
                        else if (buttons.isSecondaryPressed) select(doodle)
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
            Indicator(doodle.tag.type, selected)
            Spacer(modifier = Modifier.width(20.dp))
            KeyValue(doodle, selected)
        }
        is ValueDoodle -> {
            Index(doodle.index, selected)
            Spacer(modifier = Modifier.width(10.dp))
            NumberValue(doodle.value)
        }
    }
}

fun getHierarchy(doodle: Doodle): List<NbtDoodle> {
    val result = mutableListOf<NbtDoodle>()
    var parent = doodle.parent
    while (parent != null && parent.depth >= 0) {
        result.add(parent)
        parent = parent.parent
    }
    result.reverse()
    return result
}
