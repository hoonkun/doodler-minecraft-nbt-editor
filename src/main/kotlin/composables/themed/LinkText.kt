package composables.themed

import androidx.compose.foundation.background
import androidx.compose.foundation.text.ClickableText
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LinkText(
    text: String,
    color: Color,
    fontSize: TextUnit,
    modifier: Modifier = Modifier.background(Color.Transparent),
    onClick: (Int) -> Unit
) {
    var active by remember { mutableStateOf(false) }

    ClickableText(
        AnnotatedString(
            text, listOf(
                AnnotatedString.Range(
                    SpanStyle(
                        color = color,
                        fontSize = fontSize,
                        textDecoration = if (active) TextDecoration.Underline else TextDecoration.None
                    ), 0, text.length
                )
            )
        ),
        onClick = onClick,
        modifier = Modifier
            .then(modifier)
            .onPointerEvent(PointerEventType.Enter) { active = true }
            .onPointerEvent(PointerEventType.Exit) { active = false }
    )
}