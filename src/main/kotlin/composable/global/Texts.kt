package composable.global

import activator.composables.global.JetBrainsMono
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import doodler.theme.DoodlerTheme
import doodler.unit.dp

@Composable
fun DefaultH4(text: String) {
    Text(
        text,
        color = DoodlerTheme.Colors.OnBackground,
        fontSize = MaterialTheme.typography.h4.fontSize,
        modifier = Modifier.alpha(0.75f).padding(bottom = 10.dp)
    )
}

@Composable
fun ExternalLinkH5(
    text: String, modifier: Modifier = Modifier
) {
    ClickableH6(
        text,
        DoodlerTheme.Colors.ExternalLink,
        modifier = Modifier.then(modifier)
    ) { }
}

@Composable
fun PrimaryLinkH1(
    text: String,
    onClick: () -> Unit
) {
    ClickableH1(
        text = text,
        color = DoodlerTheme.Colors.PrimaryLink,
        onClick = onClick
    )
}

@Composable
fun PrimaryLinkH5(
    text: String,
    onClick: () -> Unit
) {
    ClickableH5(
        text = text,
        color = DoodlerTheme.Colors.PrimaryLink,
        onClick = onClick
    )
}

@Composable
fun ClickableH1(
    text: String,
    color: Color,
    onClick: () -> Unit
) = ClickableText(text, color = color, fontSize = MaterialTheme.typography.h1.fontSize, onClick = onClick)

@Composable
fun ClickableH5(
    text: String,
    color: Color,
    onClick: () -> Unit
) = ClickableText(text, color = color, fontSize = MaterialTheme.typography.h5.fontSize, onClick = onClick)

@Composable
fun ClickableH6(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) = ClickableText(
    text = text,
    color = color,
    fontSize = MaterialTheme.typography.h6.fontSize,
    onClick = onClick,
    modifier = Modifier.then(modifier)
)

@Composable
fun ClickableText(
    text: String,
    color: Color,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()

    Text(
        text,
        color = color,
        fontSize = fontSize,
        fontFamily = JetBrainsMono,
        style = TextStyle(textDecoration = if (hovered) TextDecoration.Underline else TextDecoration.None),
        modifier = Modifier.then(modifier).hoverable(interactionSource).clickable(onClick = onClick)
    )
}