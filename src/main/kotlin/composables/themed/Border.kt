package composables.themed

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

        val topOffset = Pair(Offset(0f, 0f), Offset(width, 0f))
        val rightOffset = Pair(Offset(width, 0f), Offset(width, height))
        val bottomOffset = Pair(Offset(0f, height), Offset(width, height))
        val leftOffset = Pair(Offset(0f, 0f), Offset(0f, height))

        if (top != null) drawLine(top.second, topOffset.first, topOffset.second, top.first)
        if (right != null) drawLine(right.second, rightOffset.first, rightOffset.second, right.first)
        if (bottom != null) drawLine(bottom.second, bottomOffset.first, bottomOffset.second, bottom.first)
        if (left != null) drawLine(left.second, leftOffset.first, leftOffset.second, left.first)
    }
}