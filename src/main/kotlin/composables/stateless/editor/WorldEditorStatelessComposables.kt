package composables.stateless.editor

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.BottomAppBar
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
fun MainColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), content = content)
}

@Composable
fun ColumnScope.TopBar(content: @Composable RowScope.() -> Unit) {
    TopAppBar(
        elevation = 0.dp,
        backgroundColor = ThemedColor.ActionBar,
        contentPadding = PaddingValues(start = 25.dp, top = 10.dp, bottom = 10.dp),
        modifier = Modifier
            .height(60.dp)
            .zIndex(1f)
            .drawBehind(border(bottom = Pair(1f, ThemedColor.TopBar))),
        content = content
    )
}

@Composable
fun RowScope.TopBarText(text: String) {
    Text(text, color = Color.White, fontSize = 25.sp)
}

@Composable
fun ColumnScope.MainArea(content: @Composable RowScope.() -> Unit) {
    Row (
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .background(ThemedColor.EditorArea)
            .zIndex(0f),
        content = content
    )
}

@Composable
fun RowScope.MainFiles(content: @Composable BoxScope.() -> Unit) {
    Box (modifier = Modifier.fillMaxHeight().requiredWidth(400.dp).background(ThemedColor.TaskArea), content = content)
}

@Composable
fun RowScope.MainContents(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .weight(0.7f),
        content = content
    )
}

@Composable
fun ColumnScope.BottomBar(content: @Composable RowScope.() -> Unit) {
    BottomAppBar(
        elevation = 0.dp,
        backgroundColor = ThemedColor.TaskArea,
        contentPadding = PaddingValues(top = 0.dp, bottom = 0.dp, start = 25.dp, end = 25.dp),
        modifier = Modifier
            .height(40.dp)
            .zIndex(1f)
            .drawBehind(border(top = Pair(1f, ThemedColor.TopBarBorder))),
        content = content
    )
}

@Composable
fun RowScope.BottomBarText(text: String) {
    Text(
        text,
        color = ThemedColor.Copyright,
        fontSize = 14.sp
    )
}

@Composable
fun BoxScope.EditorRoot(content: @Composable ColumnScope.() -> Unit) {
    Column (
        modifier = Modifier
            .fillMaxSize()
            .zIndex(100f),
        content = content
    )
}

@Composable
fun ColumnScope.Editables(content: @Composable BoxScope.() -> Unit) {
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
        fontSize = 21.sp,
        color = if (invalid) ThemedColor.Editor.Selector.Malformed else ThemedColor.Editor.Tag.General,
        fontFamily = JetBrainsMono,
        modifier = Modifier.focusable(false)
    )
}

@Composable
fun RowScope.CoordinateInput(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    transformer: (AnnotatedString) -> TransformedText = { TransformedText(it, OffsetMapping.Identity) }
) {
    BasicTextField(
        value,
        onValueChange,
        textStyle = TextStyle(
            fontSize = 21.sp,
            color = ThemedColor.Editor.Tag.General,
            fontFamily = JetBrainsMono
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        cursorBrush = SolidColor(ThemedColor.Editor.Tag.General),
        visualTransformation = transformer,
        modifier = Modifier
            .width((value.text.length.coerceAtLeast(1) * 12.75).dp)
            .focusable(false)
            .onFocusChanged {
                if (!it.isFocused && value.text.isEmpty()) onValueChange(TextFieldValue("-"))
            }
    )
}

@Composable
fun RowScope.ChunkSelectorDropdown(prefix: String, accent: Boolean = false, valid: Boolean = true, content: @Composable RowScope.() -> Unit) {
    Row (
        modifier = Modifier
            .background(
                ThemedColor.Editor.Selector.background(accent, valid),
                RoundedCornerShape(4.dp)
            )
            .wrapContentWidth()
            .height(45.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(17.dp))
        Text(
            prefix,
            fontSize = 18.sp,
            color = ThemedColor.Editor.Selector.Normal,
            fontFamily = JetBrainsMono
        )
        Spacer(modifier = Modifier.width(10.dp))
        content()
        Spacer(modifier = Modifier.width(17.dp))
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
