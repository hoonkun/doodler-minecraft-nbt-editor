package activator.composables.global

import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import activator.doodler.logger.DoodlerLogger

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LinkText(
    text: String,
    color: Color,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    fontFamily: FontFamily = FontFamily.Default,
    onClick: () -> Unit
) {
    DoodlerLogger.recomposition("LinkText")

    var active by remember { mutableStateOf(false) }

    Text(
        AnnotatedString(
            text, listOf(
                AnnotatedString.Range(
                    SpanStyle(
                        color = color,
                        fontSize = fontSize,
                        textDecoration = if (active) TextDecoration.Underline else TextDecoration.None,
                        fontFamily = fontFamily
                    ), 0, text.length
                )
            )
        ),
        modifier = Modifier
            .then(modifier)
            .onPointerEvent(PointerEventType.Enter) { active = true }
            .onPointerEvent(PointerEventType.Exit) { active = false }
            .onPointerEvent(PointerEventType.Release) { onClick() }
    )
}