package doodler.unit

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val multiplier = 1.0f

val Int.sp get() = (this * multiplier).sp
val Float.sp get() = (this * multiplier).sp

val Int.dp get() = (this * multiplier).dp
val Float.dp get() = (this * multiplier).dp