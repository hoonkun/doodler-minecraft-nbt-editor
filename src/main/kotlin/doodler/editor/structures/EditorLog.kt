package doodler.editor.structures

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color

enum class EditorLogLevel(
    val background: Color
) {
    Fatal(Color(0xff573f35)),
    Success(Color(0xff354757))
}

@Stable
data class EditorLog(
    val level: EditorLogLevel,
    val title: String,
    val summary: String?,
    val description: String?
)