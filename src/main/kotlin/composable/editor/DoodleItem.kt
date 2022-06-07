package composable.editor

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.unit.TextUnit
import doodler.doodle.extensions.*
import doodler.doodle.structures.*
import doodler.editor.states.NbtEditorState
import doodler.nbt.TagType
import doodler.theme.DoodlerTheme
import doodler.types.*
import doodler.unit.dp


@Composable
fun DoodleItemRoot(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) = Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.then(modifier).fillMaxWidth().padding(start = 20.dp).height(50.dp),
    content = content
)

@Composable
fun DoodleText(
    text: String,
    color: Color,
    fontSize: TextUnit = MaterialTheme.typography.h3.fontSize,
    rotate: Pair<Float, Int>? = null
) = Text(
    text = text,
    color = color,
    fontSize = fontSize,
    modifier = Modifier.also { if (rotate != null) it.rotate(rotate.first).absoluteOffset(x = rotate.second.dp) }
)

@Composable
fun DoodleText(
    text: AnnotatedString,
    fontSize: TextUnit = MaterialTheme.typography.h3.fontSize,
    rotate: Pair<Float, Int>? = null
) = Text(
    text = text,
    fontSize = fontSize,
    modifier = Modifier.also { if (rotate != null) it.rotate(rotate.first).absoluteOffset(x = rotate.second.dp) }
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
    fontSize: TextUnit = MaterialTheme.typography.h3.fontSize,
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
    fontSize: TextUnit = MaterialTheme.typography.h2.fontSize
) = DoodleText(text = text, color = color, fontSize = fontSize)

@Composable
fun TagDoodleType(
    type: TagType,
    fontSize: TextUnit = MaterialTheme.typography.h3.fontSize,
    selected: BooleanProvider = FalseProvider
) = Box (
    modifier = Modifier
        .wrapContentSize()
        .background(DoodlerTheme.Colors.DoodleItem.TagTypeBackground(selected()), RoundedCornerShape(5.dp))
        .padding(top = 2.dp, bottom = 2.dp, start = 3.dp, end = 3.dp),
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
) = TagDoodleContentText(text = value, color = DoodlerTheme.Colors.Text.IdeStringLiteral)

@Composable
fun ExpandableTagDoodleValue(
    value: String,
    selected: BooleanProvider = FalseProvider
) = Box(
    modifier = Modifier
        .wrapContentSize()
        .background(DoodlerTheme.Colors.DoodleItem.TagTypeBackground(selected()), RoundedCornerShape(5.dp))
        .padding(top = 2.dp, bottom = 2.dp, start = 10.dp, end = 10.dp),
    content = {
        TagDoodleContentText(
            text = value,
            color = DoodlerTheme.Colors.DoodleItem.ExpandableValueTextColor(selected()),
            fontSize = MaterialTheme.typography.h3.fontSize
        )
    }
)

@Composable
fun ExpandableTagItemDoodleIndex(
    index: Int,
    selected: BooleanProvider = FalseProvider
) = Box (
    modifier = Modifier
        .wrapContentSize()
        .background(DoodlerTheme.Colors.DoodleItem.TagTypeBackground(selected()), shape = RoundedCornerShape(5.dp))
        .padding(top = 2.dp, bottom = 2.dp, start = 5.dp, end = 5.dp),
    content = {
        TagDoodleContentText(
            text = "$index:",
            color = DoodlerTheme.Colors.DoodleItem.TagTypeBackground(selected()),
            fontSize = MaterialTheme.typography.h3.fontSize
        )
    }
)

@Composable
fun DepthLine(
    selected: BooleanProvider = FalseProvider,
    collapse: () -> Unit
){
    val hoverInteractionSource = remember { MutableInteractionSource() }
    val hovered by hoverInteractionSource.collectIsHoveredAsState()

    Canvas(
        modifier = Modifier.width(40.dp).fillMaxHeight()
            .clickable { collapse() },
    ) {
        drawLine(
            DoodlerTheme.Colors.DoodleItem.DepthLine(selected = selected(), hovered = hovered),
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
        TagDoodleName(name = key)
        Spacer(modifier = Modifier.width(20.dp))
    }
    if (doodle.parent?.tag?.type?.isCompound() != true) {
        ExpandableTagItemDoodleIndex(index = doodle.index)
        Spacer(modifier = Modifier.width(10.dp))
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
    Spacer(modifier = Modifier.width(20.dp))
    TagDoodleKeyValue(doodle)
}

@Composable
fun ArrayValueDoodleContent(
    index: Int,
    value: String
) {
    ExpandableTagItemDoodleIndex(index)
    NumberTagDoodleValue(value)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RowScope.ReadonlyDoodleContent(
    doodle: ReadonlyDoodle,
    hoverInteractionSource: MutableInteractionSource,
    pressInteractionSource: MutableInteractionSource,
    onClick: MouseClickScope.() -> Unit,
) = Box(
    modifier = Modifier.weight(1f).fillMaxHeight()
        .clickable(pressInteractionSource, null, onClick = EmptyLambda)
        .mouseClickable(onClick = onClick)
        .hoverable(hoverInteractionSource),
    content = {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxHeight(),
            content = {
                when (doodle) {
                    is TagDoodle -> TagDoodleContent(doodle)
                    is ArrayValueDoodle -> ArrayValueDoodleContent(doodle.index, doodle.value)
                }
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
    transformation: (AnnotatedString) -> Pair<Boolean, TransformedText>
) {
    val requester = remember { FocusRequester() }

    SideEffect {
        if (focus) requester.requestFocus()
    }

    Box(
        modifier = Modifier
            .drawBehind {
                drawLine(
                    color = DoodlerTheme.Colors.DoodleItem.SelectedItemBackground.copy(alpha = 0.5882f),
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 2f
                )
            }
            .padding(5.dp)
            .widthIn(max = if (wide) 450.dp else 250.dp)
    ) {
        BasicTextField(
            value = text.value,
            onValueChange = { text.value = it },
            singleLine = true,
            textStyle = TextStyle(
                fontFamily = DoodlerTheme.Fonts.JetbrainsMono,
                fontSize = MaterialTheme.typography.h2.fontSize,
                color = color
            ),
            cursorBrush = SolidColor(color),
            visualTransformation = {
                val (valid, transformedText) = transformation(it)
                isValid.value = valid
                transformedText
            },
            modifier = Modifier.focusRequester(requester).focusTarget()
        )
        if (text.value.isEmpty()) {
            Text(
                text = hint,
                color = Color.White.copy(alpha = 0.3f),
                fontSize = MaterialTheme.typography.h2.fontSize,
                modifier = Modifier.align(Alignment.CenterStart)
            )
        }
    }
}

@Composable
fun ActionDoodleNameField(
    name: MutableState<String>,
    isValid: MutableState<Boolean>
) {
    ActionDoodleField(
        text = name,
        isValid = isValid,
        hint = "Tag Name",
        color = DoodlerTheme.Colors.Text.IdeGeneral,
        wide = false,
        focus = true,
        transformation = NameTransformer()
    )
    Spacer(modifier = Modifier.width(20.dp))
}

@Composable
fun ActionDoodleValueField(
    value: MutableState<String>,
    isValid: MutableState<Boolean>,
    type: TagType,
    focus: Boolean
) {
    ActionDoodleField(
        text = value,
        isValid = isValid,
        hint = type.creationHint(),
        color = type.color(),
        wide = true,
        focus = focus,
        transformation = type.transformer()
    )
}

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


    if (doodle is TagCreatorDoodle || doodle is TagEditorDoodle) {
        val tagType = when (doodle) {
            is TagCreatorDoodle -> doodle.type
            is TagEditorDoodle -> doodle.source.tag.type
            else -> TagType.TAG_END // no-op
        }

        TagDoodleType(type = tagType, selected = TrueProvider)
        Spacer(modifier = Modifier.width(20.dp))

        if (!doodle.parent.tag.type.isList())
            ActionDoodleNameField(name, isValidName)

        if (tagType.isNumber() || tagType.isString())
            ActionDoodleValueField(value, isValidValue, tagType, focus = doodle.parent.tag.type.isList())
        else
            ExpandableTagDoodleValue(if (doodle is TagEditorDoodle) doodle.source.value else tagType.creationHint())

    } else if (doodle is ArrayValueCreatorDoodle) {
        ExpandableTagItemDoodleIndex(doodle.parent.children.size)
        Spacer(modifier = Modifier.width(10.dp))
        ActionDoodleValueField(value, isValidValue, doodle.parent.tag.type.arrayElementType(), focus = true)
    }

    Spacer(modifier = Modifier.weight(1f))

    EditorActionButton("Cancel", DoodlerTheme.Colors.DoodleAction.CancelAction) { cancel() }

    Spacer(modifier = Modifier.width(20.dp))

    EditorActionButton("Ok", DoodlerTheme.Colors.DoodleAction.OkAction) { commit() }

    Spacer(modifier = Modifier.width(50.dp))
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReadonlyDoodle(
    doodle: ReadonlyDoodle,
    toggle: (ReadonlyDoodle) -> Unit,
    select: (ReadonlyDoodle) -> Unit,
    collapse: (TagDoodle) -> Unit,
    selected: BooleanProvider = FalseProvider,
    actionTarget: BooleanProvider = FalseProvider,
    enabled: BooleanProvider = TrueProvider
) {
    val hoverInteractionSource = remember { MutableInteractionSource() }
    val hovered by hoverInteractionSource.collectIsHoveredAsState()

    val pressInteractionSource = remember { MutableInteractionSource() }
    val pressed by pressInteractionSource.collectIsPressedAsState()

    val hierarchy = remember(doodle) { doodle.hierarchy() }

    DoodleItemRoot(
        modifier = Modifier.drawWithContent {
            if (!enabled()) {
                drawContent()
                drawRect(DoodlerTheme.Colors.DoodleItem.NormalItemBackground.copy(alpha = 0.68627f))
            } else {
                drawRect(DoodlerTheme.Colors.DoodleItem.Background(hovered, pressed, selected(), actionTarget()))
                drawContent()
            }
        }
    ) {
        for (parent in hierarchy) {
            DepthLine(selected) { collapse(parent) }
        }
        ReadonlyDoodleContent(
            doodle = doodle,
            hoverInteractionSource = hoverInteractionSource,
            pressInteractionSource = pressInteractionSource,
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
        modifier = Modifier.drawBehind {
            drawRect(
                DoodlerTheme.Colors.DoodleItem.Background(
                    hovered = false, pressed = false, selected = true, highlightAsActionTarget = true
                )
            )
        }
    ) {
        for (parent in hierarchy) {
            DepthLine(TrueProvider, EmptyLambda)
        }
        ActionDoodleContent(
            doodle = doodle,
            stateProvider = stateProvider
        )
    }
}
