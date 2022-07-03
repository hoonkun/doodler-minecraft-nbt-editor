package composable.editor

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.zIndex
import doodler.doodle.extensions.*
import doodler.doodle.structures.*
import doodler.editor.states.NbtEditorState
import doodler.nbt.TagType
import doodler.theme.DoodlerTheme
import doodler.types.*
import doodler.unit.ddp
import doodler.unit.dsp

@Composable
fun DoodleItemRoot(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) = Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.then(modifier).padding(start = 7.65.ddp).height(25.5.ddp),
    content = content
)

@Composable
fun DoodleText(
    text: String,
    color: Color,
    fontSize: TextUnit = 9.5625.dsp,
    rotate: Pair<Float, Int>? = null
) = Text(
    text = text,
    color = color,
    fontSize = fontSize,
    modifier = Modifier.let { if (rotate != null) it.rotate(rotate.first) else it }
)

@Composable
fun DoodleText(
    text: AnnotatedString,
    fontSize: TextUnit = 9.5625.dsp
) = Text(
    text = text,
    fontSize = fontSize
)

val arrayTypeTextSpan = listOf(
    AnnotatedString.Range(SpanStyle(color = DoodlerTheme.Colors.Text.IdeFunctionProperty), 0, 1),
    AnnotatedString.Range(SpanStyle(color = DoodlerTheme.Colors.Text.IdeNumberLiteral), 1, 2),
    AnnotatedString.Range(SpanStyle(color = DoodlerTheme.Colors.Text.IdeFunctionProperty), 2, 3),
)

val disabledTypeTextSpan = listOf(
    AnnotatedString.Range(SpanStyle(color = DoodlerTheme.Colors.Text.IdeGeneral), 0, 3)
)

@Composable
fun TagDoodleTypeText(
    type: TagType,
    fontSize: TextUnit = 7.65.dsp,
    enabled: BooleanProvider
) = DoodleText(
    text =
        if (type.isArray()) {
            AnnotatedString(type.shorten(), if (enabled()) arrayTypeTextSpan else disabledTypeTextSpan)
        } else {
            val string = type.shorten()
            val color = if (enabled()) type.color() else DoodlerTheme.Colors.Text.IdeGeneral
            AnnotatedString(string, listOf(
                AnnotatedString.Range(SpanStyle(color = color), 0, string.length),
            ))
        },
    fontSize = fontSize
)

@Composable
fun TagDoodleContentText(
    text: String,
    color: Color,
    fontSize: TextUnit = 9.5625.dsp
) = DoodleText(text = text, color = color, fontSize = fontSize)

@Composable
fun TagDoodleType(
    type: TagType,
    fontSize: TextUnit = 7.65.dsp,
    selected: BooleanProvider = FalseProvider,
    backgroundColor: Color = DoodlerTheme.Colors.DoodleItem.TagTypeBackground(selected())
) = Box (
    contentAlignment = Alignment.Center,
    modifier = Modifier
        .wrapContentSize()
        .background(backgroundColor, RoundedCornerShape(1.9125.ddp))
        .padding(vertical = 1.275.ddp, horizontal = 1.9125.ddp),
    content = { TagDoodleTypeText(type = type, fontSize = fontSize, enabled = TrueProvider) }
)

@Composable
fun TagDoodleName(
    name: String
) = TagDoodleContentText(text = name, color = DoodlerTheme.Colors.Text.IdeGeneral)

@Composable
fun NumberTagDoodleValue(
    value: String
) = TagDoodleContentText(text = value, color = DoodlerTheme.Colors.Text.IdeNumberLiteral)

@Composable
fun StringTagDoodleValue(
    value: String
) = TagDoodleContentText(text = "\"${value}\"", color = DoodlerTheme.Colors.Text.IdeStringLiteral)

@Composable
fun ExpandableTagDoodleValue(
    value: String,
    selected: BooleanProvider = FalseProvider
) = Box(
    modifier = Modifier
        .wrapContentSize()
        .background(DoodlerTheme.Colors.DoodleItem.TagTypeBackground(selected()), RoundedCornerShape(1.9125.ddp))
        .padding(vertical = 1.275.ddp, horizontal = 3.1875.ddp),
    content = {
        TagDoodleContentText(
            text = value,
            color = DoodlerTheme.Colors.DoodleItem.ExpandableValueTextColor(selected()),
            fontSize = 7.65.dsp
        )
    }
)

@Composable
fun ExpandableTagItemDoodleIndex(
    index: Int,
    selected: BooleanProvider = FalseProvider,
    prefix: String = "",
    suffix: String = ":",
    backgroundColor: Color = DoodlerTheme.Colors.DoodleItem.TagTypeBackground(selected())
) = Box (
    modifier = Modifier
        .wrapContentSize()
        .background(
            color = backgroundColor,
            shape = RoundedCornerShape(1.9125.ddp)
        )
        .padding(vertical = 1.275.ddp, horizontal = 1.9125.ddp),
    content = {
        TagDoodleContentText(
            text = "$prefix$index$suffix",
            color = DoodlerTheme.Colors.DoodleItem.IndexTextColor(selected()),
            fontSize = 7.65.dsp
        )
    }
)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DepthLine(
    focused: BooleanProvider,
    selected: BooleanProvider = FalseProvider,
    focus: () -> Unit,
    unFocus: () -> Unit,
    collapse: () -> Unit
){
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    Canvas(
        modifier = Modifier.width(22.3125.ddp).fillMaxHeight()
            .onPointerEvent(PointerEventType.Enter) { focus() }
            .onPointerEvent(PointerEventType.Exit) { unFocus() }
            .hoverable(interaction)
            .clickable { collapse() },
    ) {
        drawLine(
            DoodlerTheme.Colors.DoodleItem.DepthLine(selected = selected(), hovered = hovered || focused()),
            start = Offset.Zero,
            end = Offset(0f, size.height)
        )
    }
}

@Composable
fun TagDoodleKeyValue(
    doodle: TagDoodle
) {
    val key = doodle.tag.name
    if (key != null) {
        Spacer(modifier = Modifier.width(12.75.ddp))
        TagDoodleName(name = key)
        Spacer(modifier = Modifier.width(12.75.ddp))
    }
    if (doodle.parent?.tag?.type?.isCompound() != true) {
        Spacer(modifier = Modifier.width(5.95.ddp))
        ExpandableTagItemDoodleIndex(index = doodle.index)
        Spacer(modifier = Modifier.width(12.75.ddp))
    }
    if (doodle.tag.type.isNumber()) NumberTagDoodleValue(doodle.value)
    else if (doodle.tag.type.isString()) StringTagDoodleValue(doodle.value)
    else if (doodle.tag.type.canHaveChildren()) ExpandableTagDoodleValue(doodle.value)
}

@Composable
fun TagDoodleContent(
    doodle: TagDoodle
) {
    TagDoodleType(doodle.tag.type)
    TagDoodleKeyValue(doodle)
}

@Composable
fun ArrayValueDoodleContent(
    index: Int,
    value: String
) {
    ExpandableTagItemDoodleIndex(index)
    Spacer(modifier = Modifier.width(12.75.ddp))
    NumberTagDoodleValue(value)
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun RowScope.ReadonlyDoodleContent(
    doodle: ReadonlyDoodle,
    hoverInteractionSource: MutableInteractionSource,
    pressedState: MutableState<Boolean>,
    expand: Boolean = true,
    focus: (ReadonlyDoodle?) -> Unit,
    onClick: MouseClickScope.() -> Unit,
) = Box(
    modifier = Modifier.fillMaxHeight()
        .onPointerEvent(PointerEventType.Enter) { focus(doodle) }
        .onPointerEvent(PointerEventType.Exit) { focus(null) }
        .onPointerEvent(PointerEventType.Press) { pressedState.value = true }
        .onPointerEvent(PointerEventType.Release) { pressedState.value = false }
        .mouseClickable(onClick = onClick)
        .hoverable(hoverInteractionSource)
        .let { if (expand) it.weight(1f) else it },
    content = {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxHeight(),
            content = {
                when (doodle) {
                    is TagDoodle -> TagDoodleContent(doodle)
                    is ArrayValueDoodle -> ArrayValueDoodleContent(doodle.index, doodle.value)
                }
                Spacer(modifier = Modifier.width(7.65.ddp))
            }
        )
    }
)

@Composable
fun ActionDoodleField(
    text: MutableState<String>,
    isValid: MutableState<Boolean>,
    hint: String,
    color: Color,
    wide: Boolean = true,
    focus: Boolean = false,
    transformation: (AnnotatedString) -> Pair<Boolean, TransformedText>,
    modifier: Modifier = Modifier
) {
    val requester = remember { FocusRequester() }

    SideEffect {
        if (focus) requester.requestFocus()
    }

    Box(
        modifier = Modifier
            .drawBehind {
                drawLine(
                    color = DoodlerTheme.Colors.DoodleItem.ActionTargetItemBackground.copy(alpha = 0.5882f),
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 2f
                )
            }
            .padding(1.9125.ddp)
            .widthIn(max = if (wide) 172.125.ddp else 95.625.ddp)
    ) {
        BasicTextField(
            value = text.value,
            onValueChange = { text.value = it },
            singleLine = true,
            textStyle = TextStyle(
                fontFamily = DoodlerTheme.Fonts.JetbrainsMono,
                fontSize = 9.5625.dsp,
                color = color
            ),
            cursorBrush = SolidColor(color),
            visualTransformation = {
                val (valid, transformedText) = transformation(it)
                isValid.value = valid
                transformedText
            },
            modifier = Modifier.then(modifier).focusable().focusRequester(requester)
        )
        if (text.value.isEmpty()) {
            Text(
                text = hint,
                color = Color.White.copy(alpha = 0.3f),
                fontSize = 9.5625.dsp,
                modifier = Modifier.align(Alignment.CenterStart)
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ActionDoodleNameField(
    name: MutableState<String>,
    isValid: MutableState<Boolean>,
    onEnter: () -> Unit,
    onEscape: () -> Unit
) {
    ActionDoodleField(
        text = name,
        isValid = isValid,
        hint = "Tag Name",
        color = DoodlerTheme.Colors.Text.IdeGeneral,
        wide = false,
        focus = true,
        transformation = NameTransformer(),
        modifier = Modifier.onPreviewKeyEvent {
            when (it.key) {
                Key.Enter -> onEnter()
                Key.Escape -> onEscape()
            }
            false
        }
    )
    Spacer(modifier = Modifier.width(7.65.ddp))
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ActionDoodleValueField(
    value: MutableState<String>,
    isValid: MutableState<Boolean>,
    type: TagType,
    focus: Boolean,
    onEnter: () -> Unit,
    onEscape: () -> Unit
) = ActionDoodleField(
    text = value,
    isValid = isValid,
    hint = type.creationHint(),
    color = type.color(),
    wide = true,
    focus = focus,
    transformation = type.transformer(),
    modifier = Modifier.onPreviewKeyEvent {
        when (it.key) {
            Key.Enter -> onEnter()
            Key.Escape -> onEscape()
        }
        false
    }
)

@Composable
fun RowScope.ActionDoodleContent(
    doodle: ActionDoodle,
    stateProvider: Provider<NbtEditorState>
) {
    val name = remember { mutableStateOf(if (doodle is TagEditorDoodle) doodle.source.tag.name ?: "" else "") }
    val value = remember { mutableStateOf(if (doodle is EditorDoodle) doodle.source.value else "") }

    val isValidName = remember { mutableStateOf(doodle.parent.tag.type.isUnnamedCollection()) }
    val isValidValue = remember { mutableStateOf(!(doodle is TagCreatorDoodle && doodle.type.canHaveChildren())) }

    val commit = {
        val state = stateProvider()
        when (doodle) {
            is TagCreatorDoodle -> state.action {
                creator.create(doodle.commitTag(doodle.type, name.value, value.value))
            }
            is TagEditorDoodle -> state.action {
                editor.edit(doodle.source, doodle.commitTag(doodle.source.tag.type, name.value, value.value))
            }
            is ArrayValueCreatorDoodle -> state.action { creator.create(doodle.commitValue(value.value)) }
            is ArrayValueEditorDoodle -> state.action { editor.edit(doodle.source, doodle.commitValue(value.value)) }
        }
    }

    val cancel = {
        val state = stateProvider()
        when (doodle) {
            is CreatorDoodle -> state.action { creator.cancel() }
            is EditorDoodle -> state.action { editor.cancel() }
        }
    }

    val enterCommit = {
        if (isValidName.value && isValidValue.value) commit()
    }


    if (doodle is TagCreatorDoodle || doodle is TagEditorDoodle) {
        val tagType = when (doodle) {
            is TagCreatorDoodle -> doodle.type
            is TagEditorDoodle -> doodle.source.tag.type
            else -> TagType.TAG_END // no-op
        }

        TagDoodleType(type = tagType, selected = TrueProvider)
        Spacer(modifier = Modifier.width(7.65.ddp))

        if (!doodle.parent.tag.type.isList())
            ActionDoodleNameField(name, isValidName, enterCommit, cancel)

        if (tagType.isNumber() || tagType.isString())
            ActionDoodleValueField(
                value = value,
                isValid = isValidValue,
                type = tagType,
                focus = doodle.parent.tag.type.isList(),
                onEnter = enterCommit,
                onEscape = cancel
            )
        else
            ExpandableTagDoodleValue(if (doodle is TagEditorDoodle) doodle.source.value else tagType.creationHint())

    } else if (doodle is ArrayValueCreatorDoodle) {
        ExpandableTagItemDoodleIndex(doodle.parent.children.size)
        Spacer(modifier = Modifier.width(3.825.ddp))
        ActionDoodleValueField(
            value = value,
            isValid = isValidValue,
            type = doodle.parent.tag.type.arrayElementType(),
            focus = true,
            onEnter = enterCommit,
            onEscape = cancel
        )
    }

    Spacer(modifier = Modifier.weight(1f))

    EditorActionButton("Cancel", DoodlerTheme.Colors.DoodleAction.CancelAction) { cancel() }

    Spacer(modifier = Modifier.width(7.65.ddp))

    EditorActionButton(
        text = "Ok",
        color = DoodlerTheme.Colors.DoodleAction.OkAction,
        enabled = { isValidName.value && isValidValue.value },
        onClick = { commit() }
    )

    Spacer(modifier = Modifier.width(19.125.ddp))
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReadonlyDoodle(
    doodle: ReadonlyDoodle,
    toggle: (ReadonlyDoodle) -> Unit,
    select: (ReadonlyDoodle) -> Unit,
    collapse: (TagDoodle) -> Unit,
    stateProvider: Provider<NbtEditorState>,
    selected: BooleanProvider = FalseProvider,
    actionTarget: BooleanProvider = FalseProvider,
    enabled: BooleanProvider = TrueProvider
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    val pressedState = remember { mutableStateOf(false) }

    val hierarchy = remember(doodle) { doodle.hierarchy() }

    DoodleItemRoot(
        modifier = Modifier.fillMaxWidth()
            .drawWithContent {
                drawRect(DoodlerTheme.Colors.DoodleItem.NormalItemBackground)
                if (!enabled()) {
                    drawContent()
                    drawRect(DoodlerTheme.Colors.DoodleItem.NormalItemBackground.copy(alpha = 0.68627f))
                } else {
                    drawRect(DoodlerTheme.Colors.DoodleItem.Background(
                        hovered || stateProvider().focused == doodle,
                        pressedState.value,
                        selected(),
                        actionTarget()
                    ))
                    drawContent()
                }
            }
    ) {
        for (parent in hierarchy) {
            val focus = { stateProvider().focused = parent }
            val unFocus = { stateProvider().focused = null }
            DepthLine(
                focused = { stateProvider().focused == parent },
                selected = selected,
                focus = focus,
                unFocus = unFocus,
                collapse = { collapse(parent) }
            )
        }
        ReadonlyDoodleContent(
            doodle = doodle,
            hoverInteractionSource = interaction,
            pressedState = pressedState,
            focus = { stateProvider().focused = it },
            onClick = {
                if (!enabled()) return@ReadonlyDoodleContent
                if (buttons.isPrimaryPressed) toggle(doodle)
                else if (buttons.isSecondaryPressed) select(doodle)
            }
        )
    }
}

@Composable
fun ActionDoodle(
    doodle: ActionDoodle,
    stateProvider: Provider<NbtEditorState>
) {
    val hierarchy = remember(doodle) { doodle.hierarchy() }

    DoodleItemRoot(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawRect(DoodlerTheme.Colors.DoodleItem.NormalItemBackground)
                drawRect(
                    DoodlerTheme.Colors.DoodleItem.Background(
                        hovered = false, pressed = false, selected = true, highlightAsActionTarget = true
                    )
                )
            }
    ) {
        for (parent in hierarchy) {
            DepthLine(FalseProvider, TrueProvider, EmptyLambda, EmptyLambda, EmptyLambda)
        }
        ActionDoodleContent(
            doodle = doodle,
            stateProvider = stateProvider
        )
    }
}

@Composable
fun TagDoodleDepthPreview(
    doodleProvider: Provider<Pair<ReadonlyDoodle, Int>?>,
    focus: (ReadonlyDoodle?) -> Unit,
    lazyStateProvider: Provider<LazyListState>,
    scrollTo: (ReadonlyDoodle) -> Unit
) {
    val (doodle, index) = doodleProvider() ?: return
    val lazyState = lazyStateProvider()

    if (lazyState.firstVisibleItemIndex <= index) return

    TagDoodlePreview(
        doodleProvider = { doodle },
        focus = focus,
        scrollTo = scrollTo
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TagDoodlePreview(
    doodleProvider: Provider<ReadonlyDoodle>,
    focus: (ReadonlyDoodle?) -> Unit = { },
    scrollTo: (ReadonlyDoodle) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    val pressedState = remember { mutableStateOf(false) }

    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    DoodleItemRoot(
        modifier = Modifier.then(modifier)
            .zIndex(99f)
            .border(12.75.ddp, DoodlerTheme.Colors.DoodleItem.DepthPreviewBorder)
            .drawWithContent {
                drawRect(DoodlerTheme.Colors.DoodleItem.NormalItemBackground)
                drawRect(
                    DoodlerTheme.Colors.DoodleItem.Background(
                        hovered = hovered,
                        pressed = pressedState.value,
                        selected = false,
                        highlightAsActionTarget = false
                    )
                )
                drawContent()
            }
    ) {
        ReadonlyDoodleContent(
            doodle = doodleProvider(),
            hoverInteractionSource = interaction,
            pressedState = pressedState,
            expand = false,
            focus = focus,
            onClick = {
                scrollTo(doodleProvider())
            }
        )
    }
}
