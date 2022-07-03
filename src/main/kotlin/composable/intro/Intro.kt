package composable.intro

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import composable.global.ClickableText
import doodler.application.structure.DoodlerEditorType
import doodler.application.structure.IntroDoodlerWindow
import doodler.extension.ellipsisLast
import doodler.extension.ellipsisStart
import doodler.local.Recent
import doodler.local.UserSavedLocalState
import doodler.theme.DoodlerTheme
import doodler.unit.ddp
import doodler.unit.dsp
import java.io.File
import javax.imageio.ImageIO


private val Padding get() = 15.625.ddp

fun Modifier.settingsBlur(blur: Boolean) = this.let { if (blur) it.blur(15.ddp) else it }

@Composable
fun Intro(
    window: IntroDoodlerWindow,
    openRecent: (DoodlerEditorType, File) -> Unit,
    openSelector: (DoodlerEditorType) -> Unit,
    changeGlobalScale: (Float) -> Unit
) {

    var settingsVisible by window.introState.settingsVisible

    val openWorld = { openSelector(DoodlerEditorType.World) }
    val openStandalone = { openSelector(DoodlerEditorType.Standalone) }

    val title = "doodler :1.0 with 1.19"

    IntroRoot {
        Image(
            painter = painterResource("/doodler_header.png"),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                DoodlerTheme.Colors.Background.copy(alpha = 0.4f),
                                DoodlerTheme.Colors.Background
                            ),
                            end = Offset(0f, size.height * 2 / 3)
                        )
                    )
                }
                .settingsBlur(settingsVisible)
        )
        MenuButton(settingsVisible) { settingsVisible = true }
        IntroContent(settingsVisible) {
            MainTopColumn {
                Row(modifier = Modifier.padding(18.75.ddp)) {
                    Image(
                        painter = painterResource("/icons/intro/doodler_icon_large.png"),
                        contentDescription = null,
                        modifier = Modifier.height(48.75.ddp).offset(y = 0.625.ddp)
                    )
                    Spacer(modifier = Modifier.width(8.75.ddp))
                    Column {
                        Text(
                            AnnotatedString(
                                text = title,
                                spanStyles = listOf(
                                    AnnotatedString.Range(
                                        SpanStyle(
                                            fontSize = 10.dsp,
                                            color = Color.White.copy(alpha = 0.6f)
                                        ),
                                        start = 8,
                                        end = title.length
                                    )
                                )
                            ),
                            fontSize = 25.dsp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(3.75.ddp))
                        Row {
                            ClickableText(
                                text = "Getting Started?",
                                color = DoodlerTheme.Colors.ExternalLink,
                                fontSize = 10.dsp,
                                onClick = { },
                                modifier = Modifier.padding(bottom = 3.125.ddp)
                            )
                            Spacer(modifier = Modifier.width(12.5.ddp))
                            ClickableText(
                                text = "README.md",
                                color = DoodlerTheme.Colors.ExternalLink,
                                fontSize = 10.dsp,
                                onClick = { },
                                modifier = Modifier.padding(bottom = 3.125.ddp)
                            )
                            Spacer(modifier = Modifier.width(12.5.ddp))
                            Text(
                                text = "by ",
                                color = Color.White.copy(alpha = 0.2f),
                                fontSize = 10.dsp
                            )
                            ClickableText(
                                text = AnnotatedString("@hoon_kiwicraft"),
                                color = DoodlerTheme.Colors.Text.IdeDocumentation,
                                hoverAlpha = 1.0f,
                                normalAlpha = 0.5f,
                                fontSize = 10.dsp,
                                onClick = { }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(11.25.ddp))
                Text(
                    text = "New here?",
                    fontSize = 12.dsp,
                    color = DoodlerTheme.Colors.Text.LightGray
                )
                Spacer(modifier = Modifier.height(5.ddp))
            }
            MainMiddleColumn {
                OpenNewButton(
                    text = "World",
                    suffix = "view files in world",
                    image = "/icons/intro/open_new_world.png",
                    onClick = openWorld
                )
                OpenNewButton(
                    text = "Standalone",
                    suffix = "view single nbt file",
                    image = "/icons/intro/open_new_standalone.png",
                    onClick = openStandalone
                )
            }
            MainBottomColumn {
                Spacer(modifier = Modifier.height(20.ddp))
                Text(
                    text = "Recent...",
                    fontSize = 12.dsp,
                    color = DoodlerTheme.Colors.Text.LightGray
                )
                Spacer(modifier = Modifier.height(5.ddp))
                Recent(openRecent)
            }
        }

        if (settingsVisible) {
            SettingsMenu(
                onClose = { settingsVisible = false },
                onGlobalScaleChanged = changeGlobalScale
            )
        }
    }
}

@Composable
fun IntroRoot(content: @Composable BoxScope.() -> Unit) =
    Box(
        modifier = Modifier.fillMaxSize().background(DoodlerTheme.Colors.Background),
        content = content
    )

@Composable
fun IntroContent(blur: Boolean, content: @Composable ColumnScope.() -> Unit) =
    Column(
        modifier = Modifier.fillMaxSize().settingsBlur(blur),
        content = content
    )

@Composable
fun MainTopColumn(content: @Composable ColumnScope.() -> Unit) =
    Column(
        modifier = Modifier.padding(top = Padding, start = Padding, end = Padding),
        content = content
    )

@Composable
fun MainMiddleColumn(content: @Composable RowScope.() -> Unit) =
    Row(
        modifier = Modifier.padding(horizontal = 10.3125.ddp),
        content = content
    )

@Composable
fun MainBottomColumn(content: @Composable ColumnScope.() -> Unit) =
    Column(
        modifier = Modifier.padding(bottom = Padding, start = Padding, end = Padding),
        content = content
    )

@Composable
fun RowScope.OpenNewButton(
    text: String,
    suffix: String,
    image: String,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    Box(
        modifier = Modifier
            .hoverable(interaction)
            .clickable(onClick = onClick)
            .drawBehind {
                drawRoundRect(
                    if (hovered) Color.Black.copy(alpha = 0.125f)
                    else Color.Transparent,
                    cornerRadius = CornerRadius(x = 3.75.ddp.value, y = 3.75.ddp.value)
                )
            }
            .weight(1f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(5.ddp)
        ) {
            Box(
                modifier = Modifier
                    .background(DoodlerTheme.Colors.Intro.IconBackgroundColor, shape = RoundedCornerShape(3.75.ddp))
            ) {
                Image(
                    painter = painterResource(image),
                    contentDescription = null,
                    modifier = Modifier.size(36.25.ddp).padding(2.5.ddp)
                )
            }
            Column(modifier = Modifier.padding(start = 8.75.ddp)) {
                SmallText(text = suffix)
                Text(
                    text = text,
                    fontSize = 15.dsp,
                    color = DoodlerTheme.Colors.Text.IdeFunctionName
                )
            }
        }
    }
}

@Composable
fun ColumnScope.Recent(
    openRecent: (DoodlerEditorType, File) -> Unit
) {
    val hs = rememberScrollState()

    val deleteRecent: (Recent) -> Unit = {
        UserSavedLocalState.recent.remove(it)
        UserSavedLocalState.save()
    }
    val openWorld: (File) -> Unit = { file -> openRecent(DoodlerEditorType.World, file) }
    val openStandalone: (File) -> Unit = { file -> openRecent(DoodlerEditorType.Standalone, file) }

    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .background(DoodlerTheme.Colors.BackgroundDark, shape = RoundedCornerShape(6.25.ddp))
    ) {
        Box (
            modifier = Modifier
                .padding(5.ddp)
                .horizontalScroll(hs)
                .let { if (UserSavedLocalState.recent.isEmpty()) it.align(Alignment.Center) else it }
        ) {
            if (UserSavedLocalState.recent.isEmpty()) {
                Text(
                    text = "No recently opened worlds or files... :(\nTry to open something!",
                    textAlign = TextAlign.Center,
                    color = DoodlerTheme.Colors.Text.IdeComment,
                    fontSize = 12.dsp,
                )
            } else {
                Row {
                    for (item in UserSavedLocalState.recent) {
                        if (item.type == DoodlerEditorType.World) {
                            key(item.path) { WorldRecentItem(item, deleteRecent, openWorld) }
                        } else {
                            key(item.path) { StandaloneRecentItem(item, deleteRecent, openStandalone) }
                        }
                    }
                }
            }
        }
        HorizontalScrollbar(
            adapter = ScrollbarAdapter(hs),
            style = DoodlerTheme.ScrollBar.Intro,
            modifier = Modifier.align(Alignment.BottomStart)
        )
    }
}

@Composable
fun WorldRecentItem(
    data: Recent,
    delete: (Recent) -> Unit,
    reopen: (File) -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    val exists by remember(data.path) { mutableStateOf(File(data.path).exists()) }

    val file = remember(data.path) { File("${data.path}/icon.png") }

    val worldIconPainter = remember(file) { if (file.exists()) ImageIO.read(file).toComposeImageBitmap() else null }
    val fallbackWorldIcon = painterResource("/icons/intro/open_new_world.png")

    val contentDescription = remember { "world preview icon of ${data.name}" }
    val imageModifier = Modifier
        .size(52.5.ddp)
        .clip(RoundedCornerShape(3.75.ddp))

    Box(
        modifier = Modifier
            .hoverable(interaction, exists)
            .clickable(exists) { reopen(File(data.path)) }
            .drawBehind {
                drawRoundRect(
                    if (hovered) Color.Black.copy(alpha = 0.15f)
                    else Color.Transparent,
                    cornerRadius = CornerRadius(x = 3.75.ddp.value, y = 3.75.ddp.value)
                )
            }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 8.75.ddp, vertical = 6.25.ddp)
                .alpha(if (exists) 1f else 0.2f)
        ) {
            if (worldIconPainter != null) {
                Image(
                    bitmap = worldIconPainter,
                    contentDescription = contentDescription,
                    filterQuality = FilterQuality.None,
                    modifier = imageModifier
                )
            } else {
                Image(
                    painter = fallbackWorldIcon,
                    contentDescription = contentDescription,
                    modifier = imageModifier
                )
            }
            Spacer(modifier = Modifier.height(7.5.ddp))
            RecentItemTexts(data)
        }
        if (!exists)
            Text(
                text = "Cannot Find",
                color = Color.White,
                fontSize = 8.dsp,
                modifier = Modifier.align(Alignment.Center)
            )
        if (hovered || !exists) DeleteRecentButton { delete(data) }
    }
}

@Composable
fun StandaloneRecentItem(
    data: Recent,
    delete: (Recent) -> Unit,
    reopen: (File) -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    val icon = painterResource("/icons/intro/open_new_standalone.png")

    val imageModifier = Modifier
        .size(52.5.ddp)
        .clip(RoundedCornerShape(3.75.ddp))

    Box(
        modifier = Modifier
            .hoverable(interaction)
            .clickable { reopen(File(data.path)) }
            .drawBehind {
                drawRoundRect(
                    if (hovered) Color.Black.copy(alpha = 0.15f)
                    else Color.Transparent,
                    cornerRadius = CornerRadius(x = 3.75.ddp.value, y = 3.75.ddp.value)
                )
            }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxHeight().padding(horizontal = 8.75.ddp, vertical = 6.25.ddp)
        ) {
            Image(
                painter = icon,
                contentDescription = null,
                modifier = imageModifier
            )
            Spacer(modifier = Modifier.height(7.5.ddp))
            RecentItemTexts(data)
        }
        if (hovered) DeleteRecentButton { delete(data) }
    }
}

@Composable
fun RecentItemTexts(
    data: Recent
) {
    Text(
        text = data.name.ellipsisLast(10),
        color = Color.White.copy(alpha = 0.85f),
        fontSize = 7.8.dsp
    )
    Spacer(modifier = Modifier.height(3.75.ddp))
    Text(
        text = data.path.ellipsisStart(15),
        color = Color.White.copy(alpha = 0.65f),
        fontSize = 6.5.dsp
    )
}

@Composable
fun SmallText(text: String) =
    Text(
        text = text,
        fontSize = 10.dsp,
        color = DoodlerTheme.Colors.Text.IdeComment
    )

@Composable
fun BoxScope.DeleteRecentButton(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .padding(3.ddp)
            .size(15.ddp)
            .drawBehind {
                if (hovered) drawRoundRect(Color.White.copy(alpha = 0.075f), cornerRadius = CornerRadius(7.5.ddp.value))
            }
            .align(Alignment.TopEnd)
            .hoverable(interaction)
            .clickable { onClick() }
    ) {
        Text(
            text = "\u2715",
            fontWeight = FontWeight.Bold,
            fontSize = 9.dsp,
            color = Color.White.copy(alpha = 0.4f)
        )
    }
}
