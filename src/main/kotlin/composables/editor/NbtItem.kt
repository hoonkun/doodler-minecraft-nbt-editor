package composables.editor

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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
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
import composables.global.JetBrainsMono
import composables.global.ThemedColor
import composables.global.border
import doodler.doodle.extensions.color
import doodler.doodle.extensions.creationHint
import doodler.doodle.extensions.shorten
import doodler.doodle.extensions.transformer
import doodler.doodle.*
import doodler.editor.states.NbtState
import doodler.logger.DoodlerLogger
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
private fun TagTypeIndicatorWrapper(selected: () -> Boolean, content: @Composable BoxScope.() -> Unit) {
    Box (
        modifier = Modifier
            .wrapContentSize()
            .background(ThemedColor.Editor.indicator(selected()), RoundedCornerShape(5.dp))
            .padding(top = 2.dp, bottom = 2.dp, start = 3.dp, end = 3.dp),
        content = content
    )
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
private fun NbtActionButtonWrapper(
    disabled: () -> Boolean,
    onClick: MouseClickScope.() -> Unit,
    onRightClick: MouseClickScope.() -> Unit = { },
    content: @Composable BoxScope.() -> Unit
) {
    var hover by remember { mutableStateOf(false) }
    if (disabled()) hover = false

    Box (
        modifier = Modifier
            .wrapContentSize()
            .onPointerEvent(PointerEventType.Enter) { if (!disabled()) hover = true }
            .onPointerEvent(PointerEventType.Exit) { if (!disabled()) hover = false }
            .mouseClickable {
                if (buttons.isPrimaryPressed && !disabled()) onClick()
                else if (buttons.isSecondaryPressed) onRightClick()
            }
            .padding(top = 5.dp, bottom = 5.dp)
            .background(
                if (hover) ThemedColor.from(Color.Black, alpha = 30)
                else Color.Transparent, RoundedCornerShape(3.dp)
            )
            .alpha(if (disabled()) 0.3f else 1f)
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
    DoodlerLogger.recomposition("NbtText: String")

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
fun NbtText(text: AnnotatedString, fontSize: TextUnit = 18.sp, rotate: Float = 0f, multiplier: Int = 0) {
    DoodlerLogger.recomposition("NbtText: AnnotatedString")

    val offset = if (rotate == 0f) 0 else (5 * multiplier)
    Text(
        text,
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
private fun TagTypeIndicatorText(type: TagType, disabled: Boolean, fontSize: TextUnit = 18.sp) {
    TagTypeIndicatorText(type, { disabled }, fontSize)
}

@Composable
private fun TagTypeIndicatorText(type: TagType, disabled: () -> Boolean, fontSize: TextUnit = 18.sp) {
    val text = if (type.isArray()) {
        AnnotatedString(type.shorten(), listOf(
            AnnotatedString.Range(SpanStyle(color = ThemedColor.Editor.Tag.NumberArray), 0, 1),
            AnnotatedString.Range(SpanStyle(color = ThemedColor.Editor.Tag.Number), 1, 2),
            AnnotatedString.Range(SpanStyle(color = ThemedColor.Editor.Tag.NumberArray), 2, 3),
        ))
    } else {
        val string = type.shorten()
        val color = if (disabled()) ThemedColor.Editor.Tag.General else type.color()
        AnnotatedString(string, listOf(
            AnnotatedString.Range(SpanStyle(color = color), 0, string.length),
        ))
    }
    NbtText(text, fontSize = fontSize)
}

@Composable
private fun TagTypeIndicator(type: TagType, selected: () -> Boolean) {
    TagTypeIndicatorWrapper (selected) {
        TagTypeIndicatorText(type, false)
    }
}

@Composable
private fun TagTypeIndicator(type: TagType) {
    TagTypeIndicator(type) { true }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TagCreationButton(type: TagType, actionsProvider: () -> NbtState.Actions, disabled: () -> Boolean) {
    DoodlerLogger.recomposition("TagCreationButton")

    NbtActionButtonWrapper (
        disabled = disabled,
        onClick = { actionsProvider().withLog { creator.prepare(type) } }
    ) {
        TagTypeIndicatorText(type, disabled, 16.sp)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NbtActionButton(
    disabled: Boolean,
    onClick: MouseClickScope.() -> Unit = { },
    onRightClick: MouseClickScope.() -> Unit = { },
    content: @Composable () -> Unit
) {
    DoodlerLogger.recomposition("NbtActionButton")

    NbtActionButtonWrapper ({ disabled }, onClick, onRightClick) {
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
private fun RowScope.ExpandableValue(value: String, selected: () -> Boolean) {
    Box (
        modifier = Modifier
            .wrapContentSize()
            .background(ThemedColor.Editor.indicator(selected()), RoundedCornerShape(5.dp))
            .padding(top = 2.dp, bottom = 2.dp, start = 10.dp, end = 10.dp)
    ) {
        NbtContentText(value, ThemedColor.Editor.indicatorText(selected()), 18.sp)
    }
}

@Composable
private fun RowScope.ExpandableValue(value: String, selected: Boolean) {
    ExpandableValue(value) { selected }
}

@Composable
private fun Index(index: Int, selected: () -> Boolean) {
    Box (
        modifier = Modifier
            .wrapContentSize()
            .background(ThemedColor.Editor.indicator(selected()), shape = RoundedCornerShape(5.dp))
            .padding(top = 2.dp, bottom = 2.dp, start = 5.dp, end = 5.dp)
    ) {
        NbtContentText("$index:", ThemedColor.Editor.indicatorText(selected()), 18.sp)
    }
}

@Composable
private fun Index(index: Int) {
    Index(index) { true }
}

@Composable
private fun RowScope.KeyValue(doodle: NbtDoodle, selected: () -> Boolean) {
    val key = doodle.tag.name
    if (key != null) {
        NbtContentText(key, ThemedColor.Editor.Tag.General)
        Spacer(modifier = Modifier.width(20.dp))
    }
    if ((doodle.parent?.tag?.type ?: TagType.TAG_COMPOUND) != TagType.TAG_COMPOUND && doodle.index() >= 0) {
        Index(doodle.index(), selected)
        Spacer(modifier = Modifier.width(10.dp))
    }
    if (doodle.tag.type.isNumber())
        NumberValue(doodle.value())
    else if (doodle.tag.type.isString())
        StringValue(doodle.value())
    else if (doodle.tag.type.canHaveChildren())
        ExpandableValue(doodle.value(), selected)
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun DepthPreviewNbtItem(
    indexProvider: (Doodle) -> Int,
    stateProvider: () -> DoodleUi,
    scrollTo: (Int) -> Unit
) {
    DoodlerLogger.recomposition("DepthPreviewNbtItem")

    var focused by remember { mutableStateOf(false) }
    var pressed by remember { mutableStateOf(false) }

    val state = stateProvider()
    val doodle = (if (state.focusedTree == null) state.focusedTreeView else state.focusedTree) ?: return

    val itemState = state.toItemUi(doodle)

    val scroll = {
        val index = indexProvider(doodle)
        scrollTo(index)
        state.treeViewBlur(doodle)
        state.treeBlur(doodle)
        state.directFocus(doodle)
    }

    Box (modifier = Modifier
        .background(ThemedColor.EditorArea)
        .wrapContentSize()
        .zIndex(999f)
    ) {
        Box(modifier = Modifier
            .border(2.dp, ThemedColor.Editor.TreeBorder)
            .onPointerEvent(PointerEventType.Enter) { focused = true; itemState.functions.treeViewFocus(doodle) }
            .onPointerEvent(PointerEventType.Exit) { focused = false; itemState.functions.treeViewBlur(doodle) }
            .onPointerEvent(PointerEventType.Press) { pressed = true }
            .onPointerEvent(PointerEventType.Release) { pressed = false }
            .mouseClickable(onClick = { scroll() })
            .background(ThemedColor.Editor.normalItem(pressed, focused))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(start = (20 + 40 * doodle.depth).dp)
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                ActualNbtItemContent(doodle)
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun ActualNbtItem(
    doodle: ActualDoodle,
    stateProvider: () -> DoodleItemUi,
    toggle: (ActualDoodle) -> Unit,
    select: (ActualDoodle) -> Unit,
    treeCollapse: (NbtDoodle) -> Unit,
    onCreationMode: () -> Boolean = { false },
    disabled: () -> Boolean = { false }
) {
    DoodlerLogger.recomposition("ActualNbtItem")

    ItemRoot(
        modifier = Modifier
            .drawWithContent {
                val creationMode = onCreationMode()
                val state = stateProvider()
                if (creationMode && disabled()) {
                    drawContent()
                    drawRect(ThemedColor.from(ThemedColor.EditorArea, alpha = 175))
                } else {
                    drawRect(
                        ThemedColor.Editor.item(
                            selected = state.selected,
                            pressed = state.pressed,
                            focused = state.focusedDirectly || state.focusedTree,
                            onCreationMode = true,
                            alphaMultiplier = 0.6f
                        )
                    )
                    drawContent()
                }
            }
    ) {
        Row (
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 20.dp).height(50.dp)
        ) {
            for (i in 0 until doodle.depth) {
                val parent = { stateProvider().hierarchy.parents[i] }
                val current = { stateProvider().hierarchy.parentUiStates[i] }
                Box (modifier = Modifier
                    .fillMaxHeight()
                    .wrapContentWidth()
                    .onPointerEvent(PointerEventType.Enter) { if (!disabled()) stateProvider().functions.treeFocus(parent()) }
                    .onPointerEvent(PointerEventType.Exit) { if (!disabled()) stateProvider().functions.treeBlur(parent()) }
                    .onPointerEvent(PointerEventType.Release) { if (!disabled()) treeCollapse(parent()) }
                ) {
                    Spacer(modifier = Modifier.width(40.dp))
                    Box (
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .drawBehind {
                                current().let {
                                    drawRect(
                                        ThemedColor.Editor.depthLine(
                                            stateProvider().selected,
                                            it.focusedDirectly || it.focusedTree || it.focusedTreeView
                                        )
                                    )
                                }
                            }
                    ) { }
                }
            }
            Box (
                modifier = Modifier.weight(1f)
                    .onPointerEvent(PointerEventType.Enter) {
                        if (disabled()) return@onPointerEvent
                        stateProvider().functions.directFocus(doodle)
                        stateProvider().functions.treeBlur(null)
                    }
                    .onPointerEvent(PointerEventType.Exit) { if (!disabled()) stateProvider().functions.directBlur(doodle) }
                    .onPointerEvent(PointerEventType.Press) { if (!disabled()) stateProvider().functions.press(doodle) }
                    .onPointerEvent(PointerEventType.Release) { if (!disabled()) stateProvider().functions.release(doodle) }
                    .mouseClickable(onClick = {
                        if (disabled()) return@mouseClickable
                        if (buttons.isPrimaryPressed) toggle(doodle)
                        else if (buttons.isSecondaryPressed) select(doodle)
                    })
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    ActualNbtItemContent(doodle) { stateProvider().selected }
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun VirtualNbtItem(virtual: VirtualDoodle, state: DoodleItemUi, actions: NbtState.Actions) {
    DoodlerLogger.recomposition("VirtualNbtItem")

    ItemRoot(
        modifier = Modifier
            .background(
                ThemedColor.Editor.item(
                selected = true,
                pressed = false,
                focused = false,
                onCreationMode = true
            )
            )
    ) {
        Row (
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 20.dp).height(50.dp)
        ) {
            for (i in 0 until virtual.depth) {
                val parent = state.hierarchy.parents[i]
                val current = state.hierarchy.parentUiStates[i]
                val focused = current.focusedDirectly || current.focusedTree
                Box (modifier = Modifier
                    .fillMaxHeight()
                    .wrapContentWidth()
                    .onPointerEvent(PointerEventType.Enter) { state.functions.treeFocus(parent) }
                    .onPointerEvent(PointerEventType.Exit) { state.functions.treeBlur(parent) }
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
                    DoodleCreationContent(actions, virtual)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RowScope.DoodleCreationContent(actions: NbtState.Actions, doodle: VirtualDoodle) {
    DoodlerLogger.recomposition("DoodleCreationContent")

    val nameState = remember {
        mutableStateOf(
            if (doodle is EditionDoodle) doodle.from.let { if (it is NbtDoodle) it.tag.name else null } ?: ""
            else ""
        )
    }
    val valueState = remember {
        mutableStateOf(
            if (doodle is EditionDoodle) doodle.from.let {
                if (it is NbtDoodle) it.value() else if (it is ValueDoodle) it.value else null
            } ?: ""
            else ""
        )
    }

    val nameValidState = remember { mutableStateOf(doodle.parent.tag.type.isList()) }
    val valueValidState = remember { mutableStateOf(doodle !is NbtCreationDoodle || doodle.type.canHaveChildren()) }

    val name by nameState
    val value by valueState

    val nameValid by nameValidState
    val valueValid by valueValidState

    val cancel: MouseClickScope.() -> Unit = {
        if (doodle is CreationDoodle) actions.withLog { creator.cancel() }
        else if (doodle is EditionDoodle) actions.withLog { editor.cancel() }
    }

    val ok: MouseClickScope.() -> Unit = {
        if (doodle is CreationDoodle)
            actions.withLog { creator.create(doodle.actualize(name, value), doodle.parent, doodle.parent.children.size) }
        else if (doodle is EditionDoodle)
            actions.withLog { editor.edit(doodle.from, doodle.actualize(name, value)) }
    }

    if (doodle is NbtCreationDoodle || doodle is NbtEditionDoodle) {
        val type = if (doodle is NbtCreationDoodle) doodle.type else (doodle as NbtEditionDoodle).from.tag.type
        TagTypeIndicator(type)
        Spacer(modifier = Modifier.width(20.dp))
        if (!doodle.parent.tag.type.isList()) {
            CreationField(nameState, nameValidState) {
                if (type.isNumber() || type.isString())
                    ValueField(valueState, valueValidState, type, true)
                else
                    ExpandableValue(if (doodle is EditionDoodle) value else type.creationHint(), true)
            }
        } else {
            if (type.isNumber() || type.isString())
                ValueField(valueState, valueValidState, type, wide = true, focus = true)
            else
                ExpandableValue(if (doodle is EditionDoodle) value else type.creationHint(), true)
        }
    } else if (doodle is ValueCreationDoodle) {
        Index(doodle.parent.children.size)
        Spacer(modifier = Modifier.width(10.dp))
        ValueField(valueState, valueValidState, doodle.parent.tag.type.arrayElementType(), wide = false, focus = true)
    }
    Spacer(modifier = Modifier.weight(1f))
    NbtActionButtonWrapper({ false }, cancel) {
        NbtText("CANCEL", ThemedColor.Editor.Action.Delete)
    }
    Spacer(modifier = Modifier.width(20.dp))
    NbtActionButtonWrapper({ !(nameValid && valueValid) }, ok) {
        NbtText("OK", ThemedColor.Editor.Action.Create)
    }
    Spacer(modifier = Modifier.width(50.dp))
}

private val nameTransformer: (AnnotatedString) -> Pair<Boolean, TransformedText> = { string ->
    val text = string.text
    val checker = Regex("\\W")
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
    wide: Boolean = true,
    focus: Boolean = false
) {
    DoodlerLogger.recomposition("TagField")

    val (text, setText) = textState
    val (_, setValid) = validState

    val requester = remember { FocusRequester() }

    SideEffect {
        if (focus) requester.requestFocus()
    }

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
            },
            modifier = Modifier.let { if (focus) it.focusTarget().focusRequester(requester) else it }
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
    focus: Boolean = false
) {
    DoodlerLogger.recomposition("ValueField")

    TagField(textState, validState, type.color(), type.creationHint(), type.transformer(), wide, focus)
}

@Composable
fun RowScope.CreationField(
    nameState: MutableState<String>,
    validState: MutableState<Boolean>,
    content: @Composable RowScope.() -> Unit
) {
    DoodlerLogger.recomposition("CreationField")

    TagField(
        nameState,
        validState,
        ThemedColor.Editor.Tag.General,
        "Tag Name",
        nameTransformer,
        wide = false,
        focus = true
    )
    Spacer(modifier = Modifier.width(20.dp))
    content()
}

@Composable
fun RowScope.ActualNbtItemContent(doodle: ActualDoodle, selected: () -> Boolean) {
    DoodlerLogger.recomposition("ActualNbtItemContent")

    when (doodle) {
        is NbtDoodle -> {
            TagTypeIndicator(doodle.tag.type, selected)
            Spacer(modifier = Modifier.width(20.dp))
            KeyValue(doodle, selected)
        }
        is ValueDoodle -> {
            Index(doodle.index(), selected)
            Spacer(modifier = Modifier.width(10.dp))
            NumberValue(doodle.value)
        }
    }
}

@Composable
fun RowScope.ActualNbtItemContent(doodle: ActualDoodle) {
    ActualNbtItemContent(doodle) { false }
}
