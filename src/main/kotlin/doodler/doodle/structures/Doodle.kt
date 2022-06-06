package doodler.doodle.structures

import androidx.compose.runtime.Stable

@Stable
sealed class Doodle(
    val depth: Int
) {
    abstract val path: String
}
