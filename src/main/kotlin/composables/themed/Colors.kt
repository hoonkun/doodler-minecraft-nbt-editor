package composables.themed

import androidx.compose.ui.graphics.Color

class ThemedColor {
    companion object {

        val Bright = Color(174, 213, 129)
        val Link = Color(100, 181, 246, 200)

        val ActionBar = Color(55, 55, 57)
        val TabBar = Color(50, 51, 53)
        val TopBar = Color(30, 30, 30)
        val TopBarBorder = Color(36, 36, 36)
        val TaskArea = Color(60, 63, 65)
        val EditorArea = Color(43, 43, 43)
        val SelectorArea = Color(36, 36, 36)

        val WhiteSecondary = Color(255, 255, 255, 185)
        val WhiteOthers = Color(255, 255, 255, 100)

        val ScrollBarNormal = Color(255, 255, 255, 50)
        val ScrollBarHover = Color(255, 255, 255, 100)

        val Copyright = Color(255, 255, 255, 180)

        val DocumentationDescription = Color(255, 255, 255, 145)

        fun from(base: Color, red: Int? = null, green: Int? = null, blue: Int? = null, alpha: Int? = null) =
            Color(
                red ?: (base.red * 255).toInt(),
                green ?: (base.green * 255).toInt(),
                blue ?: (base.blue * 255).toInt(),
                alpha ?: (base.alpha * 255).toInt()
            )

        fun offset(base: Color, offset: Int) =
            Color(
                (base.red * 255).toInt() + offset,
                (base.green * 255).toInt() + offset,
                (base.blue * 255).toInt() + offset
            )

        fun selectable(selected: Boolean, pressed: Boolean, focused: Boolean) =
            if (selected) Color(255, 255, 255, 30)
            else if (pressed) Color(0, 0, 0, 80)
            else if (focused) Color(0, 0, 0, 40)
            else Color.Transparent

        fun clickable(pressed: Boolean, focused: Boolean) =
            if (pressed) Color(255, 255, 255, 60)
            else if (focused) Color(255, 255, 255, 30)
            else Color.Transparent

    }

    class Editor {
        companion object {

            val HasChanges = Color(255, 160, 0)
            val TreeBorder = Color(100, 100, 100)

            private val Pressed = from(Color.Black, alpha = 80)
            private val Focused = from(Color.Black, alpha = 40)

            private val Selected = Color(91, 115, 65)

            private val Indicator = Color(60, 60, 60)
            private val SelectedIndicator = offset(Indicator, 10)

            private val IndicatorText = Color(150, 150, 150)
            private val SelectedIndicatorText = offset(IndicatorText, 7)

            private val DepthLine = Color(60, 60, 60)
            private val FocusedDepthLine = offset(DepthLine, 40)

            private val FocusedTabCloseButtonBackground = Color(255, 255, 255, 75)
            private val FocusedTabButtonIcon = Color(0, 0, 0, 200)
            private val TabButtonIcon = Color(255, 255, 255, 100)

            fun tabCloseButtonBackground(focused: Boolean) =
                if (focused) FocusedTabCloseButtonBackground else Color.Transparent

            fun tabCloseButtonIcon(focused: Boolean) =
                if (focused) FocusedTabButtonIcon else TabButtonIcon

            fun item(selected: Boolean, pressed: Boolean, focused: Boolean) =
                if (selected) selectedItem(pressed, focused)
                else normalItem(pressed, focused)

            fun normalItem(pressed: Boolean, focused: Boolean) =
                if (pressed) Pressed
                else if (focused) Focused
                else Color.Transparent

            private fun selectedItem(pressed: Boolean, focused: Boolean) =
                if (pressed) from(Selected, alpha = 20)
                else if (focused) from(Selected, alpha = 35)
                else from(Selected, alpha = 50)

            fun indicator(selected: Boolean) = if (selected) SelectedIndicator else Indicator
            fun indicatorText(selected: Boolean) = if (selected) SelectedIndicatorText else IndicatorText

            fun depthLine(selected: Boolean, focused: Boolean) =
                if (focused) offset(FocusedDepthLine, if (selected) 16 else 0)
                else offset(DepthLine, if (selected) 16 else 0)

        }

        class Action {
            companion object {

                val Delete = Color(227, 93, 48)
                val Yank = Color(88, 163, 126)
                val Edit = Color(88, 132, 163)

                val Background = Color(255, 255, 255, 25)

            }
        }

        class Selector {
            companion object {

                val Normal = Color(125, 125, 125)
                val Malformed = Color(140, 140, 140)
                val Invalid = Color(230, 81, 0)

                val ButtonText = Color(255, 255, 255, 175)

                private val ValidAccent = Color(50, 54, 47)
                private val InvalidAccent = Color(64, 55, 52)
                private val Default = Color(42, 42, 42)

                fun background(accent: Boolean, valid: Boolean) =
                    if (accent && valid) ValidAccent
                    else if (accent) InvalidAccent
                    else Default

            }
        }

        class Tag {
            companion object {

                val Number = Color(104, 151, 187)
                val String = Color(106, 135, 89)
                val Compound = Color(255, 199, 109)
                val List = Color(204, 120, 50)
                val NumberArray = Number

                val General = Color(169, 183, 198)

            }
        }
    }
}