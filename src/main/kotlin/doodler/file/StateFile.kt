package doodler.file

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import doodler.extension.contentEquals
import java.io.File


@Immutable
data class StateFile(
    val name: String,
    val absolutePath: String,
    val isDirectory: Boolean,
    val isFile: Boolean
)

fun File.toStateFile() = StateFile(name, absolutePath, isDirectory, isFile)

@Stable
data class StateFileList(
    val items: List<StateFile>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as StateFileList

        return items.contentEquals(other.items)
    }

    override fun hashCode(): Int {
        return items.hashCode()
    }
}

@Stable
fun Collection<StateFile>.toStateFileList() = StateFileList(toList())

