package doodler.unit

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import doodler.local.GlobalScale

val Int.ddp get() = this.times(GlobalScale).dp
val Float.ddp get() = this.times(GlobalScale).dp
val Double.ddp get() = this.times(GlobalScale).dp

val Int.dsp get() = this.times(GlobalScale).sp
val Double.dsp get() = this.times(GlobalScale).sp

val Int.adp get() = this.dp
val Float.adp get() = this.dp
