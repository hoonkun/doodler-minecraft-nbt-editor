package composable.intro

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import doodler.unit.ddp

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

    }
}
