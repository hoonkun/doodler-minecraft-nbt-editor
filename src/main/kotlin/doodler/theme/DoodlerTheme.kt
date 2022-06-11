package doodler.theme

import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material.ripple.RippleTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import doodler.extension.offsetRGB

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
            val SecondaryBackgroundBorder = Color(0xff323232)

            val ExternalLink = Color(0xff64b5f6)

            val PrimaryLink = Color(0xffaed581)
        }

        class Selector {

            companion object {
                val Directories = Color(0xffffc660)
                val Files = Color(0xffa9b7c6)
                val FocusedCandidate = Color(0x30ffffff)
                val PathInput = Color(0xff202020)
                val SelectButton = Color(0xff51702e)
                val SelectButtonHovered = Color(0xff668d3a)
            }

        }

        class DoodleAction {

            companion object {
                val CancelAction = Color(0xffe35d30)
                val DeleteAction = Color(0xffe35d30)
                val OkAction = Color(0xff408fe3)
                val SaveAction = Color(0xff689f38)
            }

        }

        class HierarchyView {

            companion object {
                val Selected = Color(0xff49544a)

                val Dat = Color(0x3642a5f5)
                val Mca = Color(0x36689f38)
                val Png = Color(0x36ef6c00)
                val Json = Color(0x36bdbdbd)

                val TextColor = Color(0xffbbbbbb)

                val DimensionsDirectoryBackground = Color(0xff4f4b41)
            }

        }

        class Editor {

            companion object {
                val ScrollbarDecorSelected = Color(0xff5b7341)
                val ActionBackground = Color(0x19ffffff)

                val TabPath = Color(0xff808080)

                val TabHasChanges = Color(0xff90caf9)

                fun Tab(selected: Boolean, pressed: Boolean, focused: Boolean) =
                    if (selected) Color(255, 255, 255, 20)
                    else if (pressed) Color(0, 0, 0, 80)
                    else if (focused) Color(0, 0, 0, 40)
                    else Color.Transparent

                fun TabCloseButton(hovered: Boolean) =
                    if (hovered) Color(25, 25, 25, 200)
                    else Color(255, 255, 255, 100)

                fun TabCloseButtonBackground(hovered: Boolean) =
                    if (hovered) Color(255, 255, 255, 75)
                    else Color.Transparent
            }

        }

        class DoodleItem {

            companion object {
                val NormalTagTypeBackground = Color(0xff3c3c3c)
                val SelectedTagTypeBackground = NormalTagTypeBackground.offsetRGB(0.0392f)

                val NormalExpandableValue = Color(0xff969696)
                val SelectedExpandableValue = NormalExpandableValue.offsetRGB(0.0274f)

                val NormalIndex = Color(0xff7a7a7a)
                val SelectedIndex = NormalExpandableValue.offsetRGB(0.0274f)

                val NormalItemBackground = Color(0xff2b2b2b)
                val SelectedItemBackground = Color(0xff5b7341)
                val ActionTargetItemBackground = Color(0xff426bad)

                val NormalDepthLine = Color(0xff3c3c3c)
                val SelectedDepthLine = NormalDepthLine.offsetRGB(0.0627f)
                val DepthLineHoverOffset = 0.1568f

                val DepthPreviewBorder = Color(0xff646464)

                fun TagTypeBackground(selected: Boolean) =
                    if (!selected) NormalTagTypeBackground else SelectedTagTypeBackground

                fun IndexTextColor(selected: Boolean) =
                    if (!selected) NormalIndex else SelectedIndex

                fun ExpandableValueTextColor(selected: Boolean) =
                    if (!selected) NormalExpandableValue else SelectedExpandableValue

                fun Background(
                    hovered: Boolean,
                    pressed: Boolean,
                    selected: Boolean,
                    highlightAsActionTarget: Boolean
                ) =
                    if (highlightAsActionTarget) {
                        ActionTargetItemBackground.FocusableItemBackground(hovered, pressed)
                    } else if (selected) {
                        SelectedItemBackground.FocusableItemBackground(hovered, pressed)
                    } else {
                        Color.Black.OpaqueFocusableItemBackground(hovered, pressed)
                    }

                fun DepthLine(
                    selected: Boolean,
                    hovered: Boolean
                ) =
                    if (selected) SelectedDepthLine.offsetRGB(if (hovered) DepthLineHoverOffset else 0f)
                    else NormalDepthLine.offsetRGB(if (hovered) DepthLineHoverOffset else 0f)

                private fun Color.FocusableItemBackground(
                    hovered: Boolean,
                    pressed: Boolean
                ) =
                    if (pressed) copy(alpha = 0.0784f)
                    else if (hovered) copy(alpha = 0.1372f)
                    else copy(alpha = 0.1960f)

                private fun Color.OpaqueFocusableItemBackground(
                    hovered: Boolean,
                    pressed: Boolean
                ) =
                    if (pressed) copy(alpha = 0.1372f)
                    else if (hovered) copy(alpha = 0.0784f)
                    else copy(alpha = 0f)
            }

        }

        class Text {

            companion object {
                val IdeDocumentation = Color(0xff629755)
                val IdeFunctionProperty = Color(0xff467cda)
                val IdeNumberLiteral = Color(0xff6897bb)
                val IdeStringLiteral = Color(0xff6a8759)
                val IdeGeneral = Color(0xffa9b7c6)
            }

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