package doodler.theme

import androidx.compose.material.Colors
import androidx.compose.material.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font

class DoodlerTheme {

    class Dark {

        companion object {

            val Colors = Colors(
                background = DoodlerColors.Background,
                onBackground = DoodlerColors.OnBackground,
                primary = DoodlerColors.Primary,
                onPrimary = DoodlerColors.OnPrimary,
                primaryVariant = DoodlerColors.PrimaryVariant,
                secondary = DoodlerColors.Secondary,
                onSecondary = DoodlerColors.OnSecondary,
                secondaryVariant = DoodlerColors.SecondaryVariant,
                surface = DoodlerColors.Background, // NOT USED
                onSurface = DoodlerColors.Background, // NOT USED
                error = DoodlerColors.Error,
                onError = DoodlerColors.OnError,
                isLight = false
            )

            val Typography = Typography(
                defaultFontFamily = DoodlerFonts.JetbrainsMono,
            )

        }

    }

    private class DoodlerColors {

        companion object {
            val Background = Color(0xff2b2b2b)
            val OnBackground = Color(0xffa9b7c6)
            val Primary = Color(0xff689f38)
            val PrimaryVariant = Color(0xff387002)
            val OnPrimary = Color(0xffffffff)
            val Secondary = Color(0xff422819)
            val SecondaryVariant = Color(0xff2e1c12)
            val OnSecondary = Color(0xffffffff)
            val Error = Color(0xff573f35)
            val OnError = Color(0xffffffff)
        }

    }

    private class DoodlerFonts {

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

}