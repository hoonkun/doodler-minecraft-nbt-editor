package composables.global

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

fun border(
    top: Pair<Float, Color>? = null,
    right: Pair<Float, Color>? = null,
    bottom: Pair<Float, Color>? = null,
    left: Pair<Float, Color>? = null
): DrawScope.() -> Unit {
    return {
        val width = size.width
        val height = size.height

        if (top != null) drawLine(top.second, Offset(0f, 0f), Offset(width, 0f), top.first)
        if (right != null) drawLine(right.second, Offset(width, 0f), Offset(width, height), right.first)
        if (bottom != null) drawLine(bottom.second, Offset(0f, height), Offset(width, height), bottom.first)
        if (left != null) drawLine(left.second, Offset(0f, 0f), Offset(0f, height), left.first)
    }
}