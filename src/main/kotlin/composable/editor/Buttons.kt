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
import doodler.unit.ddp
import doodler.unit.dsp


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ActionButton(
    enabled: BooleanProvider = TrueProvider,
    onRightClick: () -> Unit = EmptyLambda,
    onClick: () -> Unit,
    onDisabledClick: () -> Unit = EmptyLambda,
    content: @Composable BoxScope.() -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    Box(
        modifier = Modifier
            .wrapContentSize()
            .padding(top = 1.ddp, bottom = 1.ddp)
            .hoverable(interaction, enabled())
            .mouseClickable {
                if (buttons.isPrimaryPressed) {
                    if (enabled()) onClick()
                    else onDisabledClick()
                }
                if (buttons.isSecondaryPressed) onRightClick()
            }
            .drawBehind {
                val color =
                    if (hovered) Color.Black.copy(alpha = 0.1176f)
                    else Color.Transparent
                drawRoundRect(color = color, cornerRadius = CornerRadius(2.25.ddp.value))
            }
            .alpha(if (enabled()) 1f else 0.3f),
        content = {
            Box(modifier = Modifier.padding(3.75.ddp), content = content)
        }
    )
}

@Composable
fun TagCreatorButton(
    type: TagType,
    enabled: BooleanProvider = TrueProvider,
    onClick: () -> Unit,
    onDisabledClick: () -> Unit = EmptyLambda
) = ActionButton(enabled, onClick = onClick, onDisabledClick = onDisabledClick) {
    TagDoodleTypeText(type, enabled = enabled, fontSize = 9.dsp)
}

@Composable
fun EditorActionButton(
    text: String,
    color: Color,
    fontSize: TextUnit = 9.dsp,
    rotate: Pair<Float, Int>? = null,
    enabled: BooleanProvider = TrueProvider,
    onRightClick: () -> Unit = EmptyLambda,
    onDisabledClick: () -> Unit = EmptyLambda,
    onClick: () -> Unit
) = ActionButton(enabled, onClick = onClick, onRightClick = onRightClick, onDisabledClick = onDisabledClick) {
    DoodleText(
        text = text,
        color = color,
        fontSize = fontSize,
        rotate = rotate
    )
}
