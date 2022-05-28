package composables.global

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

        val TreeViewSelected = Color(0xFF49544A)
        val MapViewButton = Color(0xFF404A41)
        val MapViewButtonHover = Color(0xFF464F47)
        val MapViewButtonN = Color(0xFF49544A)
        val MapViewButtonHoverN = Color(0xFF4F5B50)

        val DocumentationDescription = Color(255, 255, 255, 145)

        val ChunkSelectorPropertyKey = Color(0xFF467CDA)
        val ChunkSelectorPropertyHint = Color(0xFF808080)

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
            if (selected) Color(255, 255, 255, 20)
            else if (pressed) Color(0, 0, 0, 80)
            else if (focused) Color(0, 0, 0, 40)
            else Color.Transparent

        fun clickable(pressed: Boolean, focused: Boolean) =
            if (pressed) Color(255, 255, 255, 60)
            else if (focused) Color(255, 255, 255, 30)
            else Color.Transparent

    }

    class Aside {
        companion object {

            val DAT = Color(0x3642a5f5)
            val MCA = Color(0x36689f38)
            val PNG = Color(0x36ef6c00)
            val JSON = Color(0x36bdbdbd)

        }
    }

    class Editor {
        companion object {

            val HasChanges = Color(0xFF70b8ff)
            val TreeBorder = Color(100, 100, 100)

            val Creation = Color(66, 107, 173, 50)

            private val Pressed = from(Color.Black, alpha = 80)
            private val Focused = from(Color.Black, alpha = 40)

            private val Selected = Color(91, 115, 65, 50)
            val ScrollIndicatorSelected = Color(91, 115, 65)

            private val Indicator = Color(60, 60, 60)
            private val SelectedIndicator = offset(Indicator, 10)

            private val IndicatorText = Color(150, 150, 150)
            private val SelectedIndicatorText = offset(IndicatorText, 7)

            private val DepthLine = Color(60, 60, 60)
            private val FocusedDepthLine = offset(DepthLine, 40)

            private val FocusedTabCloseButtonBackground = Color(255, 255, 255, 75)
            private val FocusedTabButtonIcon = Color(25, 25, 25, 200)
            private val TabButtonIcon = Color(255, 255, 255, 100)

            fun tabCloseButtonBackground(focused: Boolean) =
                if (focused) FocusedTabCloseButtonBackground else Color.Transparent

            fun tabCloseButtonIcon(focused: Boolean) =
                if (focused) FocusedTabButtonIcon else TabButtonIcon

            fun item(
                selected: Boolean,
                pressed: Boolean,
                focused: Boolean,
                onCreationMode: Boolean = false,
                alphaMultiplier: Float = 1.0f
            ) =
                if (selected) selectedItem(pressed, focused, onCreationMode, alphaMultiplier)
                else normalItem(pressed, focused)

            fun normalItem(pressed: Boolean, focused: Boolean) =
                if (pressed) Pressed
                else if (focused) Focused
                else Color.Transparent

            private fun selectedItem(
                pressed: Boolean,
                focused: Boolean,
                onCreationMode: Boolean,
                alphaMultiplier: Float
            ) =
                if (pressed) from(if (!onCreationMode) Selected else Creation, alpha = (20 * alphaMultiplier).toInt())
                else if (focused) from(if (!onCreationMode) Selected else Creation, alpha = (35 * alphaMultiplier).toInt())
                else from(if (!onCreationMode) Selected else Creation, alpha = (50 * alphaMultiplier).toInt())

            fun indicator(selected: Boolean) = if (selected) SelectedIndicator else Indicator
            fun indicatorText(selected: Boolean) = if (selected) SelectedIndicatorText else IndicatorText

            fun depthLine(selected: Boolean, focused: Boolean) =
                if (focused) offset(FocusedDepthLine, if (selected) 16 else 0)
                else offset(DepthLine, if (selected) 16 else 0)

        }

        class Action {
            companion object {

                val Delete = Color(227, 93, 48)
                val Create = Color(64, 143, 227)
                val Save = Color(0xff689f38)

                val Background = Color(255, 255, 255, 25)

            }
        }

        class Selector {
            companion object {

                val Normal = Color(125, 125, 125)
                val Malformed = Color(140, 140, 140)
                val Invalid = Color(245, 124, 0)

                val ButtonText = Color(255, 255, 255, 175)

                private val ValidAccent = Color(50, 54, 47)
                private val InvalidAccent = Color(64, 55, 52)
                private val Default = Color(42, 42, 42)
                private val DefaultHover = Color(50, 50, 50)

                fun background(accent: Boolean, valid: Boolean, hover: Boolean) =
                    if (accent && valid) ValidAccent
                    else if (accent) InvalidAccent
                    else if (hover) DefaultHover
                    else Default

            }
        }

        class Tag {
            companion object {

                val Number = Color(104, 151, 187)
                val String = Color(106, 135, 89)
                val Compound = Color(255, 199, 109)
                val List = Color(204, 120, 50)
                val NumberArray = Color(0xFF467CDA)

                val General = Color(169, 183, 198)

            }
        }
    }
}