package activator.doodler.extensions

import androidx.compose.runtime.snapshots.SnapshotStateList

fun <T>SnapshotStateList<T>.contentEquals(other: SnapshotStateList<T>): Boolean {
    if (size != other.size) return false
    for ((i1, i2) in mapIndexed { index, item -> item to other[index] }) {
        if (i1 != i2) return false
    }
    return true
}