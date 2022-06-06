package doodler.extension

import androidx.compose.ui.graphics.Color


fun Color.offsetRGB(
    offset: Float
) = Color(this.red.plus(offset), this.green.plus(offset), this.blue.plus(offset), this.alpha)

fun Color.offsetAlpha(
    offset: Float
) = copy(alpha = this.alpha.plus(offset))
