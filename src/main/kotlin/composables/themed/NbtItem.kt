package composables.themed

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import composables.states.editor.world.*

import doodler.nbt.TagType
import doodler.nbt.tag.*

@Composable
private fun ItemRoot(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Box (
        modifier = modifier
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
    doodle: ActualDoodle,
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
    doodle: ActualDoodle,
    state: DoodleUi,
    toggle: (ActualDoodle) -> Unit,
    select: (ActualDoodle) -> Unit,
    treeCollapse: (NbtDoodle) -> Unit,
    onCreationMode: Boolean = false,
    disabled: Boolean = false
) {
    val hierarchy = getHierarchy(doodle)

    val selected = state.selected.contains(doodle)

    ItemRoot(
        modifier = Modifier
            .alpha(if (disabled) 0.4f else 1f)
            .background(ThemedColor.Editor.item(
                selected = selected,
                pressed = state.pressed == doodle,
                focused = state.focusedDirectly == doodle || state.focusedTree == doodle,
                onCreationMode = onCreationMode,
                alphaMultiplier = if (onCreationMode) 0.6f else 1.0f
            ))
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
                    .let {
                        if (!disabled) it
                                .onPointerEvent(PointerEventType.Enter) { state.focusTree(current) }
                                .onPointerEvent(PointerEventType.Exit) { state.unFocusTree(current) }
                                .onPointerEvent(PointerEventType.Release) { treeCollapse(current) }
                        else it
                    }
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
                    .let {
                        if (!disabled) it
                            .onPointerEvent(PointerEventType.Enter) { state.focusDirectly(doodle); state.focusedTree = null }
                            .onPointerEvent(PointerEventType.Exit) { state.unFocusDirectly(doodle) }
                            .onPointerEvent(PointerEventType.Press) { state.press(doodle) }
                            .onPointerEvent(PointerEventType.Release) { state.unPress(doodle) }
                            .mouseClickable(onClick = {
                                if (buttons.isPrimaryPressed) toggle(doodle)
                                else if (buttons.isSecondaryPressed) select(doodle)
                            })
                        else it
                    }
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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CreatorItem(virtual: VirtualDoodle, state: NbtState) {
    val hierarchy = getHierarchy(virtual)

    ItemRoot(
        modifier = Modifier
            .background(ThemedColor.Editor.item(
                selected = true,
                pressed = false,
                focused = false,
                onCreationMode = true
            ))
    ) {
        Row (
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 20.dp).height(50.dp)
        ) {
            for (i in 0 until virtual.depth) {
                val current = hierarchy[i]
                val focused = state.ui.focusedDirectly == current || state.ui.focusedTree == current
                Box (modifier = Modifier
                    .fillMaxHeight()
                    .wrapContentWidth()
                    .onPointerEvent(PointerEventType.Enter) { state.ui.focusTree(current) }
                    .onPointerEvent(PointerEventType.Exit) { state.ui.unFocusTree(current) }
                ) {
                    Spacer(modifier = Modifier.width(40.dp))
                    Box (
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(
                                ThemedColor.Editor.depthLine(selected = true, focused = focused)
                            )
                    ) { }
                }
            }
            Box (
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxHeight()
                ) {
                    DoodleCreationContent(state, virtual)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RowScope.DoodleCreationContent(state: NbtState, doodle: VirtualDoodle) {
    val intoIndex = doodle.parent.expandedItems.size.coerceAtLeast(doodle.parent.collapsedItems.size)

    val nameState = remember { mutableStateOf("") }
    val valueState = remember { mutableStateOf("") }

    val nameValidState = remember { mutableStateOf(false) }
    val valueValidState = remember { mutableStateOf(doodle !is NbtCreationDoodle || doodle.type.canHaveChildren()) }

    val name by nameState
    val value by valueState

    val nameValid by nameValidState
    val valueValid by valueValidState

    val generateActual: () -> ActualDoodle = {
        val parentTag = doodle.parent.tag
        if (doodle is NbtCreationDoodle) {
            val tag = when (doodle.type) {
                TagType.TAG_BYTE -> ByteTag(value.toByte(), name, parentTag)
                TagType.TAG_SHORT -> ShortTag(value.toShort(), name, parentTag)
                TagType.TAG_INT -> IntTag(value.toInt(), name, parentTag)
                TagType.TAG_LONG -> LongTag(value.toLong(), name, parentTag)
                TagType.TAG_FLOAT -> FloatTag(value.toFloat(), name, parentTag)
                TagType.TAG_DOUBLE -> DoubleTag(value.toDouble(), name, parentTag)
                TagType.TAG_STRING -> StringTag(value, name, parentTag)
                TagType.TAG_BYTE_ARRAY -> ByteArrayTag(ByteArray(0), name, parentTag)
                TagType.TAG_INT_ARRAY -> IntArrayTag(IntArray(0), name, parentTag)
                TagType.TAG_LONG_ARRAY -> LongArrayTag(LongArray(0), name, parentTag)
                TagType.TAG_LIST -> ListTag(TagType.TAG_END, listOf(), true, name, parentTag)
                TagType.TAG_COMPOUND -> CompoundTag(mutableListOf(), name, parentTag)
                TagType.TAG_END -> throw Exception("cannot create END tag!")
            }
            NbtDoodle(tag, doodle.depth, intoIndex, doodle.parent)
        } else {
            ValueDoodle(value, doodle.depth, intoIndex, doodle.parent)
        }
    }

    if (doodle is NbtCreationDoodle) {
        Indicator(doodle.type, true)
        Spacer(modifier = Modifier.width(20.dp))
        CreationField(nameState, nameValidState) {
            when (doodle.type) {
                TagType.TAG_BYTE -> ByteField(valueState, valueValidState)
                TagType.TAG_SHORT -> ShortField(valueState, valueValidState)
                TagType.TAG_INT -> IntField(valueState, valueValidState)
                TagType.TAG_LONG -> LongField(valueState, valueValidState)
                TagType.TAG_FLOAT -> FloatField(valueState, valueValidState)
                TagType.TAG_DOUBLE -> DoubleField(valueState, valueValidState)
                TagType.TAG_STRING -> StringField(valueState, valueValidState)
                TagType.TAG_BYTE_ARRAY -> { ExpandableValue("creates empty ByteArray tag", true) }
                TagType.TAG_INT_ARRAY -> { ExpandableValue("creates empty IntArray tag", true) }
                TagType.TAG_LONG_ARRAY -> { ExpandableValue("creates empty LongArray array", true) }
                TagType.TAG_LIST -> { ExpandableValue("creates empty List tag", true) }
                TagType.TAG_COMPOUND -> { ExpandableValue("creates empty Compound tag", true) }
                TagType.TAG_END -> throw Exception("Is this possible?")
            }
        }
    } else if (doodle is ValueCreationDoodle) {
        Index(intoIndex, true)
        Spacer(modifier = Modifier.width(10.dp))
        when (doodle.parent.tag.type) {
            TagType.TAG_BYTE_ARRAY -> { ByteField(valueState, valueValidState) }
            TagType.TAG_INT_ARRAY -> { IntField(valueState, valueValidState) }
            TagType.TAG_LONG_ARRAY -> { LongField(valueState, valueValidState) }
            else -> { /* no-op */ }
        }
    }
    Spacer(modifier = Modifier.weight(1f))
    ToolBarItemIndicator(false, { state.cancelCreation() }) {
        IndicatorText("CANCEL", ThemedColor.Editor.Action.Delete)
    }
    Spacer(modifier = Modifier.width(20.dp))
    ToolBarItemIndicator(!(nameValid && valueValid), { state.create(generateActual(), doodle.parent) }) {
        IndicatorText("OK", ThemedColor.Editor.Action.Create)
    }
    Spacer(modifier = Modifier.width(50.dp))
}

private val transformName: (AnnotatedString) -> Pair<Boolean, TransformedText> = { string ->
    val text = string.text
    val checker = Regex("[^a-zA-Z0-9_]")
    val invalids = checker.findAll(text).map {
        AnnotatedString.Range(SpanStyle(color = ThemedColor.Editor.Selector.Invalid), it.range.first, it.range.last + 1)
    }.toList()
    if (invalids.isNotEmpty()) {
        Pair(
            false,
            TransformedText(
                AnnotatedString(
                    string.text,
                    invalids
                ),
                OffsetMapping.Identity
            )
        )
    } else {
        if (text.isEmpty()) {
            Pair(false, TransformedText(AnnotatedString(string.text, listOf()), OffsetMapping.Identity))
        } else {
            Pair(
                true,
                TransformedText(AnnotatedString(string.text, listOf()), OffsetMapping.Identity)
            )
        }
    }
}

fun transformedText(text: String, valid: Boolean): TransformedText {
    return if (!valid) {
        TransformedText(AnnotatedString(
            text,
            listOf(AnnotatedString.Range(SpanStyle(color = ThemedColor.Editor.Selector.Invalid), 0, text.length))
        ), OffsetMapping.Identity)
    } else {
        TransformedText(AnnotatedString(text), OffsetMapping.Identity)
    }
}

@Composable
fun RowScope.TagField(
    textState: MutableState<String>,
    validState: MutableState<Boolean>,
    color: Color,
    hint: String,
    transformation: (AnnotatedString) -> Pair<Boolean, TransformedText>
) {
    val (text, setText) = textState
    val (_, setValid) = validState

    Box(
        modifier = Modifier
            .drawBehind(border(bottom = Pair(2f, ThemedColor.from(ThemedColor.Editor.Creation, alpha = 150))))
            .padding(5.dp)
            .widthIn(max = 250.dp)
    ) {
        BasicTextField(
            text,
            setText,
            singleLine = true,
            textStyle = TextStyle(fontFamily = JetBrainsMono, fontSize = 20.sp, color = color),
            cursorBrush = SolidColor(color),
            visualTransformation = {
                val (valid, transformedText) = transformation(it)
                setValid(valid)
                transformedText
            }
        )
        if (text.isEmpty()) {
            Text(
                hint,
                fontFamily = JetBrainsMono,
                color = Color.White,
                fontSize = 20.sp,
                modifier = Modifier.alpha(0.3f)
                    .align(Alignment.CenterStart)
            )
        }
    }
}

@Composable
fun RowScope.TagNameField(
    nameState: MutableState<String>,
    validState: MutableState<Boolean>
) {
    TagField(nameState, validState, ThemedColor.Editor.Tag.General, "TAG NAME", transformName)
}

@Composable
fun RowScope.ByteField(
    valueState: MutableState<String>,
    validState: MutableState<Boolean>
) {
    TagField(valueState, validState, ThemedColor.Editor.Tag.Number, "Byte (in [-128, 127])") {
        val valid = it.text.toByteOrNull() != null
        Pair(valid, transformedText(it.text, valid))
    }
}

@Composable
fun RowScope.ShortField(
    valueState: MutableState<String>,
    validState: MutableState<Boolean>
) {
    TagField(valueState, validState, ThemedColor.Editor.Tag.Number, "Short (in [-32768, 32767])") {
        val valid = it.text.toShortOrNull() != null
        Pair(valid, transformedText(it.text, valid))
    }
}

@Composable
fun RowScope.IntField(
    valueState: MutableState<String>,
    validState: MutableState<Boolean>
) {
    TagField(valueState, validState, ThemedColor.Editor.Tag.Number, "Integer") {
        val valid = it.text.toIntOrNull() != null
        Pair(valid, transformedText(it.text, valid))
    }
}

@Composable
fun RowScope.LongField(
    valueState: MutableState<String>,
    validState: MutableState<Boolean>
) {
    TagField(valueState, validState, ThemedColor.Editor.Tag.Number, "Long") {
        val valid = it.text.toLongOrNull() != null
        Pair(valid, transformedText(it.text, valid))
    }
}

@Composable
fun RowScope.FloatField(
    valueState: MutableState<String>,
    validState: MutableState<Boolean>
) {
    TagField(valueState, validState, ThemedColor.Editor.Tag.Number, "Float") {
        val valid = it.text.toFloatOrNull() != null
        Pair(valid, transformedText(it.text, valid))
    }
}

@Composable
fun RowScope.DoubleField(
    valueState: MutableState<String>,
    validState: MutableState<Boolean>
) {
    TagField(valueState, validState, ThemedColor.Editor.Tag.Number, "Double") {
        val valid = it.text.toFloatOrNull() != null
        Pair(valid, transformedText(it.text, valid))
    }
}

@Composable
fun RowScope.StringField(
    valueState: MutableState<String>,
    validState: MutableState<Boolean>
) {
    TagField(valueState, validState, ThemedColor.Editor.Tag.Number, "String") {
        Pair(true, transformedText(it.text, true))
    }
}

@Composable
fun RowScope.CreationField(
    nameState: MutableState<String>,
    validState: MutableState<Boolean>,
    content: @Composable RowScope.() -> Unit
) {
    TagNameField(nameState, validState)
    Spacer(modifier = Modifier.width(20.dp))
    content()
}

@Composable
fun RowScope.DoodleContent(doodle: ActualDoodle, selected: Boolean) {
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
    var parent = if (doodle is ActualDoodle) doodle.parent else if (doodle is VirtualDoodle) doodle.parent else null
    while (parent != null && parent.depth >= 0) {
        result.add(parent)
        parent = parent.parent
    }
    result.reverse()
    return result
}
