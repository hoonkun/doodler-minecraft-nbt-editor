package composable.editor

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.mouseClickable
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.unit.TextUnit
import doodler.nbt.TagType
import doodler.types.BooleanProvider
import doodler.types.EmptyLambda
import doodler.types.TrueProvider
import doodler.unit.dp


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ActionButton(
    enabled: BooleanProvider = TrueProvider,
    onRightClick: () -> Unit = EmptyLambda,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    val hoverInteractionSource = remember { MutableInteractionSource() }
    val hovered by hoverInteractionSource.collectIsHoveredAsState()

    Box(
        modifier = Modifier
            .wrapContentSize()
            .padding(top = 4.dp, bottom = 4.dp)
            .hoverable(hoverInteractionSource, enabled())
            .mouseClickable(enabled()) {
                if (buttons.isPrimaryPressed) onClick()
                if (buttons.isSecondaryPressed) onRightClick()
            }
            .drawBehind {
                val color =
                    if (hovered) Color.Black.copy(alpha = 0.1176f)
                    else Color.Transparent
                drawRoundRect(color = color, cornerRadius = CornerRadius(3.dp.value))
            }
            .alpha(if (enabled()) 1f else 0.3f),
        content = {
            Box(modifier = Modifier.padding(top = 8.dp, bottom = 8.dp, start = 8.dp, end = 8.dp), content = content)
        }
    )
}

@Composable
fun TagCreatorButton(
    type: TagType,
    enabled: BooleanProvider = TrueProvider,
    onClick: () -> Unit
) {
    ActionButton(enabled, onClick = onClick) {
        TagDoodleTypeText(type, enabled = enabled, fontSize = MaterialTheme.typography.h4.fontSize)
    }
}

@Composable
fun EditorActionButton(
    text: String,
    color: Color,
    fontSize: TextUnit = MaterialTheme.typography.h4.fontSize,
    rotate: Pair<Float, Int>? = null,
    enabled: BooleanProvider = TrueProvider,
    onRightClick: () -> Unit = EmptyLambda,
    onClick: () -> Unit
) {
    ActionButton(enabled, onClick = onClick, onRightClick = onRightClick) {
        DoodleText(
            text = text,
            color = color,
            fontSize = fontSize,
            rotate = rotate
        )
    }
}
