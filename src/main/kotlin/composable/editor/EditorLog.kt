package composable.editor

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import doodler.editor.structures.EditorLog
import doodler.types.Provider
import doodler.unit.ddp
import kotlinx.coroutines.delay


private val TextStyle.fsp get() = this.fontSize * 0.75

@Composable
fun BoxScope.Log(
    logProvider: Provider<MutableState<EditorLog?>>
) {
    val state = logProvider()

    val log = state.value ?: return

    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (visible) 100 else 250,
            delayMillis = 0,
            easing = LinearEasing
        )
    )

    LaunchedEffect(log) {
        visible = true
        delay(5000)
        visible = false
        delay(250)
        state.value = null
    }

    Box(
        contentAlignment = Alignment.BottomStart,
        modifier = Modifier
            .align(Alignment.BottomStart)
            .fillMaxWidth().aspectRatio(4f)
            .alpha(alpha).drawBehind { drawBackgroundGradient() }
    ) {
        Column(
            modifier = Modifier
                .padding(15.ddp)
        ) {
            Column(modifier = Modifier
                .background(log.level.background, RoundedCornerShape(4.5.ddp))
                .padding(top = 9.ddp, end = 22.5.ddp, bottom = 9.ddp, start = 9.ddp)
            ) {
                Row {
                    LogText(text = log.title, alpha = 0.85f)
                    if (log.summary != null) {
                        Spacer(modifier = Modifier.width(7.5.ddp))
                        LogText(
                            text = log.summary,
                            alpha = 0.7f,
                            fontSize = MaterialTheme.typography.h5.fsp
                        )
                    }
                }
                if (log.description != null) {
                    Spacer(modifier = Modifier.height(1.5.ddp))
                    LogText(log.description, alpha = 0.45f, fontSize = MaterialTheme.typography.h5.fsp)
                }
            }
        }
    }

}

@Composable
fun LogText(
    text: String,
    alpha: Float,
    fontSize: TextUnit = MaterialTheme.typography.h4.fsp
) {
    Text(
        text = text,
        color = Color.White.copy(alpha = alpha),
        fontSize = fontSize
    )
}

fun DrawScope.drawBackgroundGradient() {
    val amount = 125f

    drawRect(
        Brush.linearGradient(
            listOf(Color.Black.copy(alpha = 0.7843f), Color.Transparent),
            start = Offset(0f, amount * (size.width / size.height)),
            end = Offset(amount, 0f)
        )
    )
}