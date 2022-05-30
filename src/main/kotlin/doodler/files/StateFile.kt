package doodler.files

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

@Immutable
data class StateFile(
    val name: String,
    val absolutePath: String,
    val isDirectory: Boolean,
    val isFile: Boolean
)

@Stable
fun stateFileOf(name: String, absolutePath: String, isDirectory: Boolean, isFile: Boolean) =
    StateFile(name, absolutePath, isDirectory, isFile)
