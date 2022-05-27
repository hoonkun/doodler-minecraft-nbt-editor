package doodler.doodle.structures

import androidx.compose.ui.graphics.Color

enum class DoodleLogLevel {
    FATAL;

    fun backgroundColor(): Color {
        return when (this) {
            FATAL -> Color(87, 63, 53)
        }
    }

}

class DoodleLog(
    val level: DoodleLogLevel,
    val title: String,
    val summary: String?,
    val description: String?
)