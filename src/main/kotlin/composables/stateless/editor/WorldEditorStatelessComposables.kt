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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
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
        backgroundColor = Color(55, 55, 57),
        contentPadding = PaddingValues(start = 25.dp, top = 10.dp, bottom = 10.dp),
        modifier = Modifier
            .height(60.dp)
            .zIndex(1f)
            .drawBehind(border(bottom = Pair(1f, Color(30, 30, 30)))),
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
            .background(Color(43, 43, 43))
            .zIndex(0f),
        content = content
    )
}

@Composable
fun RowScope.MainFiles(content: @Composable BoxScope.() -> Unit) {
    Box (modifier = Modifier.fillMaxHeight().weight(0.3f), content = content)
}


@Composable
fun BoxScope.FileCategoryListScrollable(scrollState: ScrollState, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .background(Color(60, 63, 65))
            .verticalScroll(scrollState),
        content = content
    )
}

@Composable
fun BoxScope.FileCategoryListScrollbar(scrollState: ScrollState) {
    VerticalScrollbar(
        ScrollbarAdapter(scrollState),
        style = ScrollbarStyle(
            100.dp,
            15.dp,
            RectangleShape,
            250,
            Color(255, 255, 255, 50),
            Color(255, 255, 255, 100)
        ),
        modifier = Modifier.align(Alignment.TopEnd)
    )
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
        backgroundColor = Color(60, 63, 65),
        contentPadding = PaddingValues(top = 0.dp, bottom = 0.dp, start = 25.dp, end = 25.dp),
        modifier = Modifier
            .height(40.dp)
            .zIndex(1f)
            .drawBehind(border(top = Pair(1f, Color(36, 36, 36)))),
        content = content
    )
}

@Composable
fun RowScope.BottomBarText(text: String) {
    Text(
        text,
        color = Color(255, 255, 255, 180),
        fontSize = 14.sp
    )
}

@Composable
fun ColumnScope.FileCategoryItems(
    parent: CategoryData,
    selected: String,
    onClick: (CategoryItemData) -> Unit
) {
    for (item in parent.items) {
        FileCategoryItem(item, selected, onClick)
    }
}

@Composable
fun ColumnScope.CategoriesBottomMargin() {
    Spacer(modifier = Modifier.height(25.dp))
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
        color = if (invalid) Color(100, 100, 100) else Color(169, 183, 198),
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
            color = Color(169, 183, 198),
            fontFamily = JetBrainsMono
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        cursorBrush = SolidColor(Color(169, 183, 198)),
        visualTransformation = transformer,
        modifier = Modifier
            .width((value.text.length.coerceAtLeast(1) * 12.75).dp)
            .focusable(false)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RowScope.AnvilSelectorDropdown(prefix: String, accent: Boolean = false, valid: Boolean = true, content: @Composable RowScope.() -> Unit) {
    Row (
        modifier = Modifier
            .background(
                if (accent && valid) Color(50, 54, 47)
                else if (accent) Color(64, 55, 52)
                else Color(42, 42, 42),
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
            color = Color(125, 125, 125),
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
            color = Color(255, 255, 255, 100),
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
            color = Color(255, 255, 255, 185),
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
        color = Color(255, 255, 255, 145),
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
