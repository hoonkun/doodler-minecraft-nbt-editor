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
import androidx.compose.ui.draw.rotate
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
import composables.states.editor.world.extensions.color
import composables.states.editor.world.extensions.creationHint
import composables.states.editor.world.extensions.shorten
import composables.states.editor.world.extensions.transformer
import doodler.nbt.AnyTag
import doodler.nbt.TagType

@Composable
private fun ItemRoot(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Box (modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }

}

@Composable
private fun TagTypeIndicatorWrapper(selected: Boolean, content: @Composable BoxScope.() -> Unit) {
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
private fun NbtActionButtonWrapper(disabled: Boolean, onClick: MouseClickScope.() -> Unit, content: @Composable BoxScope.() -> Unit) {
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
fun NbtText(text: String, color: Color, fontSize: TextUnit = 18.sp, rotate: Float = 0f, multiplier: Int = 0) {
    val offset = if (rotate == 0f) 0 else (5 * multiplier)
    Text(
        text,
        color = color,
        fontFamily = JetBrainsMono,
        fontSize = fontSize,
        modifier = Modifier
            .rotate(rotate)
            .absoluteOffset(x = offset.dp, y = 0.dp)
    )
}

@Composable
private fun NbtContentText(text: String, color: Color, fontSize: TextUnit = 20.sp) {
    Text(text, color = color, fontFamily = JetBrainsMono, fontSize = fontSize)
}

@Composable
private fun TagTypeIndicatorText(type: TagType, disabled: Boolean) {
    NbtText(type.shorten(), color = if (disabled) ThemedColor.Editor.Tag.General else type.color())
}

@Composable
private fun TagTypeIndicator(type: TagType, selected: Boolean) {
    TagTypeIndicatorWrapper (selected) {
        TagTypeIndicatorText(type, false)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TagCreationButton(holderTag: AnyTag?, type: TagType, actions: NbtState.Actions) {
    val disabled = holderTag?.canHold(type) != true
    NbtActionButtonWrapper (
        disabled = disabled,
        onClick = { actions.withLog { creator.prepare(type) } }
    ) {
        TagTypeIndicatorText(type, disabled)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NbtActionButton(
    disabled: Boolean,
    onClick: MouseClickScope.() -> Unit = { },
    content: @Composable () -> Unit
) {
    NbtActionButtonWrapper (disabled, onClick) {
        content()
    }
}

@Composable
private fun NumberValue(value: String) {
    NbtContentText(value, ThemedColor.Editor.Tag.Number)
}

@Composable
private fun StringValue(value: String) {
    NbtContentText(value, ThemedColor.Editor.Tag.String)
}

@Composable
private fun RowScope.ExpandableValue(value: String, selected: Boolean) {
    Box (
        modifier = Modifier
            .wrapContentSize()
            .background(ThemedColor.Editor.indicator(selected), RoundedCornerShape(5.dp))
            .padding(top = 2.dp, bottom = 2.dp, start = 10.dp, end = 10.dp)
    ) {
        NbtContentText(value, ThemedColor.Editor.indicatorText(selected), 18.sp)
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
        NbtContentText("$index:", ThemedColor.Editor.indicatorText(selected), 18.sp)
    }
}

@Composable
private fun RowScope.KeyValue(doodle: NbtDoodle, selected: Boolean) {
    val key = doodle.name
    if (key != null) {
        NbtContentText(key, ThemedColor.Editor.Tag.General)
        Spacer(modifier = Modifier.width(20.dp))
    }
    if ((doodle.parent?.tag?.type ?: TagType.TAG_COMPOUND) != TagType.TAG_COMPOUND && doodle.index >= 0) {
        Index(doodle.index, selected)
        Spacer(modifier = Modifier.width(10.dp))
    }
    if (doodle.tag.type.isNumber())
        NumberValue(doodle.value)
    else if (doodle.tag.type.isString())
        StringValue(doodle.value)
    else if (doodle.tag.type.canHaveChildren())
        ExpandableValue(doodle.value, selected)
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun DepthPreviewNbtItem(
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
                ActualNbtItemContent(doodle, false)
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun ActualNbtItem(
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
                    ActualNbtItemContent(doodle, selected)
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun VirtualNbtItem(virtual: VirtualDoodle, state: NbtState) {
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
                    DoodleCreationContent(state.actions, virtual)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RowScope.DoodleCreationContent(actions: NbtState.Actions, doodle: VirtualDoodle) {

    val intoIndex =
        if (!doodle.mode.isEdit()) doodle.parent.expandedItems.size.coerceAtLeast(doodle.parent.collapsedItems.size)
        else doodle.index

    val nameState = remember { mutableStateOf(
        if (doodle.mode.isEdit()) doodle.from.let { if (it is NbtDoodle) it.tag.name else null } ?: ""
        else ""
    ) }
    val valueState = remember { mutableStateOf(
        if (doodle.mode.isEdit()) doodle.from.let {
            if (it is NbtDoodle) it.value else if (it is ValueDoodle) it.value else null
        } ?: ""
        else ""
    ) }

    val nameValidState = remember { mutableStateOf(doodle.parent.tag.type.isList()) }
    val valueValidState = remember { mutableStateOf(doodle !is NbtCreationDoodle || doodle.type.canHaveChildren()) }

    val name by nameState
    val value by valueState

    val nameValid by nameValidState
    val valueValid by valueValidState

    val cancel: MouseClickScope.() -> Unit = {
        if (doodle.mode == VirtualDoodle.VirtualMode.CREATE) actions.withLog { creator.cancel() }
        else if (doodle.mode == VirtualDoodle.VirtualMode.EDIT) actions.withLog { editor.cancel() }
    }

    val ok: MouseClickScope.() -> Unit = {
        if (doodle.mode == VirtualDoodle.VirtualMode.CREATE)
            actions.withLog { creator.create(doodle.actualize(name, value, intoIndex), doodle.parent) }
        else if (doodle.mode == VirtualDoodle.VirtualMode.EDIT)
            actions.withLog { editor.edit(doodle.from, doodle.actualize(name, value, intoIndex)) }
    }

    if (doodle is NbtCreationDoodle) {
        TagTypeIndicator(doodle.type, true)
        Spacer(modifier = Modifier.width(20.dp))
        if (!doodle.parent.tag.type.isList()) {
            CreationField(nameState, nameValidState) {
                if (doodle.type.isNumber() || doodle.type.isString())
                    ValueField(valueState, valueValidState, doodle.type, true)
                else
                    ExpandableValue(if (doodle.mode.isEdit()) value else doodle.type.creationHint(), true)
            }
        } else {
            if (doodle.type.isNumber() || doodle.type.isString())
                ValueField(valueState, valueValidState, doodle.type, true)
            else
                ExpandableValue(if (doodle.mode.isEdit()) value else doodle.type.creationHint(), true)
        }
    } else if (doodle is ValueCreationDoodle) {
        Index(intoIndex, true)
        Spacer(modifier = Modifier.width(10.dp))
        ValueField(valueState, valueValidState, doodle.parent.tag.type.arrayElementType(), false)
    }
    Spacer(modifier = Modifier.weight(1f))
    NbtActionButtonWrapper(false, cancel) {
        NbtText("CANCEL", ThemedColor.Editor.Action.Delete)
    }
    Spacer(modifier = Modifier.width(20.dp))
    NbtActionButtonWrapper(!(nameValid && valueValid), ok) {
        NbtText("OK", ThemedColor.Editor.Action.Create)
    }
    Spacer(modifier = Modifier.width(50.dp))
}

private val nameTransformer: (AnnotatedString) -> Pair<Boolean, TransformedText> = { string ->
    val text = string.text
    val checker = Regex("[^a-zA-Z0-9_]")
    val invalids = checker.findAll(text).map {
        AnnotatedString.Range(SpanStyle(color = ThemedColor.Editor.Selector.Invalid), it.range.first, it.range.last + 1)
    }.toList()
    if (invalids.isNotEmpty()) {
        Pair(false, TransformedText(AnnotatedString(string.text, invalids), OffsetMapping.Identity))
    } else {
        if (text.isEmpty()) {
            Pair(false, TransformedText(AnnotatedString(string.text, listOf()), OffsetMapping.Identity))
        } else {
            Pair(true, TransformedText(AnnotatedString(string.text, listOf()), OffsetMapping.Identity))
        }
    }
}

@Composable
fun RowScope.TagField(
    textState: MutableState<String>,
    validState: MutableState<Boolean>,
    color: Color,
    hint: String,
    transformation: (AnnotatedString) -> Pair<Boolean, TransformedText>,
    wide: Boolean = true
) {
    val (text, setText) = textState
    val (_, setValid) = validState

    Box(
        modifier = Modifier
            .drawBehind(border(bottom = Pair(2f, ThemedColor.from(ThemedColor.Editor.Creation, alpha = 150))))
            .padding(5.dp)
            .widthIn(max = if (!wide) 250.dp else 450.dp)
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
fun RowScope.ValueField(
    textState: MutableState<String>,
    validState: MutableState<Boolean>,
    type: TagType,
    wide: Boolean = true,
) {
    TagField(textState, validState, type.color(), type.creationHint(), type.transformer(), wide)
}

@Composable
fun RowScope.CreationField(
    nameState: MutableState<String>,
    validState: MutableState<Boolean>,
    content: @Composable RowScope.() -> Unit
) {
    TagField(nameState, validState, ThemedColor.Editor.Tag.General, "Tag Name", nameTransformer, false)
    Spacer(modifier = Modifier.width(20.dp))
    content()
}

@Composable
fun RowScope.ActualNbtItemContent(doodle: ActualDoodle, selected: Boolean) {
    when (doodle) {
        is NbtDoodle -> {
            TagTypeIndicator(doodle.tag.type, selected)
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
