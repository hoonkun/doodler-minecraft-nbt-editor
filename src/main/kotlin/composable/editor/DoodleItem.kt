package composable.editor

import activator.composables.global.ThemedColor
import activator.doodler.doodle.extensions.color
import activator.doodler.doodle.extensions.shorten
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.TextUnit
import doodler.doodle.extensions.hierarchy
import doodler.doodle.structures.ArrayValueDoodle
import doodler.doodle.structures.ReadonlyDoodle
import doodler.doodle.structures.TagDoodle
import doodler.nbt.TagType
import doodler.theme.DoodlerTheme
import doodler.types.BooleanProvider
import doodler.types.FalseProvider
import doodler.types.TrueProvider
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
    AnnotatedString.Range(SpanStyle(color = ThemedColor.Editor.Tag.General), 0, 3)
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
    selected: BooleanProvider = TrueProvider
) = Box (
    modifier = Modifier
        .wrapContentSize()
        .background(DoodlerTheme.Colors.DoodleItem.TagTypeBackground(selected()), RoundedCornerShape(5.dp))
        .padding(top = 2.dp, bottom = 2.dp, start = 3.dp, end = 3.dp),
    content = { TagDoodleTypeText(type = type, enabled = TrueProvider) }
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
        .background(ThemedColor.Editor.indicator(selected()), shape = RoundedCornerShape(5.dp))
        .padding(top = 2.dp, bottom = 2.dp, start = 5.dp, end = 5.dp),
    content = {
        TagDoodleContentText(
            text = "$index:",
            color = ThemedColor.Editor.indicatorText(selected()),
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
) {
    Box(modifier = Modifier.weight(1f).fillMaxHeight()
        .clickable(pressInteractionSource, null) { }
        .mouseClickable(onClick = onClick)
        .hoverable(hoverInteractionSource)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxHeight(),
        ) {
            when (doodle) {
                is TagDoodle -> TagDoodleContent(doodle)
                is ArrayValueDoodle -> ArrayValueDoodleContent(doodle.index, doodle.value)
            }
        }
    }
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

