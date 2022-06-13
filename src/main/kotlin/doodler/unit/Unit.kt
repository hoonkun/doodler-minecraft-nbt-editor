package doodler.unit

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

const val GlobalMultiplier = 1.0f

val Int.ddp get() = this.times(GlobalMultiplier).dp
val Float.ddp get() = this.times(GlobalMultiplier).dp
val Double.ddp get() = this.times(GlobalMultiplier).dp

val Int.dsp get() = this.times(GlobalMultiplier).sp
// val Float.dsp get() = this.times(GlobalMultiplier).sp
val Double.dsp get() = this.times(GlobalMultiplier).sp

val Int.adp get() = this.dp
val Float.adp get() = this.dp

// val TextStyle.fsp get() = this.fontSize
