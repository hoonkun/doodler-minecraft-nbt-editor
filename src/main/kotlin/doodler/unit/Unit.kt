package doodler.unit

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val multiplier = 1.0f

val Int.sp get() = (this * multiplier).sp
val Float.sp get() = (this * multiplier).sp

val Int.dp get() = (this * multiplier).dp
val Float.dp get() = (this * multiplier).dp

class ScaledUnits {

    class HierarchyView {

        companion object {
            private const val Value = 0.9f

            val Dp.scaled get() = this * Value
            val TextUnit.scaled get() = this * Value
        }

    }

    class Editor {

        companion object {
            private const val Value = 0.75f

            val Dp.scaled get() = this * Value
            val TextUnit.scaled get() = this * Value
        }

    }

    class Tabs {

        companion object {
            private const val Value = 0.9f

            val Dp.scaled get() = this * Value
            val TextUnit.scaled get() = this * Value
        }

    }

    class ChunkSelector {

        companion object {
            private const val Value = 0.6f

            val TextUnit.scaled get() = this * Value
            val Dp.scaled get() = this * Value
        }

    }

    class AnvilPreview {

        companion object {
            private const val Value = 0.8f

            val TextUnit.scaled get() = this * Value
            val Dp.scaled get() = this * Value
        }

    }

}