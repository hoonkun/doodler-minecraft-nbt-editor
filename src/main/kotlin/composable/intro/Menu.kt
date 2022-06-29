package composable.intro

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import doodler.local.*
import doodler.theme.DoodlerTheme
import doodler.unit.ddp
import doodler.unit.dsp

@Composable
fun BoxScope.MenuButton(blur: Boolean, onClick: () -> Unit) {

    val hoverInteractionSource = remember { MutableInteractionSource() }
    val hovered by hoverInteractionSource.collectIsHoveredAsState()

    val icon = painterResource("/icons/menu_icon.png")

    Box(modifier = Modifier
        .size(40.ddp)
        .align(Alignment.TopEnd)
        .padding(5.ddp)
        .clip(RoundedCornerShape(15.ddp))
        .hoverable(hoverInteractionSource)
        .clickable { onClick() }
        .drawBehind {
            if (hovered) drawRoundRect(Color.White.copy(alpha = 0.15f), cornerRadius = CornerRadius(15.ddp.value))
        }
        .settingsBlur(blur)
    ) {
        Image(
            painter = icon,
            contentDescription = null,
            modifier = Modifier.padding(6.ddp)
        )
    }
}

@Composable
fun SettingsMenu(
    visible: Boolean,
    onClose: () -> Unit,
    onGlobalScaleChanged: (Float) -> Unit
) {

    if (!visible) return

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = 0.65f))
        .clickable { onClose() }
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .padding(15.ddp)
                .align(Alignment.Center)
        ) {
            Text(text = "Settings", fontSize = 20.dsp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(10.ddp))
            SettingsItem("Global Scale") {
                CircularTextButton(text = "-", enabled = GlobalScale > 1.0f) {
                    onGlobalScaleChanged(-0.25f)
                }
                NumberValue(GlobalScale.toString())
                CircularTextButton(text = "+", enabled = GlobalScale < 3.0f) {
                    onGlobalScaleChanged(0.25f)
                }
            }
            SettingsItem("Max LoaderStack Size") {
                CircularTextButton(text = "-", enabled = UserSavedLocalState.loaderStackSize > 3) {
                    UserSavedLocalState.loaderStackSize -= 1
                    editSavedLocal(loaderStackSize = UserSavedLocalState.loaderStackSize)
                }
                NumberValue(UserSavedLocalState.loaderStackSize.toString())
                CircularTextButton(text = "+", enabled = UserSavedLocalState.loaderStackSize < 15) {
                    UserSavedLocalState.loaderStackSize += 1
                    editSavedLocal(loaderStackSize = UserSavedLocalState.loaderStackSize)
                }
            }
            Spacer(modifier = Modifier.height(30.ddp))
            TextButton("close", onClose)
            Spacer(modifier = Modifier.height(5.ddp))
            Text(
                text = "/* there is only few options to set currently,\n" +
                        "   but more options will be provided\n" +
                        "   based on your feedback!                    */",
                color = DoodlerTheme.Colors.Text.IdeComment,
                fontSize = 7.dsp,
                modifier = Modifier.align(Alignment.End)
            )
        }

    }
}

@Composable
fun SettingsItem(
    name: String,
    content: @Composable RowScope.() -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = name, fontSize = 13.dsp, color = Color.White.copy(alpha = 0.85f))
        Spacer(modifier = Modifier.weight(1f))
        content()
    }
    Spacer(modifier = Modifier.height(3.ddp))
}

@Composable
fun NumberValue(
    value: String
) = Text(
    text = value,
    color = DoodlerTheme.Colors.Text.IdeNumberLiteral,
    fontSize = 13.dsp,
    modifier = Modifier.padding(horizontal = 10.ddp)
)

@Composable
fun CircularTextButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val hovered by source.collectIsHoveredAsState()

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(20.ddp)
            .hoverable(source, enabled)
            .clickable(source, null, enabled) { onClick() }
            .alpha(if (enabled) 1f else 0.4f)
            .drawBehind {
                if (pressed) drawRoundRect(Color.White.copy(alpha = 0.25f), cornerRadius = CornerRadius(10.ddp.value))
                else if (hovered) drawRoundRect(Color.White.copy(alpha = 0.15f), cornerRadius = CornerRadius(10.ddp.value))
            }
    ) {
        Text(text = text, color = Color.White.copy(alpha = 0.75f), fontSize = 13.dsp)
    }
}

@Composable
fun ColumnScope.TextButton(
    text: String,
    onClick: () -> Unit
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val hovered by source.collectIsHoveredAsState()

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .align(Alignment.End)
            .hoverable(source)
            .clickable(source, null) { onClick() }
            .drawBehind {
                if (pressed) drawRoundRect(Color.White.copy(alpha = 0.25f), cornerRadius = CornerRadius(3.ddp.value))
                else if (hovered) drawRoundRect(Color.White.copy(alpha = 0.15f), cornerRadius = CornerRadius(3.ddp.value))
            }
    ) {
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 13.dsp,
            modifier = Modifier.padding(horizontal = 8.ddp, vertical = 2.ddp)
        )
    }
}
