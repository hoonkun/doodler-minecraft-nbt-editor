package composables.stateless.editor

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import composables.themed.*
import java.awt.Desktop
import java.net.URI
import java.util.*


@Composable
fun BoxScope.EditorManagerRoot(content: @Composable ColumnScope.() -> Unit) {
    Column (
        modifier = Modifier
            .fillMaxSize()
            .zIndex(100f),
        content = content
    )
}

@Composable
fun ColumnScope.Editors(content: @Composable BoxScope.() -> Unit) {
    Box (
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        content = content
    )
}

@Composable
fun RowScope.CoordinateText(text: String, invalid: Boolean = false) {
    Text(
        text,
        fontSize = 16.sp,
        color = if (invalid) ThemedColor.Editor.Selector.Invalid else ThemedColor.Editor.Tag.General,
        fontFamily = JetBrainsMono,
        modifier = Modifier.focusable(false)
    )
}

@Composable
fun RowScope.CoordinateInput(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    transformer: (AnnotatedString) -> TransformedText = { TransformedText(it, OffsetMapping.Identity) },
    disabled: Boolean = false
) {
    BasicTextField(
        value,
        onValueChange,
        textStyle = TextStyle(
            fontSize = 16.sp,
            color = ThemedColor.Editor.Tag.General,
            fontFamily = JetBrainsMono
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        cursorBrush = SolidColor(ThemedColor.Editor.Tag.General),
        visualTransformation = transformer,
        enabled = !disabled,
        modifier = Modifier
            .width((value.text.length.coerceAtLeast(1) * 9.75).dp)
            .focusable(false)
            .onFocusChanged {
                if (!it.isFocused && value.text.isEmpty()) onValueChange(TextFieldValue("-"))
            }
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun ChunkSelectorDropdown(
    prefix: String,
    accent: Boolean = false,
    valid: Boolean = true,
    modifier: Modifier = Modifier,
    disabled: Boolean = false,
    onClick: (MouseClickScope.() -> Unit)? = null,
    content: @Composable RowScope.(Boolean) -> Unit
) {
    var hover by remember { mutableStateOf(false) }

    Row (
        modifier = Modifier.then(modifier)
            .background(
                ThemedColor.from(
                    ThemedColor.Editor.Selector.background(accent, valid, onClick != null && hover),
                    alpha = if (disabled) (255 * 0.6f).toInt() else 255
                ),
                RoundedCornerShape(4.dp)
            )
            .height(40.dp)
            .alpha(if (disabled) 0.6f else 1f)
            .let {
                if (onClick != null)
                    it.mouseClickable(onClick = onClick)
                        .onPointerEvent(PointerEventType.Enter) { hover = true }
                        .onPointerEvent(PointerEventType.Exit) { hover = false }
                else it
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            prefix,
            fontSize = 14.sp,
            color = ThemedColor.Editor.Selector.Normal,
            fontFamily = JetBrainsMono
        )
        Spacer(modifier = Modifier.width(8.dp))
        content(disabled)
        Spacer(modifier = Modifier.width(12.dp))
    }
}

@Composable
fun BoxScope.NoFileSelected(worldName: String) {
    Column (
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            "< world >",
            color = ThemedColor.WhiteOthers,
            fontSize = 29.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            worldName,
            color = Color.White,
            fontSize = 38.sp
        )
        Spacer(modifier = Modifier.height(35.dp))
        Text(
            "Select Tab in Left Area!",
            color = ThemedColor.WhiteSecondary,
            fontSize = 33.sp
        )
        Spacer(modifier = Modifier.height(25.dp))
        WhatIsThis("")
    }
}

@Composable
fun WhatIsThis(link: String) {
    Spacer(modifier = Modifier.height(60.dp))
    Text(
        "What is this?",
        color = ThemedColor.DocumentationDescription,
        fontSize = 25.sp
    )
    Spacer(modifier = Modifier.height(10.dp))
    LinkText(
        "Documentation",
        color = ThemedColor.Link,
        fontSize = 22.sp
    ) {
        openBrowser(link)
    }
}

fun openBrowser(url: String) {
    val osName = System.getProperty("os.name").lowercase(Locale.getDefault())

    val others: () -> Unit = {
        val runtime = Runtime.getRuntime()
        if (osName.contains("mac")) {
            runtime.exec("open $url")
        } else if (osName.contains("nix") || osName.contains("nux")) {
            runtime.exec("xdg-open $url")
        }
    }

    try {
        if (Desktop.isDesktopSupported()) {
            val desktop = Desktop.getDesktop()
            if (desktop.isSupported(Desktop.Action.BROWSE)) desktop.browse(URI(url))
            else others()
        } else {
            others()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
