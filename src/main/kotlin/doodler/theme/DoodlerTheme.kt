package doodler.theme

import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material.ripple.RippleTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font

class DoodlerTheme {

    class Colors {

        companion object {
            val Background = Color(0xff2b2b2b)
            val OnBackground = Color(0xffa9b7c6)
            val Primary = Color(0xff689f38)
            val PrimaryVariant = Color(0xff387002)
            val OnPrimary = Color(0xffffffff)
            val Secondary = Color(0xff422819)
            val SecondaryVariant = Color(0xff2e1c12)
            val OnSecondary = Color(0xffffffff)

            val SecondaryBackground = Color(0xff3c3f41)

            val ExternalLink = Color(0xff64b5f6)

            val PrimaryLink = Color(0xffaed581)
        }

    }

    class Fonts {

        companion object {
            val JetbrainsMono = FontFamily(
                Font (
                    resource = "JetBrainsMono-Regular.ttf",
                    weight = FontWeight.W400,
                    style = FontStyle.Normal
                )
            )
        }

    }

    object ClearRippleTheme : RippleTheme {
        @Composable
        override fun defaultColor(): Color = Color.Transparent

        @Composable
        override fun rippleAlpha() = RippleAlpha(
            draggedAlpha = 0.0f,
            focusedAlpha = 0.0f,
            hoveredAlpha = 0.0f,
            pressedAlpha = 0.0f,
        )
    }

}