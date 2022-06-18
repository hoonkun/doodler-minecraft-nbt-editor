package composable.intro

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import composable.global.ClickableText
import doodler.application.structure.DoodlerEditorType
import doodler.theme.DoodlerTheme
import doodler.unit.ddp

private val TextStyle.fsp get() = this.fontSize

private val Int.sdp get() = this.ddp * 1.25f
private val Double.sdp get() = this.ddp * 1.25f

private val Padding = 12.5.sdp

@Composable
fun Intro(
    openSelector: (DoodlerEditorType) -> Unit
) {

    val openWorld = { openSelector(DoodlerEditorType.World) }

    val title = "doodler :1.0 with 1.18.2"

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
        )
        IntroContent {
            MainTopColumn {
                Row(modifier = Modifier.padding(15.sdp)) {
                    Image(
                        painter = painterResource("/icons/intro/doodler_icon_large.png"),
                        contentDescription = null,
                        modifier = Modifier.height(39.sdp).offset(y = 0.5.sdp)
                    )
                    Spacer(modifier = Modifier.width(7.sdp))
                    Column {
                        Text(
                            AnnotatedString(
                                text = title,
                                spanStyles = listOf(
                                    AnnotatedString.Range(
                                        SpanStyle(
                                            fontSize = MaterialTheme.typography.h6.fsp,
                                            color = Color.White.copy(alpha = 0.6f)
                                        ),
                                        start = 8,
                                        end = title.length
                                    )
                                )
                            ),
                            fontSize = MaterialTheme.typography.h1.fsp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(3.sdp))
                        Row {
                            ClickableText(
                                text = "Getting Started?",
                                color = DoodlerTheme.Colors.ExternalLink,
                                fontSize = MaterialTheme.typography.h6.fsp,
                                onClick = { },
                                modifier = Modifier.padding(bottom = 2.5.sdp)
                            )
                            Spacer(modifier = Modifier.width(10.sdp))
                            ClickableText(
                                text = "README.md",
                                color = DoodlerTheme.Colors.ExternalLink,
                                fontSize = MaterialTheme.typography.h6.fsp,
                                onClick = { },
                                modifier = Modifier.padding(bottom = 2.5.sdp)
                            )
                            Spacer(modifier = Modifier.width(10.sdp))
                            Text(
                                text = "by ",
                                color = Color.White.copy(alpha = 0.2f),
                                fontSize = MaterialTheme.typography.h6.fsp
                            )
                            ClickableText(
                                text = AnnotatedString("@hoon_kiwicraft"),
                                color = DoodlerTheme.Colors.Text.IdeDocumentation,
                                hoverAlpha = 1.0f,
                                normalAlpha = 0.5f,
                                fontSize = MaterialTheme.typography.h6.fsp,
                                onClick = { }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(9.sdp))
                Text(
                    text = "New here?",
                    fontSize = MaterialTheme.typography.h5.fsp,
                    color = DoodlerTheme.Colors.Text.LightGray
                )
                Spacer(modifier = Modifier.height(4.sdp))
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
                    onClick = openWorld
                )
            }
            MainBottomColumn {
                Spacer(modifier = Modifier.height(16.sdp))
                Text(
                    text = "Recent...",
                    fontSize = MaterialTheme.typography.h5.fsp,
                    color = DoodlerTheme.Colors.Text.LightGray
                )
                Spacer(modifier = Modifier.height(4.sdp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(DoodlerTheme.Colors.BackgroundDark, shape = RoundedCornerShape(5.sdp))
                ) {
                    Text(
                        text = "No recently opened worlds or files... :(\nTry to open something!",
                        textAlign = TextAlign.Center,
                        color = DoodlerTheme.Colors.Text.IdeComment,
                        fontSize = MaterialTheme.typography.h5.fsp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
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
fun IntroContent(content: @Composable ColumnScope.() -> Unit) =
    Column(
        modifier = Modifier.fillMaxSize(),
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
        modifier = Modifier.padding(horizontal = 8.25.sdp),
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
    val hoverInteractionSource = remember { MutableInteractionSource() }
    val hovered by hoverInteractionSource.collectIsHoveredAsState()

    Box(
        modifier = Modifier
            .hoverable(hoverInteractionSource)
            .clickable(onClick = onClick)
            .drawBehind {
                drawRoundRect(
                    if (hovered) Color.Black.copy(alpha = 0.125f)
                    else Color.Transparent,
                    cornerRadius = CornerRadius(x = 3.sdp.value, y = 3.sdp.value)
                )
            }
            .weight(1f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(4.sdp)
        ) {
            Box(
                modifier = Modifier
                    .background(DoodlerTheme.Colors.Intro.IconBackgroundColor, shape = RoundedCornerShape(3.sdp))
            ) {
                Image(
                    painter = painterResource(image),
                    contentDescription = null,
                    modifier = Modifier.size(29.sdp).padding(2.sdp)
                )
            }
            Column(modifier = Modifier.padding(start = 7.sdp)) {
                SmallText(text = suffix)
                Text(
                    text = text,
                    fontSize = MaterialTheme.typography.h4.fsp,
                    color = DoodlerTheme.Colors.Text.IdeFunctionName
                )
            }
        }
    }
}

@Composable
fun SmallText(text: String) =
    Text(
        text = text,
        fontSize = MaterialTheme.typography.h6.fsp,
        color = DoodlerTheme.Colors.Text.IdeComment
    )
