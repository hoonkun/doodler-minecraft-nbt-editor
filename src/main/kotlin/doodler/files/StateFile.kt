package doodler.files

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import doodler.extensions.contentEquals

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

@Stable
data class StateFileList(
    val items: SnapshotStateList<StateFile>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as StateFileList

        return items.contentEquals(other.items)
    }

    override fun hashCode(): Int {
        var result = 0
        for (item in items) {
            result = 31 * result + item.hashCode()
        }
        return result
    }
}

@Stable
fun Collection<StateFile>.toStateFileList() = StateFileList(toMutableStateList())

