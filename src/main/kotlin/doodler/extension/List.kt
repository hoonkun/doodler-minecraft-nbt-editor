package doodler.extension

fun <T> List<T>.contentEquals(other: List<T>): Boolean {
    if (size != other.size) return false
    for ((i1, i2) in mapIndexed { index, item -> item to other[index] }) {
        if (i1 != i2) return false
    }
    return true
}

fun <T> List<T>.indexOfAbsoluteEquals(element: T): Int {
    forEachIndexed { index, item ->
        if (item === element) return index
    }
    return -1
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

fun Collection<Int>.toReversedRange(min: Int, max: Int): List<IntRange> {
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
