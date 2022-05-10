package composables.themed

import androidx.compose.ui.graphics.Color

class ThemedColor {
    companion object {
        val Bright = Color(174, 213, 129)
        val Link = Color(100, 181, 246, 200)

        fun selectable(selected: Boolean, press: Boolean, hover: Boolean): Color {
            return if (selected) Color(255, 255, 255, 30)
            else if (press) Color(0, 0, 0, 80)
            else if (hover) Color(0, 0, 0, 40)
            else Color.Transparent
        }
    }
}