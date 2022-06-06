package doodler.extension


fun <T>MutableList<T>.replaceAt(index: Int, to: T) {
    removeAt(index)
    add(index, to)
}

fun <T>MutableList<T>.removeRange(range: IntRange): List<T> {
    val size = range.last - range.first
    val position = range.first
    val result = mutableListOf<T>()
    for (index in 0..size) {
        result.add(removeAt(position))
    }
    return result
}