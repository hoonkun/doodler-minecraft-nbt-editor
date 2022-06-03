package activator.doodler.extensions

fun <T>MutableList<T>.replaceAt(index: Int, to: T) {
    removeAt(index)
    add(index, to)
}

fun List<Int>.toRanges(): List<IntRange> {
    val sorted = sorted()
    val result = mutableListOf<IntRange>()

    if (isEmpty()) return result

    var start = sorted[0]
    var current = sorted[0]
    sorted.slice(1 until size).forEach {
        if (it == current + 1) {
            current = it
            return@forEach
        }
        result.add(start..current)
        start = it
        current = it
    }

    result.add(start..current)

    return result
}

fun List<Int>.toReversedRange(min: Int, max: Int): List<IntRange> {
    val sorted = sorted()
    val result = mutableListOf<IntRange>()

    if (isEmpty()) return result

    var current = min
    sorted.slice(1 until size).forEach {
        if (it == current + 1) {
            current = it
            return@forEach
        }
        result.add(current until it)
        current = it
    }

    if (current < max) result.add(current..max)

    return result
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