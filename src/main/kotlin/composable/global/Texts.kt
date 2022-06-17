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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import doodler.theme.DoodlerTheme

@Composable
fun ClickableText(
    text: String,
    color: Color,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    ClickableText(
        text = AnnotatedString(text),
        color = color,
        fontSize = fontSize,
        modifier = modifier,
        onClick = onClick
    )
}

@Composable
fun ClickableText(
    text: AnnotatedString,
    color: Color,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    hoverAlpha: Float = 1f,
    normalAlpha: Float = 1f,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()

    Text(
        text,
        color = color,
        fontSize = fontSize,
        fontFamily = DoodlerTheme.Fonts.JetbrainsMono,
        style = TextStyle(textDecoration = if (hovered) TextDecoration.Underline else TextDecoration.None),
        modifier = Modifier.then(modifier)
            .hoverable(interactionSource)
            .clickable(onClick = onClick)
            .alpha(if (hovered) hoverAlpha else normalAlpha)
    )
}