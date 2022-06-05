package doodler.extension

fun <T> List<T>.contentEquals(other: List<T>): Boolean {
    if (size != other.size) return false
    for ((i1, i2) in mapIndexed { index, item -> item to other[index] }) {
        if (i1 != i2) return false
    }
    return true
}