package composables.states.editor.world

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import composables.themed.JetBrainsMono
import composables.themed.ThemedColor

enum class DoodleLogLevel {
    FATAL;

    fun backgroundColor(): Color {
        return when (this) {
            FATAL -> Color(87, 63, 53)
        }
    }

}

data class DoodleLog(
    val level: DoodleLogLevel,
    val title: String,
    val summary: String?,
    val description: String?
)

@Composable
fun LogText(text: String, alpha: Float, fontSize: TextUnit = 19.sp) {
    Text(
        text,
        color = Color.White,
        fontFamily = JetBrainsMono,
        fontSize = fontSize,
        modifier = Modifier.alpha(alpha)
    )
}

@Composable
fun Log(log: DoodleLog) {
    Box (
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(4f)
            .drawBehind {
                val amount = 125f

                drawRect(
                    Brush.linearGradient(
                        listOf(ThemedColor.from(Color.Black, alpha = 200), Color.Transparent),
                        start = Offset(0f, amount * (size.width / size.height)),
                        end = Offset(amount, 0f)
                    )
                )
            }
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(top = 60.dp, end = 100.dp, bottom = 30.dp, start = 30.dp).zIndex(10001f)
        ) {
            Box(
                modifier = Modifier
                    .background(log.level.backgroundColor(), RoundedCornerShape(6.dp))
            ) {
                Column(modifier = Modifier.padding(top = 20.dp, start = 20.dp, bottom = 20.dp, end = 40.dp)) {
                    Row {
                        LogText(log.title, 0.85f)
                        if (log.summary != null) {
                            LogText(":", 0.7f)
                            Spacer(modifier = Modifier.width(20.dp))
                            LogText(log.summary, 0.7f)
                        }
                    }
                    Spacer(modifier = Modifier.height(7.dp))
                    if (log.description != null)
                        LogText(log.description, 0.45f, 15.sp)
                }
            }
        }
    }
}
