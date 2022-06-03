package composable.global

import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import doodler.unit.sp


@Composable
fun ClickableH1(
    text: String,
    color: Color,
    onClick: () -> Unit
) = ClickableText(text, color = color, fontSize = 25.sp, onClick = onClick)

@Composable
fun ClickableH4(
    text: String,
    color: Color,
    onClick: () -> Unit
) = ClickableText(text, color = color, fontSize = 12.sp, onClick = onClick)

@Composable
fun ClickableText(
    text: String,
    color: Color,
    fontSize: TextUnit,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()

    Text(
        text,
        color = color,
        fontSize = fontSize,
        style = TextStyle(textDecoration = if (hovered) TextDecoration.Underline else TextDecoration.None),
        modifier = Modifier.hoverable(interactionSource).clickable(onClick = onClick)
    )
}