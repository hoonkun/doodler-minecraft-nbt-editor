package composables.editor

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.*
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
import composables.global.JetBrainsMono
import composables.global.ThemedColor
import doodler.doodle.structures.DoodleLog
import kotlinx.coroutines.delay

// TODO:
//  이거 나중에, 에러가 연속해서 발생할 경우 이전 에러가 위쪽으로 살짝 올라가고 크기가 작아지는 모션같은거 넣어보자.
//  그러니까 에러가 연속해서 발생해도 지금은 알 수 없게 되어있는데 그걸 알 수 있게 해주면 좋을 듯.
// TODO:
//  추후에 발생했던 로그 중 최근 N개를 볼 수 있는 창도 만들면 좋을 것 같음
@Composable
fun ColumnScope.Log(
    logState: MutableState<DoodleLog?>,
) {

    var currentLog by logState

    var disabled by remember { mutableStateOf(true) }
    val alpha by animateFloatAsState(if (disabled) 0f else 1f, tween(if (disabled) 250 else 100, 0, LinearEasing))

    LaunchedEffect(currentLog) {
        disabled = false
        delay(5000)
        disabled = true
        delay(250)
        currentLog = null
    }

    val log = currentLog ?: return
    Box (
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(4f)
            .alpha(alpha)
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

